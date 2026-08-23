// port-lint: tests images/dynimage.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.imageops.FilterType
import io.github.kotlinmania.image.metadata.Orientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class DynimageTests {
    @Test
    fun testEmptyFile() {
        assertFailsWith<ImageError> {
            loadFromMemory(byteArrayOf())
        }
    }

    private fun testGrayscale(img: DynamicImage, alphaDiscarded: Boolean) {
        img.putPixel(0u, 0u, Rgba(255u, 0u, 0u, 100u))
        val expectedAlpha: UByte = if (alphaDiscarded) 255u else 100u
        val pixel = img.grayscale().getPixel(0u, 0u)
        assertEquals(54u, pixel.r)
        assertEquals(54u, pixel.g)
        assertEquals(54u, pixel.b)
        assertEquals(expectedAlpha, pixel.a)
    }

    @Test
    fun testGrayscaleLuma8() {
        testGrayscale(DynamicImage.newLuma8(1u, 1u), alphaDiscarded = true)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.L8), alphaDiscarded = true)
    }

    @Test
    fun testGrayscaleLumaA8() {
        testGrayscale(DynamicImage.newLumaA8(1u, 1u), alphaDiscarded = false)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.La8), alphaDiscarded = false)
    }

    @Test
    fun testGrayscaleRgb8() {
        testGrayscale(DynamicImage.newRgb8(1u, 1u), alphaDiscarded = true)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.Rgb8), alphaDiscarded = true)
    }

    @Test
    fun testGrayscaleRgba8() {
        testGrayscale(DynamicImage.newRgba8(1u, 1u), alphaDiscarded = false)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.Rgba8), alphaDiscarded = false)
    }

    @Test
    fun testGrayscaleLuma16() {
        testGrayscale(DynamicImage.newLuma16(1u, 1u), alphaDiscarded = true)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.L16), alphaDiscarded = true)
    }

    @Test
    fun testGrayscaleLumaA16() {
        testGrayscale(DynamicImage.newLumaA16(1u, 1u), alphaDiscarded = false)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.La16), alphaDiscarded = false)
    }

    @Test
    fun testGrayscaleRgb16() {
        testGrayscale(DynamicImage.newRgb16(1u, 1u), alphaDiscarded = true)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.Rgb16), alphaDiscarded = true)
    }

    @Test
    fun testGrayscaleRgba16() {
        testGrayscale(DynamicImage.newRgba16(1u, 1u), alphaDiscarded = false)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.Rgba16), alphaDiscarded = false)
    }

    @Test
    fun testGrayscaleRgb32F() {
        testGrayscale(DynamicImage.newRgb32F(1u, 1u), alphaDiscarded = true)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.Rgb32F), alphaDiscarded = true)
    }

    @Test
    fun testGrayscaleRgba32F() {
        testGrayscale(DynamicImage.newRgba32F(1u, 1u), alphaDiscarded = false)
        testGrayscale(DynamicImage.new(1u, 1u, ColorType.Rgba32F), alphaDiscarded = false)
    }

    @Test
    fun testToVecU8() {
        val b1 = DynamicImage.newLuma8(1u, 1u).intoBytes()
        assertEquals(1, b1.size)
        val b2 = DynamicImage.newLuma16(1u, 1u).intoBytes()
        assertEquals(2, b2.size)
    }

    @Test
    fun issue1705CanTurn16bitImageIntoBytes() {
        val bytesIn = ByteArray(64 * 64 * 2) { (-1).toByte() }
        val img = DynamicImage.ImageLuma16(createGray16(64u, 64u, bytesIn))
        assertNotNull(img.asLuma16())

        val bytes = img.intoBytes()
        assertEquals(64 * 64 * 2, bytes.size)
        for (b in bytes) {
            assertEquals((-1).toByte(), b)
        }
    }

    @Test
    fun testConvertTo() {
        val imageLuma8 = DynamicImage.newLuma8(1u, 1u)
        val imageLuma16 = DynamicImage.newLuma16(1u, 1u)
        assertEquals(imageLuma8.toLuma16().asRaw().size, imageLuma16.toLuma16().asRaw().size)
    }

    @Test
    fun testInvert() {
        val img = DynamicImage.newRgba8(2u, 2u)
        img.putPixel(0u, 0u, Rgba(10u, 20u, 30u, 255u))
        img.invert()
        val p = img.getPixel(0u, 0u)
        assertEquals(245u, p.r)
        assertEquals(235u, p.g)
        assertEquals(225u, p.b)
        assertEquals(255u, p.a)
    }

    @Test
    fun testRotateAndFlip() {
        val img = DynamicImage.newRgb8(3u, 2u)
        img.putPixel(0u, 0u, Rgba(255u, 0u, 0u, 255u))
        val r90 = img.rotate90()
        assertEquals(2u, r90.width())
        assertEquals(3u, r90.height())

        val r180 = img.rotate180()
        assertEquals(3u, r180.width())
        assertEquals(2u, r180.height())

        val r270 = img.rotate270()
        assertEquals(2u, r270.width())
        assertEquals(3u, r270.height())

        val fliph = img.fliph()
        assertEquals(3u, fliph.width())
        assertEquals(2u, fliph.height())

        val flipv = img.flipv()
        assertEquals(3u, flipv.width())
        assertEquals(2u, flipv.height())
    }

    @Test
    fun testApplyOrientation() {
        val img = DynamicImage.newRgb8(4u, 2u)
        val noTransform = img.applyOrientation(Orientation.NoTransforms)
        assertEquals(4u, noTransform.width())
        assertEquals(2u, noTransform.height())

        val rot90 = img.applyOrientation(Orientation.Rotate90)
        assertEquals(2u, rot90.width())
        assertEquals(4u, rot90.height())
    }

    @Test
    fun testResizeAndThumbnail() {
        val img = DynamicImage.newRgb8(10u, 10u)
        val resized = img.resize(5u, 5u, FilterType.Nearest)
        assertEquals(5u, resized.width())
        assertEquals(5u, resized.height())

        val thumb = img.thumbnail(4u, 4u)
        assertEquals(4u, thumb.width())
        assertEquals(4u, thumb.height())

        val filled = img.resizeToFill(8u, 4u, FilterType.Nearest)
        assertEquals(8u, filled.width())
        assertEquals(4u, filled.height())
    }

    @Test
    fun testBlurAndFilter() {
        val img = DynamicImage.newRgb8(5u, 5u)
        val blurred = img.blur(1.0f)
        assertEquals(5u, blurred.width())
        assertEquals(5u, blurred.height())

        val fastBlurred = img.fastBlur(1.0f)
        assertEquals(5u, fastBlurred.width())
        assertEquals(5u, fastBlurred.height())

        val unsharp = img.unsharpen(1.0f, 2)
        assertEquals(5u, unsharp.width())
        assertEquals(5u, unsharp.height())

        val kernel =
            floatArrayOf(
                0.0f,
                -1.0f,
                0.0f,
                -1.0f,
                5.0f,
                -1.0f,
                0.0f,
                -1.0f,
                0.0f,
            )
        val filtered = img.filter3x3(kernel)
        assertEquals(5u, filtered.width())
        assertEquals(5u, filtered.height())
    }

    @Test
    fun testAdjustments() {
        val img = DynamicImage.newRgb8(2u, 2u)
        val contrasted = img.adjustContrast(0.5f)
        assertEquals(2u, contrasted.width())

        val brightened = img.brighten(10)
        assertEquals(2u, brightened.width())

        val hueRotated = img.huerotate(90)
        assertEquals(2u, hueRotated.width())
    }

    @Test
    fun testCrop() {
        val img = DynamicImage.newRgb8(10u, 10u)
        val cropped = img.crop(2u, 3u, 4u, 5u)
        assertEquals(4u, cropped.width())
        assertEquals(5u, cropped.height())
    }
}
