// port-lint: tests codecs/farbfeld.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FarbfeldTest {
    @Test
    fun testDimensionOverflow() {
        val header =
            byteArrayOf(
                0x66,
                0x61,
                0x72,
                0x62,
                0x66,
                0x65,
                0x6c,
                0x64, // farbfeld
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
            )
        assertFailsWith<ImageError> {
            FarbfeldDecoder(header)
        }
    }

    @Test
    fun testEncodeDecodeRoundtrip() {
        val width = 2u
        val height = 2u
        // 2x2 RGBA16 -> 4 pixels * 8 bytes/pixel = 32 bytes
        val originalData = ByteArray(32) { (it * 7).toByte() }

        val sink = BufferIoWrite()
        val encoder = FarbfeldEncoder(sink)
        encoder.encode(originalData, width, height)

        val encodedBytes = sink.toByteArray()
        assertEquals(8 + 8 + 32, encodedBytes.size)

        val decoder = FarbfeldDecoder(encodedBytes)
        assertEquals(Pair(width, height), decoder.dimensions())
        assertEquals(ColorType.Rgba16, decoder.colorType())
        assertEquals(32uL, decoder.totalBytes())

        val decodedData = ByteArray(32)
        decoder.readImage(decodedData)
        assertTrue(originalData.contentEquals(decodedData))
    }

    @Test
    fun testInvalidMagicThrows() {
        val badHeader = ByteArray(16) { 0 }
        assertFailsWith<ImageError> {
            FarbfeldDecoder(badHeader)
        }
    }
}
