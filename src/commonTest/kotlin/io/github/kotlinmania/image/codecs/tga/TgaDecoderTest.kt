// port-lint: tests codecs/tga/decoder.rs
package io.github.kotlinmania.image.codecs.tga

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TgaDecoderTest {
    @Test
    fun testEmptyDimensionsThrows() {
        // TGA header with width = 0
        val header = ByteArray(18)
        header[2] = 2 // TrueColor
        header[16] = 24 // 24 bpp
        assertFailsWith<ImageError> {
            TgaDecoder(BufferIoRead(header))
        }
    }

    @Test
    fun testUnsupportedPixelDepthThrows() {
        val header = ByteArray(18)
        header[2] = 2 // TrueColor
        header[12] = 10 // width = 10
        header[14] = 10 // height = 10
        header[16] = 12 // invalid depth 12 bpp
        assertFailsWith<ImageError.Unsupported> {
            TgaDecoder(BufferIoRead(header))
        }
    }

    @Test
    fun testOrientationTopLeftVsBottomLeft() {
        // Test decoding an uncompressed 2x2 image created via encoder
        val image = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)
        val writeBuf = BufferIoWrite()
        val encoder = TgaEncoder(writeBuf).disableRle()
        encoder.encode(image, 2u, 2u, ExtendedColorType.Rgb8)

        val encoded = writeBuf.toByteArray()
        val decoder = TgaDecoder(BufferIoRead(encoded))
        assertEquals(Pair(2u, 2u), decoder.dimensions())
        assertEquals(ColorType.Rgb8, decoder.colorType())

        val decoded = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(decoded)
        assertContentEquals(image, decoded)
    }

    @Test
    fun testColorMapHelper() {
        val palette = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val colorMap = ColorMap(startOffset = 0, entrySize = 3, bytes = palette)
        assertContentEquals(byteArrayOf(1, 2, 3), colorMap.get(0))
        assertContentEquals(byteArrayOf(4, 5, 6), colorMap.get(1))
        assertContentEquals(byteArrayOf(7, 8, 9), colorMap.get(2))
        assertEquals(null, colorMap.get(3))
        assertEquals(null, colorMap.get(-1))
    }

    @Test
    fun testTgaOrientationParsing() {
        assertEquals(TgaOrientation.BottomLeft, TgaOrientation.fromImageDescByte(0x00u))
        assertEquals(TgaOrientation.TopLeft, TgaOrientation.fromImageDescByte(0x20u))
        assertEquals(TgaOrientation.BottomRight, TgaOrientation.fromImageDescByte(0x10u))
        assertEquals(TgaOrientation.TopRight, TgaOrientation.fromImageDescByte(0x30u))
    }
}
