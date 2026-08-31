// port-lint: tests image/src/codecs/openexr.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenexrTest {
    @Test
    fun testOpenexrEncodeAndDecode() {
        val width = 1u
        val height = 1u
        val data = ByteArray(16) // 4 float components = 16 bytes
        val writer = BufferIoWrite()
        val encoder = OpenExrEncoder(writer)
        encoder.encode(data, width, height, ExtendedColorType.Rgba32F)

        val bytes = writer.toByteArray()
        assertTrue(bytes.isNotEmpty())

        val decoder = OpenExrDecoder(bytes)
        assertEquals(Pair(width, height), decoder.dimensions())
    }
}
