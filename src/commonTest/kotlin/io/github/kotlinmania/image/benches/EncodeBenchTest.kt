// port-lint: tests image/benches/encode.rs
package io.github.kotlinmania.image.benches

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.codecs.bmp.BmpEncoder
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertTrue

class EncodeBenchTest {
    private interface Encoder {
        fun encodeRaw(into: BufferIoWrite, im: ByteArray, dims: UInt, color: ExtendedColorType)
    }

    private class Bmp : Encoder {
        override fun encodeRaw(into: BufferIoWrite, im: ByteArray, dims: UInt, color: ExtendedColorType) {
            val enc = BmpEncoder(into)
            enc.encode(im, dims, dims, color)
        }
    }

    private data class BenchDef(
        val encoder: Encoder,
        val name: String,
        val sizes: List<UInt>,
        val colors: List<ColorType>,
    )

    @Test
    fun benchEncodeAll() {
        val benchDefs = listOf(
            BenchDef(
                encoder = Bmp(),
                name = "bmp",
                sizes = listOf(64u, 128u),
                colors = listOf(ColorType.L8, ColorType.Rgb8, ColorType.Rgba8),
            ),
        )

        for (def in benchDefs) {
            for (color in def.colors) {
                val extColor = ExtendedColorType.from(color)
                for (size in def.sizes) {
                    val bytesPerPixel = (color.bitsPerPixel().toInt() + 7) / 8
                    val im = ByteArray(bytesPerPixel * size.toInt() * size.toInt())
                    val writeBuf = BufferIoWrite()
                    def.encoder.encodeRaw(writeBuf, im, size, extColor)
                    assertTrue(writeBuf.toByteArray().isNotEmpty())
                }
            }
        }
    }
}
