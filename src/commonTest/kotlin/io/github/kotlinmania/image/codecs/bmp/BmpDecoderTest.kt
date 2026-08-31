// port-lint: tests image/src/codecs/bmp/decoder.rs
package io.github.kotlinmania.image.codecs.bmp

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals

class BmpDecoderTest {
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
    fun readRect() {
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
}
