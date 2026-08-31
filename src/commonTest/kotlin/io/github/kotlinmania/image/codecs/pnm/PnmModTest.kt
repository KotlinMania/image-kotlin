// port-lint: tests image/src/codecs/pnm/mod.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PnmModTest {
    internal fun executeRoundtripDefault(buffer: ByteArray, width: UInt, height: UInt, color: ExtendedColorType) {
        val writer = BufferIoWrite()
        val encoder = PnmEncoder(writer)
        encoder.encode(buffer, width, height, color)
        val encodedBuffer = writer.toByteArray()

        val decoder = PnmDecoder(encodedBuffer)
        val loadedColor = decoder.colorType()
        val totalBytes = (decoder.dimensions().first * decoder.dimensions().second * loadedColor.bytesPerPixel().toUInt()).toInt()
        val loadedImage = ByteArray(totalBytes)
        decoder.readImage(loadedImage)
        val header = decoder.header()

        assertEquals(width, header.width())
        assertEquals(height, header.height())
        assertEquals(color, loadedColor.toExtendedColorType())
        assertTrue(buffer.contentEquals(loadedImage))
    }

    internal fun executeRoundtripWithSubtype(
        buffer: ByteArray,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
        subtype: PnmSubtype,
    ) {
        val writer = BufferIoWrite()
        val encoder = PnmEncoder(writer).withSubtype(subtype)
        encoder.encode(buffer, width, height, color)
        val encodedBuffer = writer.toByteArray()

        val decoder = PnmDecoder(encodedBuffer)
        val loadedColor = decoder.colorType()
        val totalBytes = (decoder.dimensions().first * decoder.dimensions().second * loadedColor.bytesPerPixel().toUInt()).toInt()
        val loadedImage = ByteArray(totalBytes)
        decoder.readImage(loadedImage)
        val header = decoder.header()

        assertEquals(width, header.width())
        assertEquals(height, header.height())
        assertEquals(subtype, header.subtype())
        assertEquals(color, loadedColor.toExtendedColorType())
        assertTrue(buffer.contentEquals(loadedImage))
    }

    internal fun executeRoundtripU16(buffer: ShortArray, width: UInt, height: UInt, color: ExtendedColorType) {
        val writer = BufferIoWrite()
        val encoder = PnmEncoder(writer)
        encoder.encode(buffer, width, height, color)
        val encodedBuffer = writer.toByteArray()

        val decoder = PnmDecoder(encodedBuffer)
        val loadedColor = decoder.colorType()
        val totalBytes = (decoder.dimensions().first * decoder.dimensions().second * loadedColor.bytesPerPixel().toUInt()).toInt()
        val loadedImage = ByteArray(totalBytes)
        decoder.readImage(loadedImage)
        val header = decoder.header()

        val bufferU8 = ByteArray(buffer.size * 2)
        for (i in buffer.indices) {
            val v = buffer[i].toInt() and 0xFFFF
            bufferU8[i * 2] = (v and 0xFF).toByte()
            bufferU8[i * 2 + 1] = (v ushr 8).toByte()
        }

        assertEquals(width, header.width())
        assertEquals(height, header.height())
        assertEquals(color, loadedColor.toExtendedColorType())
        assertTrue(bufferU8.contentEquals(loadedImage))
    }

    @Test
    fun roundtripGray() {
        val buf =
            byteArrayOf(
                0,
                0,
                0,
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                0,
                255.toByte(),
                0,
                255.toByte(),
                0,
                0,
                0,
            )

        executeRoundtripDefault(buf, 4u, 4u, ExtendedColorType.L8)
        executeRoundtripWithSubtype(buf, 4u, 4u, ExtendedColorType.L8, PnmSubtype.ArbitraryMap)
        executeRoundtripWithSubtype(
            buf,
            4u,
            4u,
            ExtendedColorType.L8,
            PnmSubtype.Graymap(SampleEncoding.Ascii),
        )
        executeRoundtripWithSubtype(
            buf,
            4u,
            4u,
            ExtendedColorType.L8,
            PnmSubtype.Graymap(SampleEncoding.Binary),
        )
    }

    @Test
    fun roundtripRgb() {
        val buf =
            byteArrayOf(
                0,
                0,
                0,
                0,
                0,
                255.toByte(),
                0,
                255.toByte(),
                0,
                0,
                255.toByte(),
                255.toByte(),
                255.toByte(),
                0,
                0,
                255.toByte(),
                0,
                255.toByte(),
                255.toByte(),
                255.toByte(),
                0,
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
            )
        executeRoundtripDefault(buf, 3u, 3u, ExtendedColorType.Rgb8)
        executeRoundtripWithSubtype(
            buf,
            3u,
            3u,
            ExtendedColorType.Rgb8,
            PnmSubtype.ArbitraryMap,
        )
        executeRoundtripWithSubtype(
            buf,
            3u,
            3u,
            ExtendedColorType.Rgb8,
            PnmSubtype.Pixmap(SampleEncoding.Binary),
        )
        executeRoundtripWithSubtype(
            buf,
            3u,
            3u,
            ExtendedColorType.Rgb8,
            PnmSubtype.Pixmap(SampleEncoding.Ascii),
        )
    }

    @Test
    fun roundtripU16() {
        val buf = shortArrayOf(0, 1, 0xFFFF.toShort(), 0x1234.toShort(), 0x3412.toShort(), 0xBEAF.toInt().toShort())
        executeRoundtripU16(buf, 6u, 1u, ExtendedColorType.L16)
    }
}
