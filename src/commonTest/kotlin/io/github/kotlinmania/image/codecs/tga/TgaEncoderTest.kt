// port-lint: tests image/src/codecs/tga/encoder.rs
package io.github.kotlinmania.image.codecs.tga

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TgaEncoderTest {
    @Test
    fun testImageWidthTooLarge() {
        val size = 65536
        val dimension = size.toUInt()
        val img = ByteArray(size)

        val writeBuf = BufferIoWrite()
        val encoder = TgaEncoder(writeBuf)
        val ex =
            assertFailsWith<ImageError.Encoding> {
                encoder.encode(img, dimension, 1u, ExtendedColorType.L8)
            }
        val cause = ex.error.underlying
        assertTrue(cause is EncoderError.WidthInvalid)
        assertEquals(dimension, cause.width)
    }

    @Test
    fun testImageHeightTooLarge() {
        val size = 65536
        val dimension = size.toUInt()
        val img = ByteArray(size)

        val writeBuf = BufferIoWrite()
        val encoder = TgaEncoder(writeBuf)
        val ex =
            assertFailsWith<ImageError.Encoding> {
                encoder.encode(img, 1u, dimension, ExtendedColorType.L8)
            }
        val cause = ex.error.underlying
        assertTrue(cause is EncoderError.HeightInvalid)
        assertEquals(dimension, cause.height)
    }

    @Test
    fun testCompressionDiff() {
        val image = byteArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2)

        val uncompressedBytes =
            run {
                val writeBuf = BufferIoWrite()
                val encoder = TgaEncoder(writeBuf).disableRle()
                encoder.encode(image, 5u, 1u, ExtendedColorType.Rgb8)
                writeBuf.toByteArray()
            }

        val compressedBytes =
            run {
                val writeBuf = BufferIoWrite()
                val encoder = TgaEncoder(writeBuf)
                encoder.encode(image, 5u, 1u, ExtendedColorType.Rgb8)
                writeBuf.toByteArray()
            }

        assertTrue(uncompressedBytes.size > compressedBytes.size)
    }

    private fun roundTripImageCompressed(
        image: ByteArray,
        width: UInt,
        height: UInt,
        c: ExtendedColorType,
    ): ByteArray {
        val writeBuf = BufferIoWrite()
        val encoder = TgaEncoder(writeBuf)
        encoder.encode(image, width, height, c)

        val encodedData = writeBuf.toByteArray()
        val decoder = TgaDecoder(BufferIoRead(encodedData))
        val buf = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(buf)
        return buf
    }

    private fun roundTripImageUncompressed(
        image: ByteArray,
        width: UInt,
        height: UInt,
        c: ExtendedColorType,
    ): ByteArray {
        val writeBuf = BufferIoWrite()
        val encoder = TgaEncoder(writeBuf).disableRle()
        encoder.encode(image, width, height, c)

        val encodedData = writeBuf.toByteArray()
        val decoder = TgaDecoder(BufferIoRead(encodedData))
        val buf = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(buf)
        return buf
    }

    @Test
    fun mixedPackets() {
        val image =
            byteArrayOf(
                255.toByte(),
                255.toByte(),
                255.toByte(),
                0,
                0,
                0,
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
            )
        val decoded = roundTripImageCompressed(image, 5u, 1u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun roundTripGray() {
        val image = byteArrayOf(0, 1, 2)
        val decoded = roundTripImageCompressed(image, 3u, 1u, ExtendedColorType.L8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun roundTripGraya() {
        val image = byteArrayOf(0, 1, 2, 3, 4, 5)
        val decoded = roundTripImageCompressed(image, 1u, 3u, ExtendedColorType.La8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun roundTripSinglePixelRgb() {
        val image = byteArrayOf(0, 1, 2)
        val decoded = roundTripImageCompressed(image, 1u, 1u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun roundTripThreePixelRgb() {
        val image = byteArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2)
        val decoded = roundTripImageCompressed(image, 3u, 1u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun roundTrip3pxRgb() {
        val image = ByteArray(3 * 3 * 3)
        val decoded = roundTripImageCompressed(image, 3u, 3u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun roundTripDifferent() {
        val image = byteArrayOf(0, 1, 2, 0, 1, 3, 0, 1, 4)
        val decoded = roundTripImageCompressed(image, 3u, 1u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun roundTripDifferent2() {
        val image = byteArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 4)
        val decoded = roundTripImageCompressed(image, 4u, 1u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun roundTripDifferent3() {
        val image = byteArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 4, 0, 1, 2)
        val decoded = roundTripImageCompressed(image, 5u, 1u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun uncompressedRoundTripSinglePixelRgb() {
        val image = byteArrayOf(0, 1, 2)
        val decoded = roundTripImageUncompressed(image, 1u, 1u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun uncompressedRoundTripSinglePixelRgba() {
        val image = byteArrayOf(0, 1, 2, 3)
        val decoded = roundTripImageUncompressed(image, 1u, 1u, ExtendedColorType.Rgba8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun uncompressedRoundTripGray() {
        val image = byteArrayOf(0, 1, 2)
        val decoded = roundTripImageUncompressed(image, 3u, 1u, ExtendedColorType.L8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun uncompressedRoundTripGraya() {
        val image = byteArrayOf(0, 1, 2, 3, 4, 5)
        val decoded = roundTripImageUncompressed(image, 1u, 3u, ExtendedColorType.La8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }

    @Test
    fun uncompressedRoundTrip3pxRgb() {
        val image = ByteArray(3 * 3 * 3)
        val decoded = roundTripImageUncompressed(image, 3u, 3u, ExtendedColorType.Rgb8)
        assertEquals(image.size, decoded.size)
        assertContentEquals(image, decoded)
    }
}
