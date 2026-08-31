// port-lint: tests codecs/dds.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DdsTest {
    @Test
    fun testDimensionOverflow() {
        // A DXT1 header set to 0xFFFF_FFFC width and height (the highest u32%4 == 0)
        val header =
            byteArrayOf(
                0x44,
                0x44,
                0x53,
                0x20,
                0x7C,
                0x0,
                0x0,
                0x0,
                0x7,
                0x10,
                0x8,
                0x0,
                0xFC.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFC.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0x0,
                0xC0.toByte(),
                0x12,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x1,
                0x0,
                0x0,
                0x0,
                0x49,
                0x4D,
                0x41,
                0x47,
                0x45,
                0x4D,
                0x41,
                0x47,
                0x49,
                0x43,
                0x4B,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x20,
                0x0,
                0x0,
                0x0,
                0x4,
                0x0,
                0x0,
                0x0,
                0x44,
                0x58,
                0x54,
                0x31,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x10,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
                0x0,
            )

        assertFailsWith<ImageError> {
            DdsDecoder(header)
        }
    }

    @Test
    fun testInvalidSignature() {
        val bad = byteArrayOf(0, 0, 0, 0)
        assertFailsWith<ImageError.Decoding> {
            DdsDecoder(bad)
        }
    }

    @Test
    fun testValidDdsDxt1() {
        // Construct a valid 4x4 DXT1 DDS file header + 8 bytes data
        val header = ByteArray(128 + 8)
        // DDS magic: "DDS "
        header[0] = 0x44
        header[1] = 0x44
        header[2] = 0x53
        header[3] = 0x20
        // Header size: 124
        header[4] = 124
        header[5] = 0
        header[6] = 0
        header[7] = 0
        // Flags: 0x1007 (DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT)
        header[8] = 0x07
        header[9] = 0x10
        header[10] = 0x00
        header[11] = 0x00
        // Height: 4
        header[12] = 4
        header[13] = 0
        header[14] = 0
        header[15] = 0
        // Width: 4
        header[16] = 4
        header[17] = 0
        header[18] = 0
        header[19] = 0

        // PixelFormat offset: 4 + 72 = 76 (i.e. bytes 76..107)
        // PixelFormat size: 32
        header[76] = 32
        header[77] = 0
        header[78] = 0
        header[79] = 0
        // PixelFormat flags: DDPF_FOURCC = 0x4
        header[80] = 0x04
        header[81] = 0
        header[82] = 0
        header[83] = 0
        // FourCC: "DXT1"
        header[84] = 'D'.code.toByte()
        header[85] = 'X'.code.toByte()
        header[86] = 'T'.code.toByte()
        header[87] = '1'.code.toByte()

        // Caps: DDSCAPS_TEXTURE = 0x1000
        header[108] = 0x00
        header[109] = 0x10
        header[110] = 0x00
        header[111] = 0x00

        // 8 bytes of DXT1 payload at offset 128 (solid white)
        header[128] = 0xFF.toByte()
        header[129] = 0xFF.toByte()
        header[130] = 0xFF.toByte()
        header[131] = 0xFF.toByte()

        val decoder = DdsDecoder(header)
        assertEquals(Pair(4u, 4u), decoder.dimensions())
        assertEquals(ColorType.Rgb8, decoder.colorType())

        val output = ByteArray(48)
        decoder.readImage(output)
        for (i in 0 until 16) {
            assertEquals(0xFF.toByte(), output[i * 3])
            assertEquals(0xFF.toByte(), output[i * 3 + 1])
            assertEquals(0xFF.toByte(), output[i * 3 + 2])
        }
    }
}
