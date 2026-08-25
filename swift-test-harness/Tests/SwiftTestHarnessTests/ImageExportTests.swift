#if canImport(Testing)
import Testing
import Image

@Suite struct ImageExportTests {
    @Test func testSwiftModuleLoads() {
        #expect(Bool(true), "Image swift module imported cleanly")
    }

    @Test func testRectFieldsRoundTripFromSwift() {
        let rect = ExportedKotlinPackages.io.github.kotlinmania.image.math.Rect(
            x: 10,
            y: 20,
            width: 100,
            height: 200
        )
        #expect(rect.x == 10)
        #expect(rect.y == 20)
        #expect(rect.width == 100)
        #expect(rect.height == 200)
    }

    @Test func testRectEqualityFromSwift() {
        let lhs = ExportedKotlinPackages.io.github.kotlinmania.image.math.Rect(
            x: 0, y: 0, width: 5, height: 5
        )
        let rhs = ExportedKotlinPackages.io.github.kotlinmania.image.math.Rect(
            x: 0, y: 0, width: 5, height: 5
        )
        let diff = ExportedKotlinPackages.io.github.kotlinmania.image.math.Rect(
            x: 1, y: 0, width: 5, height: 5
        )
        #expect(lhs.equals(other: rhs))
        #expect(!lhs.equals(other: diff))
    }

    @Test func testOrientationToExifMatchesCanonicalExifMapping() {
        #expect(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.NoTransforms.toExif() == 1
        )
        #expect(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.FlipHorizontal.toExif() == 2
        )
        #expect(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate180.toExif() == 3
        )
        #expect(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.FlipVertical.toExif() == 4
        )
        #expect(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate90FlipH.toExif() == 5
        )
        #expect(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate90.toExif() == 6
        )
        #expect(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate270FlipH.toExif() == 7
        )
        #expect(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate270.toExif() == 8
        )
    }

    @Test func testDxtVariantFromSwift() {
        let dxt1 = ExportedKotlinPackages.io.github.kotlinmania.image.codecs.DxtVariant.DXT1
        #expect(dxt1.decodedBytesPerBlock() == 48)
        #expect(dxt1.encodedBytesPerBlock() == 8)

        let dxt3 = ExportedKotlinPackages.io.github.kotlinmania.image.codecs.DxtVariant.DXT3
        #expect(dxt3.decodedBytesPerBlock() == 64)
        #expect(dxt3.encodedBytesPerBlock() == 16)

        let dxt5 = ExportedKotlinPackages.io.github.kotlinmania.image.codecs.DxtVariant.DXT5
        #expect(dxt5.decodedBytesPerBlock() == 64)
        #expect(dxt5.encodedBytesPerBlock() == 16)
    }
}
#elseif canImport(XCTest)
import XCTest
import Image

final class ImageExportTests: XCTestCase {
    func testSwiftModuleLoads() {
        XCTAssertTrue(true, "Image swift module imported cleanly")
    }

    func testRectFieldsRoundTripFromSwift() {
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

    func testRectEqualityFromSwift() {
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

    func testOrientationToExifMatchesCanonicalExifMapping() {
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.NoTransforms.toExif(), 1
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.FlipHorizontal.toExif(), 2
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate180.toExif(), 3
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.FlipVertical.toExif(), 4
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate90FlipH.toExif(), 5
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate90.toExif(), 6
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate270FlipH.toExif(), 7
        )
        XCTAssertEqual(
            ExportedKotlinPackages.io.github.kotlinmania.image.metadata.Orientation.Rotate270.toExif(), 8
        )
    }

    func testDxtVariantFromSwift() {
        let dxt1 = ExportedKotlinPackages.io.github.kotlinmania.image.codecs.DxtVariant.DXT1
        XCTAssertEqual(dxt1.decodedBytesPerBlock(), 48)
        XCTAssertEqual(dxt1.encodedBytesPerBlock(), 8)

        let dxt3 = ExportedKotlinPackages.io.github.kotlinmania.image.codecs.DxtVariant.DXT3
        XCTAssertEqual(dxt3.decodedBytesPerBlock(), 64)
        XCTAssertEqual(dxt3.encodedBytesPerBlock(), 16)

        let dxt5 = ExportedKotlinPackages.io.github.kotlinmania.image.codecs.DxtVariant.DXT5
        XCTAssertEqual(dxt5.decodedBytesPerBlock(), 64)
        XCTAssertEqual(dxt5.encodedBytesPerBlock(), 16)
    }
}
#endif
