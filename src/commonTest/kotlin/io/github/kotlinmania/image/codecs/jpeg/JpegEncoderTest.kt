// port-lint: tests codecs/jpeg/encoder.rs
package io.github.kotlinmania.image.codecs.jpeg

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JpegEncoderTest {
    @Test
    fun roundtripSanityCheck() {
        val img = byteArrayOf(255.toByte(), 0, 0)
        val out = BufferIoWrite()
        val encoder = JpegEncoder(out, 100u)
        encoder.writeImage(img, 1u, 1u, ExtendedColorType.Rgb8)

        val encoded = out.toByteArray()
        assertTrue(encoded.isNotEmpty())
        // Verify SOI and EOI markers
        assertEquals(0xFF.toByte(), encoded[0])
        assertEquals(0xD8.toByte(), encoded[1])
        assertEquals(0xFF.toByte(), encoded[encoded.size - 2])
        assertEquals(0xD9.toByte(), encoded[encoded.size - 1])
    }

    @Test
    fun grayscaleRoundtripSanityCheck() {
        val img = byteArrayOf(255.toByte(), 0, 0, 255.toByte())
        val out = BufferIoWrite()
        val encoder = JpegEncoder(out, 100u)
        encoder.writeImage(img, 2u, 2u, ExtendedColorType.L8)

        val encoded = out.toByteArray()
        assertTrue(encoded.isNotEmpty())
        assertEquals(0xFF.toByte(), encoded[0])
        assertEquals(0xD8.toByte(), encoded[1])
        assertEquals(0xFF.toByte(), encoded[encoded.size - 2])
        assertEquals(0xD9.toByte(), encoded[encoded.size - 1])
    }

    @Test
    fun jfifHeaderDensityCheck() {
        val density = PixelDensity.dpi(300u)
        val expected =
            byteArrayOf(
                'J'.code.toByte(),
                'F'.code.toByte(),
                'I'.code.toByte(),
                'F'.code.toByte(),
                0,
                1,
                2,
                1,
                (300 shr 8).toByte(),
                (300 and 0xFF).toByte(),
                (300 shr 8).toByte(),
                (300 and 0xFF).toByte(),
                0,
                0,
            )
        val encoder = JpegEncoder(BufferIoWrite())
        encoder.setPixelDensity(density)
    }

    @Test
    fun testImageTooLarge() {
        val encoder = JpegEncoder(BufferIoWrite(), 100u)
        assertFailsWith<ImageError.Encoding> {
            encoder.writeImage(ByteArray(65536), 65536u, 1u, ExtendedColorType.L8)
        }
    }
}
