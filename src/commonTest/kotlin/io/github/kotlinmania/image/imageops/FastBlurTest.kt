// port-lint: tests imageops/fast_blur.rs
package io.github.kotlinmania.image.imageops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FastBlurTest {
    @Test
    fun testBoxesForGauss() {
        val boxes = boxesForGauss(2.0f, 3)
        assertEquals(3, boxes.size)
        for (b in boxes) {
            assertTrue(b > 0)
            assertEquals(1, b % 2) // must be odd
        }
    }

    @Test
    fun testFastBlurImage() {
        val width = 10
        val height = 10
        val channels = 1
        val image = ByteArray(width * height) { 0 }
        image[5 * width + 5] = 255.toByte() // Single impulse point

        val blurred = fastBlur(image, width, height, channels, 1.5f)
        assertEquals(100, blurred.size)
        // Center pixel should have lower value now, surrounding pixels should be non-zero
        val centerVal = blurred[5 * width + 5].toInt() and 0xFF
        assertTrue(centerVal in 1..254)
        val neighborVal = blurred[5 * width + 6].toInt() and 0xFF
        assertTrue(neighborVal > 0)
    }

    @Test
    fun testFastBlurEmpty() {
        val empty = ByteArray(0)
        val result = fastBlur(empty, 0, 0, 1, 1.0f)
        assertEquals(0, result.size)
    }
}
