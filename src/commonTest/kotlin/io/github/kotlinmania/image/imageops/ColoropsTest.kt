// port-lint: tests image/src/imageops/colorops.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.GrayImage
import io.github.kotlinmania.image.images.GenericImageView
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColoropsTest {
    private fun <P> assertPixelsEq(actual: GenericImageView<P>, expected: GenericImageView<P>) {
        val actualDim = actual.dimensions()
        val expectedDim = expected.dimensions()
        assertEquals(expectedDim, actualDim, "dimensions do not match")
        val diffs = pixelDiffs(actual, expected)
        assertTrue(diffs.isEmpty(), "pixels do not match: $diffs")
    }

    @Test
    fun testDither() {
        val image = byteArrayOf(127, 127, 127, 127)
        val cmap = BiLevel
        dither(image, 2, 2, cmap)
        val expectedDither = byteArrayOf(0, 0xFF.toByte(), 0xFF.toByte(), 0)
        assertEquals(expectedDither.toList(), image.toList())
        val indexed = indexColors(image, 2, 2, cmap)
        val expectedIndexed = byteArrayOf(0, 1, 1, 0)
        assertEquals(expectedIndexed.toList(), indexed.toList())

        val imgBuf = ImageBuffer.createGray(2u, 2u, byteArrayOf(127, 127, 127, 127))!!
        dither(imgBuf, cmap)
        assertEquals(expectedDither.toList(), imgBuf.asRaw().toList())
        val indexedBuf = indexColors(imgBuf, cmap)
        assertEquals(expectedIndexed.toList(), indexedBuf.asRaw().toList())
    }

    @Test
    fun testGrayscale() {
        val rgb = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte())
        val gray = grayscale(rgb, 1, 1, 3)
        assertEquals(1, gray.size)
        assertEquals(255.toByte(), gray[0])

        val image: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expected: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        assertPixelsEq(grayscale(image), expected)
    }

    @Test
    fun testInvert() {
        val img = byteArrayOf(0, 1, 2, 10, 11, 12)
        invert(img, 1)
        val expected = byteArrayOf(255.toByte(), 254.toByte(), 253.toByte(), 245.toByte(), 244.toByte(), 243.toByte())
        assertEquals(expected.toList(), img.toList())

        val image: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(255.toByte(), 254.toByte(), 253.toByte(), 245.toByte(), 244.toByte(), 243.toByte()))!!
        invert(image)
        assertPixelsEq(image, expectedBuf)
    }

    @Test
    fun testBrighten() {
        val img = byteArrayOf(0, 1, 2, 10, 11, 12)
        val bright = brighten(img, 3, 2, 1, 10)
        val expected = byteArrayOf(10, 11, 12, 20, 21, 22)
        assertEquals(expected.toList(), bright.toList())

        val image: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(10, 11, 12, 20, 21, 22))!!
        assertPixelsEq(brighten(image, 10), expectedBuf)
    }

    @Test
    fun testBrightenPlace() {
        val img = byteArrayOf(0, 1, 2, 10, 11, 12)
        brightenInPlace(img, 1, 10)
        val expected = byteArrayOf(10, 11, 12, 20, 21, 22)
        assertEquals(expected.toList(), img.toList())

        val image: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(10, 11, 12, 20, 21, 22))!!
        brightenInPlace(image, 10)
        assertPixelsEq(image, expectedBuf)
    }

    @Test
    fun testHueRotateIdentity() {
        val img = byteArrayOf(100, 150.toByte(), 200.toByte(), 255.toByte())
        val rot0 = huerotate(img, 1, 1, 4, 0)
        assertEquals(img[0], rot0[0])
        assertEquals(img[1], rot0[1])
        assertEquals(img[2], rot0[2])
        assertEquals(img[3], rot0[3])
    }
}

