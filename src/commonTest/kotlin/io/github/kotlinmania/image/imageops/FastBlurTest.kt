// port-lint: tests imageops/fast_blur.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FastBlurTest {
    private class Rng(private var state: ULong) {
        companion object {
            fun new(seed: ULong): Rng = Rng(seed)
        }

        fun nextU32(): UInt {
            state = state * 6364136223846793005uL + 1uL
            return (state shr 32).toUInt()
        }

        fun nextU8(): Int = (nextU32() % 256u).toInt()

        fun nextF32InRange(a: Float, b: Float): Float {
            val u = nextU32()
            val unit = u.toFloat() / (UInt.MAX_VALUE.toFloat() + 1.0f)
            return a + (b - a) * unit
        }
    }

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

    @Test
    fun testBoxBlur() {
        val rng = Rng(1234567890123456789uL)
        for (iter in 0 until 35) {
            val width = rng.nextU8()
            val height = rng.nextU8()
            val sigma = rng.nextF32InRange(0.1f, 100.0f)
            val px = rng.nextU8().toByte()
            val cn = rng.nextU8()
            if (width == 0 || height == 0 || sigma <= 0f) {
                continue
            }
            when (cn % 4) {
                0 -> {
                    val vc = ByteArray(width * height) { px }
                    val img = ImageBuffer.createGray(width.toUInt(), height.toUInt(), vc)!!
                    val dyn = DynamicImage.ImageLuma8(img)
                    val res = dyn.fastBlur(sigma)
                    for (clr in res.asBytes()) {
                        assertEquals(px, clr)
                    }
                }
                1 -> {
                    val vc = ByteArray(width * height * 2) { px }
                    val img = ImageBuffer.createGrayAlpha(width.toUInt(), height.toUInt(), vc)!!
                    val dyn = DynamicImage.ImageLumaA8(img)
                    val res = dyn.fastBlur(sigma)
                    for (clr in res.asBytes()) {
                        assertEquals(px, clr)
                    }
                }
                2 -> {
                    val vc = ByteArray(width * height * 3) { px }
                    val img = ImageBuffer.createRgb(width.toUInt(), height.toUInt(), vc)!!
                    val dyn = DynamicImage.ImageRgb8(img)
                    val res = dyn.fastBlur(sigma)
                    for (clr in res.asBytes()) {
                        assertEquals(px, clr)
                    }
                }
                3 -> {
                    val vc = ByteArray(width * height * 4) { px }
                    val img = ImageBuffer.createRgba(width.toUInt(), height.toUInt(), vc)!!
                    val dyn = DynamicImage.ImageRgba8(img)
                    val res = dyn.fastBlur(sigma)
                    for (clr in res.asBytes()) {
                        assertEquals(px, clr)
                    }
                }
            }
        }
    }
}
