import XCTest
import Image

// Parity tests for the Kotlin → Swift Export → SPM → swift test pipeline.
//
// The presence and successful compilation of this file proves three layers
// of the pipeline:
//
//   1. `embedSwiftExportForXcode` produced `Image.swiftmodule/`
//      and the supporting KotlinRuntimeSupport / ExportedKotlinPackages /
//      KotlinRuntime swiftmodule bundles. If any of them were missing,
//      `import Image` above would fail at compile time.
//
//   2. The static archive `libImage.a` (produced by the
//      `linkSwiftExportBinaryDebugStaticMacosArm64` and
//      `mergeMacosDebugSwiftExportLibraries` tasks) supplied every
//      `__root____*` and `KotlinError`-related symbol the Swift modules
//      reference.
//
//   3. The Kotlin `swiftExport { moduleName = "Image" }` and
//      `flattenPackage = "io.github.kotlinmania.image"` configuration in
//      build.gradle.kts produced a module name reachable from this
//      Package.swift via the `ImageLibrary` product.
//
// Beyond the pipeline canary, the test cases below exercise real exported
// API: `image.math.Rect` and `image.metadata.Orientation`. They mirror the
// commonTest coverage of those Kotlin types where the Swift Export bridge
// supports the return-type shape. Methods on `Orientation.Companion` that
// return `Orientation?` (`fromExif`, `fromExifChunk`, `removeFromExifChunk`)
// are exercised by the Kotlin commonTest only — those Companion methods
// are hidden from the Swift bridge via `@HiddenFromObjC` because the
// Kotlin/Native enum-by-reference Swift Export bridge currently fails the
// pointer-identity switch in the synthesized
// `init(__externalRCRefUnsafe:options:)` for enum-class return values.
// See SWIFT_EXPORT_ROLLOUT.md for the rationale.
final class ImageExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Image swift module imported cleanly")
    }

    func testRectFieldsRoundTripFromSwift() throws {
        let rect = ExportedKotlinPackages.io.github.kotlinmania.image.math.Rect(
            x: 10,
            y: 20,
            width: 100,
            height: 200
        )
        XCTAssertEqual(rect.x, 10)
        XCTAssertEqual(rect.y, 20)
        XCTAssertEqual(rect.width, 100)
        XCTAssertEqual(rect.height, 200)
    }

    func testRectEqualityFromSwift() throws {
        let lhs = ExportedKotlinPackages.io.github.kotlinmania.image.math.Rect(
            x: 0, y: 0, width: 5, height: 5
        )
        let rhs = ExportedKotlinPackages.io.github.kotlinmania.image.math.Rect(
            x: 0, y: 0, width: 5, height: 5
        )
        let diff = ExportedKotlinPackages.io.github.kotlinmania.image.math.Rect(
            x: 1, y: 0, width: 5, height: 5
        )
        XCTAssertTrue(lhs.equals(other: rhs))
        XCTAssertFalse(lhs.equals(other: diff))
    }

    // toExif() returns Int32 (mapped from Kotlin Int), which crosses the
    // Swift Export bridge cleanly. The canonical EXIF mapping (1..8) is the
    // contract that the upstream Rust crate and our Kotlin commonTest both
    // exercise.
    func testOrientationToExifMatchesCanonicalExifMapping() throws {
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.NoTransforms.toExif(),
            1
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.FlipHorizontal.toExif(),
            2
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate180.toExif(),
            3
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.FlipVertical.toExif(),
            4
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate90FlipH.toExif(),
            5
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate90.toExif(),
            6
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate270FlipH.toExif(),
            7
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate270.toExif(),
            8
        )
    }
}
