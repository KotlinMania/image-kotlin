// port-lint: source codecs/qoi.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QoiTest {
    @Test
    fun testEncodeDecodeRoundtripRgba() {
        val width = 4u
        val height = 4u
        val originalData = ByteArray(16 * 4) { ((it * 17) % 256).toByte() }

        val sink = BufferIoWrite()
        val encoder = QoiEncoder(sink)
        encoder.encode(originalData, width, height, 4)

        val encodedBytes = sink.toByteArray()
        assertTrue(encodedBytes.size > 14 + 8)

        val decoder = QoiDecoder(encodedBytes)
        assertEquals(Pair(width, height), decoder.dimensions())
        assertEquals(ColorType.Rgba8, decoder.colorType())

        val decodedData = ByteArray(16 * 4)
        decoder.readImage(decodedData)
        assertTrue(originalData.contentEquals(decodedData))
    }

    @Test
    fun testEncodeDecodeRoundtripRgb() {
        val width = 3u
        val height = 3u
        val originalData = ByteArray(9 * 3) { ((it * 23) % 256).toByte() }

        val sink = BufferIoWrite()
        val encoder = QoiEncoder(sink)
        encoder.encode(originalData, width, height, 3)

        val encodedBytes = sink.toByteArray()
        val decoder = QoiDecoder(encodedBytes)
        assertEquals(Pair(width, height), decoder.dimensions())
        assertEquals(ColorType.Rgb8, decoder.colorType())

        val decodedData = ByteArray(9 * 3)
        decoder.readImage(decodedData)
        assertTrue(originalData.contentEquals(decodedData))
    }
}
