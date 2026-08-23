// port-lint: tests imageops/colorops.rs
package io.github.kotlinmania.image.imageops

import kotlin.test.Test
import kotlin.test.assertEquals

class ColoropsTest {
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
    }

    @Test
    fun testGrayscale() {
        val rgb = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte())
        val gray = grayscale(rgb, 1, 1, 3)
        assertEquals(1, gray.size)
        assertEquals(255.toByte(), gray[0])
    }

    @Test
    fun testInvert() {
        val img = byteArrayOf(0, 1, 2, 10, 11, 12)
        invert(img, 1)
        val expected = byteArrayOf(255.toByte(), 254.toByte(), 253.toByte(), 245.toByte(), 244.toByte(), 243.toByte())
        assertEquals(expected.toList(), img.toList())
    }

    @Test
    fun testBrighten() {
        val img = byteArrayOf(0, 1, 2, 10, 11, 12)
        val bright = brighten(img, 3, 2, 1, 10)
        val expected = byteArrayOf(10, 11, 12, 20, 21, 22)
        assertEquals(expected.toList(), bright.toList())
    }

    @Test
    fun testBrightenPlace() {
        val img = byteArrayOf(0, 1, 2, 10, 11, 12)
        brightenInPlace(img, 1, 10)
        val expected = byteArrayOf(10, 11, 12, 20, 21, 22)
        assertEquals(expected.toList(), img.toList())
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
