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
}

