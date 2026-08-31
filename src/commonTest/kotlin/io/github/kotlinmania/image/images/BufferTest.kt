// port-lint: tests images/buffer.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.math.Rect
import io.github.kotlinmania.image.metadata.Cicp
import io.github.kotlinmania.image.metadata.CicpTransform
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun colorConversion() {
        val source = ImageBuffer.createRgb(128u, 128u) { _, _ -> Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()) }
        val target = ImageBuffer.createRgba(128u, 128u) { _, _ -> Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()) }

        source.setRgbPrimaries(Cicp.SRGB.primaries)
        source.setTransferFunction(Cicp.SRGB.transfer)

        target.setRgbPrimaries(Cicp.DISPLAY_P3.primaries)
        target.setTransferFunction(Cicp.DISPLAY_P3.transfer)

        target.copyFromColorSpace(source, ConvertColorOptions())

        assertEquals(Rgba(234u.toUByte(), 51u.toUByte(), 35u.toUByte(), 255u.toUByte()), target.getPixel(0u, 0u))
    }

    @Test
    fun grayConversions() {
        val source = ImageBuffer.createGray(128u, 128u) { _, _ -> Luma(255u.toUByte()) }
        val target = ImageBuffer.createRgba(128u, 128u) { _, _ -> Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()) }

        source.setRgbPrimaries(Cicp.SRGB.primaries)
        source.setTransferFunction(Cicp.SRGB.transfer)

        target.setRgbPrimaries(Cicp.SRGB.primaries)
        target.setTransferFunction(Cicp.SRGB.transfer)

        target.copyFromColorSpace(source, ConvertColorOptions())

        assertEquals(Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 255u.toUByte()), target.getPixel(0u, 0u))
    }

    @Test
    fun rgbToGrayConversion() {
        val source = ImageBuffer.createRgb(128u, 128u) { _, _ -> Rgb(128u.toUByte(), 128u.toUByte(), 128u.toUByte()) }
        val target = ImageBuffer.createGray(128u, 128u) { _, _ -> Luma(0u.toUByte()) }

        source.setRgbPrimaries(Cicp.SRGB.primaries)
        source.setTransferFunction(Cicp.SRGB.transfer)

        target.setRgbPrimaries(Cicp.SRGB.primaries)
        target.setTransferFunction(Cicp.SRGB.transfer)

        target.copyFromColorSpace(source, ConvertColorOptions())

        assertEquals(Luma(128u.toUByte()), target.getPixel(0u, 0u))
    }

    @Test
    fun applyColor() {
        val buffer = ImageBuffer.createRgb(128u, 128u) { _, _ -> Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()) }

        buffer.setRgbPrimaries(Cicp.SRGB.primaries)
        buffer.setTransferFunction(Cicp.SRGB.transfer)

        buffer.applyColorSpace(Cicp.DISPLAY_P3, ConvertColorOptions())

        for ((_, _, p) in buffer.pixels()) {
            assertEquals(Rgb(234u.toUByte(), 51u.toUByte(), 35u.toUByte()), p)
        }
    }

    @Test
    fun toColor() {
        val source = ImageBuffer.createRgb(128u, 128u) { _, _ -> Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()) }
        source.setRgbPrimaries(Cicp.SRGB.primaries)
        source.setTransferFunction(Cicp.SRGB.transfer)

        val target = source.toColorSpace(Cicp.DISPLAY_P3, ConvertColorOptions())

        assertEquals(Rgb(234u.toUByte(), 51u.toUByte(), 35u.toUByte()), target.getPixel(0u, 0u))
    }

    @Test
    fun transformationMismatch() {
        val source = ImageBuffer.createGray(128u, 128u) { _, _ -> Luma(255u.toUByte()) }
        val target = ImageBuffer.createRgba(128u, 128u) { _, _ -> Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()) }

        source.setColorSpace(Cicp.SRGB)
        target.setColorSpace(Cicp.DISPLAY_P3)

        val options = ConvertColorOptions(transform = CicpTransform.new(Cicp.SRGB, Cicp.SRGB))

        assertFailsWith<ImageError.Parameter> {
            target.copyFromColorSpace(source, options)
        }
    }
}
