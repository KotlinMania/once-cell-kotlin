pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }
dependencyResolutionManagement {
    repositories {
        // An explicit producer staging repository is allowed; never mavenLocal or source substitution.
        providers.gradleProperty("producerRepository").orNull?.let { location ->
            exclusiveContent {
                forRepository { maven { url = uri(location) } }
                filter {
                    includeModule("io.github.kotlinmania", "once-cell-kotlin")
                    includeModule("io.github.kotlinmania", "once-cell-kotlin-android")
                    includeModule("io.github.kotlinmania", "once-cell-kotlin-jvm")
                    includeModule("io.github.kotlinmania", "once-cell-kotlin-macosarm64")
                    includeModule("io.github.kotlinmania", "once-cell-kotlin-js")
                    includeModule("io.github.kotlinmania", "once-cell-kotlin-wasm-js")
                    includeModule("io.github.kotlinmania", "once-cell-kotlin-wasm-wasi")
                }
            }
        }
        google()
        mavenCentral()
    }
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
rootProject.name = "once-cell-publication-consumer"
