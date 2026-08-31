// port-lint: tests image/src/codecs/avif/mod.rs
package io.github.kotlinmania.image.codecs.avif

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvifTest {
    @Test
    fun testAvifEncodeAndDecode() {
        val width = 2u
        val height = 2u
        val data =
            byteArrayOf(
                255.toByte(),
                0,
                0,
                255.toByte(),
                0,
                255.toByte(),
                0,
                255.toByte(),
                0,
                0,
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
                255.toByte(),
            )
        val writer = BufferIoWrite()
        val encoder =
            AvifEncoder(writer)
                .withColorspace(ColorSpace.Srgb)
        encoder.encode(data, width, height, ExtendedColorType.Rgba8)

        val bytes = writer.toByteArray()
        assertTrue(bytes.isNotEmpty())

        val decoder = AvifDecoder(bytes)
        assertEquals(Pair(1u, 1u), decoder.dimensions())
    }
}
