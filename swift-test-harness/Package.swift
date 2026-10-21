// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "SwiftTestHarness",
    platforms: [
        .macOS("15.0"),
    ],
    dependencies: [
        .package(name: "OnceCell", path: "../build/SPMPackage/macosArm64/Debug")
    ],
    targets: [
        .testTarget(
            name: "SwiftTestHarnessTests",
            dependencies: [
                .product(name: "OnceCellLibrary", package: "OnceCell")
            ],
            linkerSettings: [
                .unsafeFlags([
                    "-L", "../build/swift-test",
                    "-L", "../build/MergedLibraries/macos/Debug",
                    "-lOnceCell",
                ]),
            ]
        ),
    ]
)