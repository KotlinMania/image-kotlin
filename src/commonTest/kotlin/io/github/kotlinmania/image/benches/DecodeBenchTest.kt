// port-lint: tests decode.rs
package io.github.kotlinmania.image.benches

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.codecs.bmp.BmpDecoder
import io.github.kotlinmania.image.codecs.bmp.BmpEncoder
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class DecodeBenchTest {
    private data class BenchDef(
        val name: String,
        val format: ImageFormat,
        val width: UInt,
        val height: UInt,
    )

    @Test
    fun benchDecodeBmp() {
        val defs = listOf(
            BenchDef("100x100", ImageFormat.Bmp, 100u, 100u),
            BenchDef("200x200", ImageFormat.Bmp, 200u, 200u),
        )

        for (def in defs) {
            val raw = ByteArray((def.width * def.height * 3u).toInt()) { (it % 256).toByte() }
            val writeBuf = BufferIoWrite()
            val encoder = BmpEncoder(writeBuf)
            encoder.encode(raw, def.width, def.height, ExtendedColorType.Rgb8)

            val encoded = writeBuf.toByteArray()
            val readBuf = BufferIoRead(encoded)
            val decoder = BmpDecoder(readBuf)

            assertEquals(Pair(def.width, def.height), decoder.dimensions())
            assertEquals(ColorType.Rgb8, decoder.colorType())
            val decoded = ByteArray((def.width * def.height * 3u).toInt())
            decoder.readImage(decoded)
            assertEquals(raw.size, decoded.size)
        }
    }
}
