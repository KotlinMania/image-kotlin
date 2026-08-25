// port-lint: tests codecs/gif.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GifTest {
    @Test
    fun testEncodeAndDecodeRoundtrip() {
        val width = 2u
        val height = 2u
        val data = byteArrayOf(
            255.toByte(), 0, 0, 255.toByte(),
            0, 255.toByte(), 0, 255.toByte(),
            0, 0, 255.toByte(), 255.toByte(),
            255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(),
        )
        val writer = BufferIoWrite()
        val encoder = GifEncoder(writer)
        encoder.encode(data, width, height, ExtendedColorType.Rgba8)

        val encodedBytes = writer.toByteArray()
        assertTrue(encodedBytes.isNotEmpty())

        val decoder = GifDecoder(encodedBytes)
        assertEquals(Pair(width, height), decoder.dimensions())
        val outBuf = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(outBuf)
        assertEquals(16, outBuf.size)
    }

    @Test
    fun testRepeatSettings() {
        val writer = BufferIoWrite()
        val encoder = GifEncoder(writer)
        encoder.setRepeat(Repeat.Infinite)
        val data = byteArrayOf(0, 0, 0, 255.toByte())
        encoder.encode(data, 1u, 1u, ExtendedColorType.Rgba8)
        val bytes = writer.toByteArray()
        assertTrue(bytes.isNotEmpty())
    }
}
