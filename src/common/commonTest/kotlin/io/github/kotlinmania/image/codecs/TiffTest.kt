// port-lint: tests codecs/tiff.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TiffTest {
    @Test
    fun testEncodeAndDecodeTiff() {
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
        val encoder = TiffEncoder(writer)
        encoder.encode(data, width, height, ExtendedColorType.Rgba8)

        val encodedBytes = writer.toByteArray()
        assertTrue(encodedBytes.isNotEmpty())

        val decoder = TiffDecoder(encodedBytes)
        val outBuf = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(outBuf)
        assertEquals(4, outBuf.size)
    }
}
