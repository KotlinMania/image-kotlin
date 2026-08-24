// port-lint: tests codecs/dxt.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DxtTest {
    @Test
    fun testEnc565Decode() {
        val black = enc565Decode(0u)
        assertContentEquals(byteArrayOf(0, 0, 0), black)

        val white = enc565Decode(0xFFFFu)
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), white)

        val red = enc565Decode((0x1F shl 11).toUShort())
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0, 0), red)

        val green = enc565Decode((0x3F shl 5).toUShort())
        assertContentEquals(byteArrayOf(0, 0xFF.toByte(), 0), green)

        val blue = enc565Decode(0x1Fu.toUShort())
        assertContentEquals(byteArrayOf(0, 0, 0xFF.toByte()), blue)
    }

    @Test
    fun testAlphaTableDxt5() {
        val table1 = alphaTableDxt5(255u, 0u)
        assertEquals(8, table1.size)
        assertEquals(255.toByte(), table1[0])
        assertEquals(0.toByte(), table1[1])

        val table2 = alphaTableDxt5(0u, 255u)
        assertEquals(8, table2.size)
        assertEquals(0.toByte(), table2[0])
        assertEquals(255.toByte(), table2[1])
        assertEquals(0.toByte(), table2[6])
        assertEquals(0xFF.toByte(), table2[7])
    }

    @Test
    fun testVariantProperties() {
        assertEquals(48L, DxtVariant.DXT1.decodedBytesPerBlock())
        assertEquals(64L, DxtVariant.DXT3.decodedBytesPerBlock())
        assertEquals(64L, DxtVariant.DXT5.decodedBytesPerBlock())

        assertEquals(8L, DxtVariant.DXT1.encodedBytesPerBlock())
        assertEquals(16L, DxtVariant.DXT3.encodedBytesPerBlock())
        assertEquals(16L, DxtVariant.DXT5.encodedBytesPerBlock())

        assertEquals(ColorType.Rgb8, DxtVariant.DXT1.colorType())
        assertEquals(ColorType.Rgba8, DxtVariant.DXT3.colorType())
        assertEquals(ColorType.Rgba8, DxtVariant.DXT5.colorType())
    }

    @Test
    fun testDimensionMismatch() {
        assertFailsWith<ImageError.Parameter> {
            DxtDecoder(ByteArray(8), 3u, 4u, DxtVariant.DXT1)
        }
        assertFailsWith<ImageError.Parameter> {
            DxtDecoder(ByteArray(8), 4u, 5u, DxtVariant.DXT1)
        }
    }

    @Test
    fun testDecodeDxt1Block() {
        // 4x4 block of solid red (color0 = red, color1 = red, indices = all 0)
        val red565 = (0x1F shl 11).toUShort()
        val data = ByteArray(8)
        data[0] = (red565.toInt() and 0xFF).toByte()
        data[1] = ((red565.toInt() ushr 8) and 0xFF).toByte()
        data[2] = (red565.toInt() and 0xFF).toByte()
        data[3] = ((red565.toInt() ushr 8) and 0xFF).toByte()

        val decoder = DxtDecoder(data, 4u, 4u, DxtVariant.DXT1)
        assertEquals(Pair(4u, 4u), decoder.dimensions())
        assertEquals(ColorType.Rgb8, decoder.colorType())

        val output = ByteArray(48)
        decoder.readImage(output)
        for (i in 0 until 16) {
            assertEquals(0xFF.toByte(), output[i * 3], "Red channel at pixel $i")
            assertEquals(0.toByte(), output[i * 3 + 1], "Green channel at pixel $i")
            assertEquals(0.toByte(), output[i * 3 + 2], "Blue channel at pixel $i")
        }
    }
}
