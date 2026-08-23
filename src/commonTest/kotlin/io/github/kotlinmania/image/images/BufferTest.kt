// port-lint: tests images/buffer.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.math.Rect
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BufferTest {
    @Test
    fun testSliceBuffer() {
        val data = ByteArray(9)
        val buf = ImageBuffer.createGray(3u, 3u, data)!!
        assertContentEquals(data, buf.asRaw())
    }

    @Test
    fun testLumaU8ZeroTest() {
        val buffer = ImageBuffer.createGray(2u, 2u)
        assertTrue(buffer.asRaw().all { it == 0.toByte() })
    }

    @Test
    fun testRgbU8ZeroTest() {
        val buffer = ImageBuffer.createRgb(2u, 2u)
        assertTrue(buffer.asRaw().all { it == 0.toByte() })
    }

    @Test
    fun testRgbAU8ZeroTest() {
        val buffer = ImageBuffer.createRgba(2u, 2u)
        assertTrue(buffer.asRaw().all { it == 0.toByte() })
    }

    @Test
    fun testGetPixel() {
        val a = ImageBuffer.createRgb(10u, 10u)
        a.asRaw()[3 * 10] = 255.toByte()
        assertEquals(255u.toUByte(), a.getPixel(0u, 1u).r)
    }

    @Test
    fun testGetPixelChecked() {
        val a = ImageBuffer.createRgb(10u, 10u)
        a.putPixel(0u, 1u, Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()))

        assertEquals(Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()), a.getPixelChecked(0u, 1u))
        assertEquals(a.getPixelChecked(0u, 1u), a.getPixel(0u, 1u))
        assertNull(a.getPixelChecked(10u, 0u))
        assertNull(a.getPixelChecked(0u, 10u))

        val white = Rgb(255u.toUByte(), 255u.toUByte(), 255u.toUByte())
        val b = ImageBuffer.createRgb(2u, 1u)
        b.putPixel(1u, 0u, white)

        assertEquals(white, b.getPixelChecked(1u, 0u))
        assertEquals(b.getPixelChecked(1u, 0u), b.getPixel(1u, 0u))
    }

    @Test
    fun testMutIter() {
        val a = ImageBuffer.createRgb(10u, 10u)
        a.putPixel(0u, 0u, Rgb(42u.toUByte(), 0u.toUByte(), 0u.toUByte()))
        assertEquals(42.toByte(), a.asRaw()[0])
    }

    @Test
    fun testZeroWidthZeroHeight() {
        val image = ImageBuffer.createRgb(0u, 0u)
        assertEquals(0, image.rowsMut().size)
        assertEquals(0, image.pixelsMut().size)
        assertEquals(0, image.rows().size)
        assertFalse(image.pixels().hasNext())
    }

    @Test
    fun testZeroWidthNonzeroHeight() {
        val image = ImageBuffer.createRgb(0u, 2u)
        assertEquals(0, image.rowsMut().size)
        assertEquals(0, image.pixelsMut().size)
        assertEquals(0, image.rows().size)
        assertFalse(image.pixels().hasNext())
    }

    @Test
    fun testNonzeroWidthZeroHeight() {
        val image = ImageBuffer.createRgb(2u, 0u)
        assertEquals(0, image.rowsMut().size)
        assertEquals(0, image.pixelsMut().size)
        assertEquals(0, image.rows().size)
        assertFalse(image.pixels().hasNext())
    }

    @Test
    fun testPixelsOnLargeBuffer() {
        val image = ImageBuffer.createRgb(1u, 1u, ByteArray(6))!!
        assertEquals(1, image.pixels().asSequence().count())
        assertEquals(1, image.enumeratePixels().size)
        assertEquals(1, image.pixelsMut().size)
        assertEquals(1, image.enumeratePixelsMut().size)
        assertEquals(1, image.rows().size)
        assertEquals(1, image.rowsMut().size)
    }

    @Test
    fun testDefault() {
        val image = ImageBuffer.createRgb(0u, 0u)
        assertEquals(Pair(0u, 0u), image.dimensions())
    }

    @Test
    fun testImageBufferCopyWithinOob() {
        val image = ImageBuffer.createGray(4u, 4u, ByteArray(16))!!
        assertFalse(image.copyWithin(Rect(0u, 0u, 5u, 4u), 0u, 0u))
        assertFalse(image.copyWithin(Rect(0u, 0u, 4u, 5u), 0u, 0u))
        assertFalse(image.copyWithin(Rect(1u, 0u, 4u, 4u), 0u, 0u))
        assertFalse(image.copyWithin(Rect(0u, 0u, 4u, 4u), 1u, 0u))
        assertFalse(image.copyWithin(Rect(0u, 1u, 4u, 4u), 0u, 0u))
        assertFalse(image.copyWithin(Rect(0u, 0u, 4u, 4u), 0u, 1u))
        assertFalse(image.copyWithin(Rect(1u, 1u, 4u, 4u), 0u, 0u))
    }

    @Test
    fun testImageBufferCopyWithinTl() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val expected = byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 8, 4, 5, 6, 12, 8, 9, 10)
        val image = ImageBuffer.createGray(4u, 4u, data)!!
        assertTrue(image.copyWithin(Rect(0u, 0u, 3u, 3u), 1u, 1u))
        assertContentEquals(expected, image.intoRaw())
    }

    @Test
    fun testImageBufferCopyWithinTr() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val expected = byteArrayOf(0, 1, 2, 3, 1, 2, 3, 7, 5, 6, 7, 11, 9, 10, 11, 15)
        val image = ImageBuffer.createGray(4u, 4u, data)!!
        assertTrue(image.copyWithin(Rect(1u, 0u, 3u, 3u), 0u, 1u))
        assertContentEquals(expected, image.intoRaw())
    }

    @Test
    fun testImageBufferCopyWithinBl() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val expected = byteArrayOf(0, 4, 5, 6, 4, 8, 9, 10, 8, 12, 13, 14, 12, 13, 14, 15)
        val image = ImageBuffer.createGray(4u, 4u, data)!!
        assertTrue(image.copyWithin(Rect(0u, 1u, 3u, 3u), 1u, 0u))
        assertContentEquals(expected, image.intoRaw())
    }

    @Test
    fun testImageBufferCopyWithinBr() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val expected = byteArrayOf(5, 6, 7, 3, 9, 10, 11, 7, 13, 14, 15, 11, 12, 13, 14, 15)
        val image = ImageBuffer.createGray(4u, 4u, data)!!
        assertTrue(image.copyWithin(Rect(1u, 1u, 3u, 3u), 0u, 0u))
        assertContentEquals(expected, image.intoRaw())
    }
}
