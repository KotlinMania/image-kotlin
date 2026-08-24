// port-lint: tests imageops/mod.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageopsTest {
    @Test
    fun testOverlayBoundsExt() {
        assertEquals(
            OverlayBoundsExtResult(0u, 0u, 0u, 0u, 10u, 10u),
            overlayBoundsExt(Pair(10u, 10u), Pair(10u, 10u), 0L, 0L),
        )
        assertEquals(
            OverlayBoundsExtResult(1u, 0u, 0u, 0u, 9u, 10u),
            overlayBoundsExt(Pair(10u, 10u), Pair(10u, 10u), 1L, 0L),
        )
        assertEquals(
            OverlayBoundsExtResult(0u, 0u, 0u, 0u, 0u, 0u),
            overlayBoundsExt(Pair(10u, 10u), Pair(10u, 10u), 0L, 11L),
        )
        assertEquals(
            OverlayBoundsExtResult(0u, 0u, 1u, 0u, 9u, 10u),
            overlayBoundsExt(Pair(10u, 10u), Pair(10u, 10u), -1L, 0L),
        )
        assertEquals(
            OverlayBoundsExtResult(0u, 0u, 0u, 0u, 0u, 0u),
            overlayBoundsExt(Pair(10u, 10u), Pair(10u, 10u), -10L, 0L),
        )
        assertEquals(
            OverlayBoundsExtResult(0u, 0u, 0u, 0u, 0u, 0u),
            overlayBoundsExt(Pair(10u, 10u), Pair(10u, 10u), 1L shl 50, 0L),
        )
        assertEquals(
            OverlayBoundsExtResult(0u, 0u, 0u, 0u, 0u, 0u),
            overlayBoundsExt(Pair(10u, 10u), Pair(10u, 10u), -(1L shl 50), 0L),
        )
        assertEquals(
            OverlayBoundsExtResult(0u, 0u, UInt.MAX_VALUE - 10u, 0u, 10u, 10u),
            overlayBoundsExt(Pair(10u, 10u), Pair(UInt.MAX_VALUE, 10u), 10L - UInt.MAX_VALUE.toLong(), 0L),
        )
    }

    @Test
    fun testImageInImage() {
        val target = ImageBuffer.createRgb(32u, 32u)
        val source = ImageBuffer.createRgb(16u, 16u)
        for (y in 0u until 16u) {
            for (x in 0u until 16u) {
                source.putPixel(x, y, Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()))
            }
        }
        overlay(target, source, 0L, 0L)
        assertEquals(Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(0u, 0u))
        assertEquals(Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(15u, 0u))
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(16u, 0u))
        assertEquals(Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(0u, 15u))
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(0u, 16u))
    }

    @Test
    fun testImageInImageOutsideOfBounds() {
        val target = ImageBuffer.createRgb(32u, 32u)
        val source = ImageBuffer.createRgb(32u, 32u)
        for (y in 0u until 32u) {
            for (x in 0u until 32u) {
                source.putPixel(x, y, Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()))
            }
        }
        overlay(target, source, 1L, 1L)
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(0u, 0u))
        assertEquals(Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(1u, 1u))
        assertEquals(Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(31u, 31u))
    }

    @Test
    fun testImageOutsideImageNoWrapAround() {
        val target = ImageBuffer.createRgb(32u, 32u)
        val source = ImageBuffer.createRgb(32u, 32u)
        for (y in 0u until 32u) {
            for (x in 0u until 32u) {
                source.putPixel(x, y, Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()))
            }
        }
        overlay(target, source, 33L, 33L)
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(0u, 0u))
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(1u, 1u))
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(31u, 31u))
    }

    @Test
    fun testImageCoordinateOverflow() {
        val target = ImageBuffer.createRgb(16u, 16u)
        val source = ImageBuffer.createRgb(32u, 32u)
        for (y in 0u until 32u) {
            for (x in 0u until 32u) {
                source.putPixel(x, y, Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()))
            }
        }
        overlay(
            target,
            source,
            UInt.MAX_VALUE.toLong() - 31L,
            UInt.MAX_VALUE.toLong() - 31L,
        )
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(0u, 0u))
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(1u, 1u))
        assertEquals(Rgb(0u.toUByte(), 0u.toUByte(), 0u.toUByte()), target.getPixel(15u, 15u))
    }

    @Test
    fun testImageHorizontalGradientLimits() {
        val img = ImageBuffer.createRgb(100u, 1u)
        val start = Rgb(0u.toUByte(), 128u.toUByte(), 0u.toUByte())
        val end = Rgb(255u.toUByte(), 255u.toUByte(), 255u.toUByte())

        horizontalGradient(img, start, end)

        assertEquals(start, img.getPixel(0u, 0u))
        assertEquals(end, img.getPixel(img.width() - 1u, 0u))
    }

    @Test
    fun testImageVerticalGradientLimits() {
        val img = ImageBuffer.createRgb(1u, 100u)
        val start = Rgb(0u.toUByte(), 128u.toUByte(), 0u.toUByte())
        val end = Rgb(255u.toUByte(), 255u.toUByte(), 255u.toUByte())

        verticalGradient(img, start, end)

        assertEquals(start, img.getPixel(0u, 0u))
        assertEquals(end, img.getPixel(0u, img.height() - 1u))
    }

    @Test
    fun testFastBlurZero() {
        val buf = ByteArray(50 * 50 * 4)
        val res = fastBlur(buf, 50, 50, 4, 0.0f)
        assertEquals(buf.size, res.size)
    }

    @Test
    fun testFastBlurNegative() {
        val buf = ByteArray(50 * 50 * 4)
        val res = fastBlur(buf, 50, 50, 4, -1.0f)
        assertEquals(buf.size, res.size)
    }

    @Test
    fun testFastLargeSigma() {
        val buf = ByteArray(1 * 1 * 4)
        val res = fastBlur(buf, 1, 1, 4, 50.0f)
        assertEquals(buf.size, res.size)
    }

    @Test
    fun testFastBlurEmpty() {
        val b0 = ByteArray(0)
        assertEquals(0, fastBlur(b0, 0, 0, 4, 1.0f).size)
        val b1 = ByteArray(0)
        assertEquals(0, fastBlur(b1, 20, 0, 4, 1.0f).size)
        val b2 = ByteArray(0)
        assertEquals(0, fastBlur(b2, 0, 20, 4, 1.0f).size)
    }

    @Test
    fun testFastBlur3Channels() {
        val buf = ByteArray(50 * 50 * 3)
        val res = fastBlur(buf, 50, 50, 3, 1.0f)
        assertEquals(buf.size, res.size)
    }

    @Test
    fun testFastBlur2Channels() {
        val buf = ByteArray(50 * 50 * 2)
        val res = fastBlur(buf, 50, 50, 2, 1.0f)
        assertEquals(buf.size, res.size)
    }

    @Test
    fun testFastBlur1Channels() {
        val buf = ByteArray(50 * 50 * 1)
        val res = fastBlur(buf, 50, 50, 1, 1.0f)
        assertEquals(buf.size, res.size)
    }
}
