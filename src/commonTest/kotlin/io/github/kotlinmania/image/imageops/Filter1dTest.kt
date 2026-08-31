// port-lint: tests image/src/imageops/filter_1d.rs
package io.github.kotlinmania.image.imageops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Filter1dTest {
    @Test
    fun testKernelShapeAndSize() {
        val shape = KernelShape(3, 5)
        assertEquals(3, shape.width)
        assertEquals(5, shape.height)

        val size = FilterImageSize(10, 20)
        assertEquals(10, size.width)
        assertEquals(20, size.height)
    }

    @Test
    fun testPrepareSymmetricKernel() {
        val kernelFloat = floatArrayOf(0f, 0.25f, 0.5f, 0.25f, 0f)
        val trimmedFloat = prepareSymmetricKernel(kernelFloat)
        assertEquals(3, trimmedFloat.size)
        assertEquals(0.25f, trimmedFloat[0], 1e-5f)
        assertEquals(0.5f, trimmedFloat[1], 1e-5f)
        assertEquals(0.25f, trimmedFloat[2], 1e-5f)

        val kernelInt = intArrayOf(0, 0, 100, 200, 100, 0, 0)
        val trimmedInt = prepareSymmetricKernel(kernelInt)
        assertEquals(3, trimmedInt.size)
        assertEquals(100, trimmedInt[0])
        assertEquals(200, trimmedInt[1])
        assertEquals(100, trimmedInt[2])
    }

    @Test
    fun testFilter2dSeparableIdentity() {
        val width = 4
        val height = 4
        val size = FilterImageSize(width, height)
        val image = ByteArray(width * height * 3) { (it % 256).toByte() }
        val dest = ByteArray(image.size)
        val kernel = floatArrayOf(1.0f)

        filter2dSepRgb(image, dest, size, kernel, kernel)

        for (i in image.indices) {
            val expected = image[i].toInt() and 0xFF
            val actual = dest[i].toInt() and 0xFF
            assertTrue(kotlin.math.abs(expected - actual) <= 1, "Pixel $i mismatch: expected $expected, got $actual")
        }
    }

    @Test
    fun testFilter2dSeparablePlane() {
        val width = 5
        val height = 5
        val size = FilterImageSize(width, height)
        val image = ByteArray(width * height) { 100.toByte() }
        val dest = ByteArray(image.size)
        val kernel = floatArrayOf(0.25f, 0.5f, 0.25f)

        filter2dSepPlane(image, dest, size, kernel, kernel)

        for (i in dest.indices) {
            val v = dest[i].toInt() and 0xFF
            // Constant color should remain constant under normalized symmetric filter
            assertTrue(kotlin.math.abs(v - 100) <= 2, "Pixel $i value $v should be close to 100")
        }
    }

    @Test
    fun testFilter2dSeparableRgbaF32() {
        val width = 4
        val height = 4
        val size = FilterImageSize(width, height)
        val image = FloatArray(width * height * 4) { 1.0f }
        val dest = FloatArray(image.size)
        val kernel = floatArrayOf(0.25f, 0.5f, 0.25f)

        filter2dSepRgbaF32(image, dest, size, kernel, kernel)

        for (i in dest.indices) {
            assertTrue(kotlin.math.abs(dest[i] - 1.0f) < 1e-4f, "Float pixel $i: expected 1.0, got ${dest[i]}")
        }
    }

    @Test
    fun testFilter2dSeparableU16() {
        val width = 4
        val height = 4
        val size = FilterImageSize(width, height)
        val image = ShortArray(width * height * 3) { 30000.toShort() }
        val dest = ShortArray(image.size)
        val kernel = floatArrayOf(0.25f, 0.5f, 0.25f)

        filter2dSepRgbU16(image, dest, size, kernel, kernel)

        for (i in dest.indices) {
            val v = dest[i].toInt() and 0xFFFF
            assertTrue(kotlin.math.abs(v - 30000) <= 5, "U16 pixel $i value $v should be close to 30000")
        }
    }
}
