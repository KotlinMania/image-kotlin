// port-lint: tests codecs/bmp/encoder.rs
package io.github.kotlinmania.image.codecs.bmp

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BmpTest {
    private fun roundTripImage(
        image: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ): ByteArray {
        val writeBuf = BufferIoWrite()
        val encoder = BmpEncoder(writeBuf)
        encoder.encode(image, width, height, colorType)

        val encoded = writeBuf.toByteArray()
        val readBuf = BufferIoRead(encoded)
        val decoder = BmpDecoder(readBuf)

        val totalBytes = decoder.totalBytes().toInt()
        val decoded = ByteArray(totalBytes)
        decoder.readImage(decoded)
        return decoded
    }

    @Test
    fun roundTripSinglePixelRgb() {
        val image = byteArrayOf(255.toByte(), 0, 0)
        val decoded = roundTripImage(image, 1u, 1u, ExtendedColorType.Rgb8)
        assertEquals(3, decoded.size)
        assertEquals(255.toByte(), decoded[0])
        assertEquals(0, decoded[1])
        assertEquals(0, decoded[2])
    }

    @Test
    fun roundTripSinglePixelRgba() {
        val image = byteArrayOf(1, 2, 3, 4)
        val decoded = roundTripImage(image, 1u, 1u, ExtendedColorType.Rgba8)
        assertEquals(4, decoded.size)
        assertEquals(1, decoded[0])
        assertEquals(2, decoded[1])
        assertEquals(3, decoded[2])
        assertEquals(4, decoded[3])
    }

    @Test
    fun roundTrip3pxRgb() {
        val image = ByteArray(3 * 3 * 3)
        val decoded = roundTripImage(image, 3u, 3u, ExtendedColorType.Rgb8)
        assertEquals(27, decoded.size)
    }

    @Test
    fun roundTripGray() {
        val image = byteArrayOf(0, 1, 2)
        val decoded = roundTripImage(image, 3u, 1u, ExtendedColorType.L8)
        // should be read back as 3 RGB pixels
        assertEquals(9, decoded.size)
        assertEquals(0, decoded[0])
        assertEquals(0, decoded[1])
        assertEquals(0, decoded[2])
        assertEquals(1, decoded[3])
        assertEquals(1, decoded[4])
        assertEquals(1, decoded[5])
        assertEquals(2, decoded[6])
        assertEquals(2, decoded[7])
        assertEquals(2, decoded[8])
    }

    @Test
    fun roundTripGraya() {
        val image = byteArrayOf(0, 0, 1, 0, 2, 0)
        val decoded = roundTripImage(image, 1u, 3u, ExtendedColorType.La8)
        // should be read back as 3 RGB pixels
        assertEquals(9, decoded.size)
        assertEquals(0, decoded[0])
        assertEquals(0, decoded[1])
        assertEquals(0, decoded[2])
        assertEquals(1, decoded[3])
        assertEquals(1, decoded[4])
        assertEquals(1, decoded[5])
        assertEquals(2, decoded[6])
        assertEquals(2, decoded[7])
        assertEquals(2, decoded[8])
    }

    @Test
    fun hugeFilesReturnError() {
        val encoder = BmpEncoder(BufferIoWrite())
        assertFailsWith<Throwable> {
            encoder.encode(ByteArray(0), 40_000u, 40_000u, ExtendedColorType.Rgb8)
        }
    }

    @Test
    fun regressionIssue2604() {
        val image = ByteArray(0)
        val encoder = BmpEncoder(BufferIoWrite())
        assertFailsWith<ImageError.Parameter> {
            encoder.encode(image, 1u shl 31, 0u, ExtendedColorType.Rgb8)
        }
    }

    @Test
    fun testBitfieldLen() {
        for (len in 1..8) {
            val bitfield = Bitfield(shift = 0, len = len)
            for (i in 0 until (1 shl len)) {
                val read = bitfield.read(i.toUInt())
                val calc = kotlin.math.round((i.toDouble() / ((1 shl len) - 1).toDouble()) * 255.0).toInt()
                assertEquals(calc, read, "len: $len i: $i")
            }
        }
    }

    @Test
    fun readRleTooShort() {
        val data = byteArrayOf(
            0x42.toByte(), 0x4d.toByte(), 0x04.toByte(), 0xee.toByte(), 0xfe.toByte(), 0xff.toByte(), 0xff.toByte(), 0x10.toByte(), 0xff.toByte(), 0x00.toByte(), 0x04.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x7c.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0c.toByte(), 0x41.toByte(), 0x00.toByte(), 0x00.toByte(), 0x07.toByte(), 0x10.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(),
            0x04.toByte(), 0x00.toByte(), 0x02.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0d.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xfe.toByte(), 0x21.toByte(),
            0xff.toByte(), 0x00.toByte(), 0x66.toByte(), 0x61.toByte(), 0x72.toByte(), 0x62.toByte(), 0x66.toByte(), 0x65.toByte(), 0x6c.toByte(), 0x64.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xff.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x00.toByte(), 0x00.toByte(), 0x19.toByte(), 0x51.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x10.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xfa.toByte(), 0xff.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x11.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0f.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x2d.toByte(), 0x31.toByte(), 0x31.toByte(), 0x35.toByte(), 0x36.toByte(), 0x00.toByte(), 0xff.toByte(), 0x00.toByte(), 0x00.toByte(), 0x52.toByte(), 0x3a.toByte(),
            0x37.toByte(), 0x30.toByte(), 0x7e.toByte(), 0x71.toByte(), 0x63.toByte(), 0x91.toByte(), 0x5a.toByte(), 0x04.toByte(), 0x00.toByte(), 0x10.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x04.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x2d.toByte(), 0x35.toByte(), 0x37.toByte(), 0x00.toByte(), 0xff.toByte(), 0x00.toByte(), 0x00.toByte(), 0x52.toByte(),
            0x3a.toByte(), 0x37.toByte(), 0x30.toByte(), 0x7e.toByte(), 0x71.toByte(), 0x63.toByte(), 0x91.toByte(), 0x5a.toByte(), 0x04.toByte(), 0x05.toByte(), 0x3c.toByte(), 0x00.toByte(), 0x00.toByte(), 0x11.toByte(),
            0x00.toByte(), 0x5d.toByte(), 0x7a.toByte(), 0x82.toByte(), 0xb7.toByte(), 0xca.toByte(), 0x2d.toByte(), 0x31.toByte(), 0xff.toByte(), 0xff.toByte(), 0xc7.toByte(), 0x95.toByte(), 0x33.toByte(), 0x2e.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x7c.toByte(), 0x00.toByte(),
            0x20.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x20.toByte(), 0x66.toByte(), 0x00.toByte(), 0x4d.toByte(),
            0x4d.toByte(), 0x00.toByte(), 0x2a.toByte(), 0x00.toByte(),
        )
        val decoder = BmpDecoder.new(BufferIoRead(data))
        val buf = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(buf)
    }

    @Test
    fun testNoHeader() {
        val writeBuf = BufferIoWrite()
        val encoder = BmpEncoder(writeBuf)
        val image = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)
        encoder.encode(image, 2u, 2u, ExtendedColorType.Rgb8)
        val encoded = writeBuf.toByteArray()
        val withoutHeader = encoded.copyOfRange(14, encoded.size)
        val decoder = BmpDecoder.newWithoutFileHeader(BufferIoRead(withoutHeader))
        val decoded = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(decoded)
        assertEquals(image.size, decoded.size)
        for (i in image.indices) {
            assertEquals(image[i], decoded[i])
        }
    }

    @Test
    fun testReadRect() {
        val writeBuf = BufferIoWrite()
        val encoder = BmpEncoder(writeBuf)
        val image = ByteArray(4 * 4 * 3) { it.toByte() }
        encoder.encode(image, 4u, 4u, ExtendedColorType.Rgb8)
        val encoded = writeBuf.toByteArray()
        val decoder = BmpDecoder.new(BufferIoRead(encoded))
        val rectBuf = ByteArray(2 * 2 * 3)
        decoder.readRect(1u, 1u, 2u, 2u, rectBuf, 2 * 3)
        assertEquals(12, rectBuf.size)
    }
}
