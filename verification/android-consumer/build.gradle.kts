import groovy.json.JsonOutput
import java.security.MessageDigest
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.w3c.dom.Element

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
}

providers.gradleProperty("consumerBuildRoot").orNull?.let { layout.buildDirectory.set(file(it)) }
val producerVersion = providers.gradleProperty("producerVersion").getOrElse("0.1.1")
val expectedVersions = mapOf(
    "once-cell-kotlin" to producerVersion,
)

kotlin {
    jvmToolchain(21)
    compilerOptions { allWarningsAsErrors.set(true) }
    jvm()
    macosArm64()
    js { nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi { nodejs() }
    android {
        namespace = "io.github.kotlinmania.oncecell.publicationconsumer"
        compileSdk = providers.gradleProperty("consumerCompileSdk").getOrElse("34").toInt()
        minSdk = 24
        withHostTestBuilder { sourceSetTreeName = "test" }
    }
    sourceSets {
        commonMain.dependencies {
            expectedVersions.forEach { (module, version) -> implementation("io.github.kotlinmania:$module:$version") }
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

tasks.withType<AbstractTestTask>().configureEach { outputs.upToDateWhen { false } }
val hostTests = tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }

kotlin.targets.named("android") {
    compilations.named("main") {
        val compileDependencies = configurations.getByName(compileDependencyConfigurationName)
        val runtimeName = compileDependencyConfigurationName.replace("CompileClasspath", "RuntimeClasspath")
        val runtimeDependencies = configurations.named(runtimeName)
        val classpathReceipt = layout.buildDirectory.file("reports/publication/raw-classpaths.json")
        val writeClasspath = tasks.register("writeAndroidPublicationClasspath") {
            outputs.file(classpathReceipt)
            outputs.upToDateWhen { false }
            doLast {
                val artifactsByConfiguration = mapOf(
                    "compile" to compileDependencies,
                    "runtime" to runtimeDependencies.get(),
                ).mapValues { (scope, configuration) ->
                    val artifacts = configuration.incoming.artifacts.artifacts.map { it.file }.distinct()
                    check(artifacts.isNotEmpty() && artifacts.all { it.extension in setOf("aar", "jar") }) {
                        "Unsupported raw Android $scope classpath artifact layout: $artifacts"
                    }
                    artifacts.sortedBy { it.absolutePath }.map { it.absolutePath }
                }
                classpathReceipt.get().asFile.apply {
                    parentFile.mkdirs()
                    writeText(JsonOutput.prettyPrint(JsonOutput.toJson(artifactsByConfiguration)) + "\n")
                }
            }
        }
        val checkClasspath = tasks.register<Exec>("checkAndroidPublicationClasspath") {
            dependsOn(writeClasspath)
            mustRunAfter(hostTests)
            commandLine(
                "python3",
                file("../check_android_classpath.py"),
                classpathReceipt.get().asFile,
                "--configurations",
                "--receipt",
                layout.buildDirectory.file("reports/publication/duplicates.json").get().asFile,
            )
        }
        hostTests.configureEach { finalizedBy(checkClasspath) }
        tasks.register("verifyAndroidConsumer") {
            dependsOn(compileTaskProvider, hostTests, checkClasspath)
            doLast {
                val selections = mutableListOf<String>()
                compileDependencies.incoming.resolutionResult.rootComponent.get().dependencies
                    .filterIsInstance<ResolvedDependencyResult>().forEach { dependency ->
                        val requested = dependency.requested as? ModuleComponentSelector ?: return@forEach
                        val version = expectedVersions[requested.module] ?: return@forEach
                        val variant = dependency.resolvedVariant.externalVariant.orElse(dependency.resolvedVariant)
                        val owner = variant.owner as ModuleComponentIdentifier
                        val attributes = variant.attributes.keySet().associate {
                            it.name to variant.attributes.getAttribute(it).toString()
                        }
                        check(owner.group == "io.github.kotlinmania" && owner.module == "${requested.module}-android" && owner.version == version)
                        check(
                            attributes["org.jetbrains.kotlin.platform.type"] == "androidJvm" &&
                                attributes["org.gradle.libraryelements"] == "aar" &&
                                attributes["org.gradle.jvm.environment"] == "android",
                        )
                        selections += "${owner.group}:${owner.module}:${owner.version} $attributes"
                    }
                check(selections.size == expectedVersions.size) { "Not every root dependency selected an Android AAR" }
                val expected = setOf(
                    "concreteTypedStringAndIntCellsPreservePayloadsAndRejectDuplicates",
                    "genericSubclassAndNestedContainerPreserveRelationships",
                    "failedInitDoesNotPoisonOnceCellWhileLazyPoisonsOnPanic",
                    "mutationTakeAndCloneFromResetAndOverwriteDeterministically",
                    "raceCellsPreserveFirstWriterAndRejectZeroUsize",
                )
                val executed = mutableSetOf<String>()
                check(hostTests.size == 1)
                val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                hostTests.single().reports.junitXml.outputLocation.get().asFile.listFiles()
                    .orEmpty().filter { it.extension == "xml" }.forEach { xml ->
                        val cases = parser.parse(xml).getElementsByTagName("testcase")
                        for (index in 0 until cases.length) {
                            val case = cases.item(index) as Element
                            if (case.getAttribute("classname") == "consumer.PublicationConsumerTest") {
                                check(listOf("failure", "error", "skipped").none { case.getElementsByTagName(it).length > 0 })
                                check(executed.add(case.getAttribute("name")))
                            }
                        }
                    }
                check(executed == expected) { "Missing/extra/skipped consumer tests: $executed" }
                layout.buildDirectory.file("reports/publication/consumer.txt").get().asFile.apply {
                    parentFile.mkdirs()
                    writeText((selections + executed.sorted()).joinToString("\n", postfix = "\n"))
                }
            }
        }
    }
}

val producerProperties = Properties().apply {
    file("../../gradle.properties").inputStream().use { load(it) }
}
rootProject.extensions.configure<NodeJsEnvSpec>("kotlinNodeJsSpec") {
    version.set(producerProperties.getProperty("node.version"))
}
rootProject.extensions.configure<WasmNodeJsEnvSpec>("kotlinWasmNodeJsSpec") {
    version.set(producerProperties.getProperty("wasm.node.version"))
}
rootProject.extensions.configure<YarnRootEnvSpec>("kotlinYarnSpec") {
    version.set(producerProperties.getProperty("yarn.version"))
    yarnLockAutoReplace.set(true)
}
rootProject.extensions.configure<WasmYarnRootEnvSpec>("kotlinWasmYarnSpec") {
    version.set(producerProperties.getProperty("wasm.yarn.version"))
    yarnLockAutoReplace.set(true)
}
rootProject.extensions.configure<YarnRootExtension>("kotlinYarn") {
    producerProperties.stringPropertyNames().filter { it.startsWith("yarn.resolution.") }.forEach { key ->
        val module = key.removePrefix("yarn.resolution.")
        resolution(module, producerProperties.getProperty(key))
        resolution("**/$module", producerProperties.getProperty(key))
    }
}
rootProject.extensions.configure<NodeJsRootExtension>("kotlinNodeJs") {
    versions.mocha.version = producerProperties.getProperty("node.mocha.version")
    versions.kotlinWebHelpers.version = producerProperties.getProperty("node.kotlinWebHelpers.version")
}

listOf(
    "jvm" to "jvmTest",
    "macosArm64" to "macosArm64Test",
    "js" to "jsNodeTest",
    "wasmJs" to "wasmJsNodeTest",
    "wasmWasi" to "wasmWasiNodeTest",
).forEach { (target, test) ->
    val compilation = kotlin.targets.getByName(target).compilations.getByName("main")
    val dependencies = configurations.getByName(compilation.compileDependencyConfigurationName)
    val receipt = tasks.register("write${target.replaceFirstChar { it.uppercase() }}PublicationClasspath") {
        doLast {
            val entries = dependencies.incoming.artifacts.artifacts.map { artifact ->
                mapOf(
                    "component" to artifact.id.componentIdentifier.displayName,
                    "file" to artifact.file.absolutePath,
                    "sha256" to MessageDigest.getInstance("SHA-256").digest(artifact.file.readBytes())
                        .joinToString("") { "%02x".format(it) },
                    "attributes" to artifact.variant.attributes.keySet().associate {
                        it.name to artifact.variant.attributes.getAttribute(it).toString()
                    },
                )
            }
            layout.buildDirectory.file("reports/publication/$target-classpath.json").get().asFile.apply {
                parentFile.mkdirs()
                writeText(JsonOutput.prettyPrint(JsonOutput.toJson(entries)) + "\n")
            }
        }
    }
    tasks.named(test) {
        outputs.upToDateWhen { false }
        finalizedBy(receipt)
        if (target == "js") dependsOn("kotlinUpgradeYarnLock")
        if (target.startsWith("wasm")) dependsOn("kotlinWasmUpgradeYarnLock")
    }
}
