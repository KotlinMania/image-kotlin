// port-lint: source imageops/colorops.rs
package io.github.kotlinmania.image.imageops

import kotlin.test.Test
import kotlin.test.assertEquals

class ColoropsTest {
    @Test
    fun testInvert() {
        val img = byteArrayOf(0, 100, 200.toByte(), 255.toByte())
        invert(img, 4) // 4 channels -> alpha at index 3 is untouched
        assertEquals(255.toByte(), img[0])
        assertEquals(155.toByte(), img[1])
        assertEquals(55.toByte(), img[2])
        assertEquals(255.toByte(), img[3])
    }

    @Test
    fun testGrayscale() {
        val rgb = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte())
        val gray = grayscale(rgb, 1, 1, 3)
        assertEquals(1, gray.size)
        assertEquals(255.toByte(), gray[0])
    }

    @Test
    fun testBrighten() {
        val img = byteArrayOf(10, 20, 30, 255.toByte())
        val bright = brighten(img, 1, 1, 4, 15)
        assertEquals(25.toByte(), bright[0])
        assertEquals(35.toByte(), bright[1])
        assertEquals(45.toByte(), bright[2])
        assertEquals(255.toByte(), bright[3])
    }

    @Test
    fun testHueRotateIdentity() {
        val img = byteArrayOf(100, 150.toByte(), 200.toByte(), 255.toByte())
        val rot0 = huerotate(img, 1, 1, 4, 0)
        // Check 0 degrees leaves pixel effectively unchanged
        assertEquals(img[0], rot0[0])
        assertEquals(img[1], rot0[1])
        assertEquals(img[2], rot0[2])
        assertEquals(img[3], rot0[3])
    }
}
