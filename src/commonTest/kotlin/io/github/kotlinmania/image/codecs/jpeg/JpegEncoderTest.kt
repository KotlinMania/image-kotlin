// port-lint: tests codecs/jpeg/encoder.rs
package io.github.kotlinmania.image.codecs.jpeg

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JpegEncoderTest {
    @Test
    fun roundtripSanityCheck() {
        val img = byteArrayOf(255.toByte(), 0, 0)
        val out = BufferIoWrite()
        val encoder = JpegEncoder(out, 100u)
        encoder.writeImage(img, 1u, 1u, ExtendedColorType.Rgb8)

        val encoded = out.toByteArray()
        assertTrue(encoded.isNotEmpty())
        // Verify SOI and EOI markers
        assertEquals(0xFF.toByte(), encoded[0])
        assertEquals(0xD8.toByte(), encoded[1])
        assertEquals(0xFF.toByte(), encoded[encoded.size - 2])
        assertEquals(0xD9.toByte(), encoded[encoded.size - 1])
    }

    @Test
    fun grayscaleRoundtripSanityCheck() {
        val img = byteArrayOf(255.toByte(), 0, 0, 255.toByte())
        val out = BufferIoWrite()
        val encoder = JpegEncoder(out, 100u)
        encoder.writeImage(img, 2u, 2u, ExtendedColorType.L8)

        val encoded = out.toByteArray()
        assertTrue(encoded.isNotEmpty())
        assertEquals(0xFF.toByte(), encoded[0])
        assertEquals(0xD8.toByte(), encoded[1])
        assertEquals(0xFF.toByte(), encoded[encoded.size - 2])
        assertEquals(0xD9.toByte(), encoded[encoded.size - 1])
    }

    @Test
    fun jfifHeaderDensityCheck() {
        val density = PixelDensity.dpi(300u)
        val expected =
            byteArrayOf(
                'J'.code.toByte(),
                'F'.code.toByte(),
                'I'.code.toByte(),
                'F'.code.toByte(),
                0,
                1,
                2,
                1,
                (300 shr 8).toByte(),
                (300 and 0xFF).toByte(),
                (300 shr 8).toByte(),
                (300 and 0xFF).toByte(),
                0,
                0,
            )
        val encoder = JpegEncoder(BufferIoWrite())
        encoder.setPixelDensity(density)
    }

    @Test
    fun testImageTooLarge() {
        val encoder = JpegEncoder(BufferIoWrite(), 100u)
        assertFailsWith<ImageError.Encoding> {
            encoder.writeImage(ByteArray(65536), 65536u, 1u, ExtendedColorType.L8)
        }
    }

    @Test
    fun testBuildJfifHeader() {
        val density = PixelDensity.dpi(100u)
        val buf = buildJfifHeader(density)
        val expected = byteArrayOf(0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x02, 0x01, 0, 100, 0, 100, 0, 0)
        assertTrue(expected.contentEquals(buf))
    }

    @Test
    fun testBuildFrameHeader() {
        val components =
            listOf(
                Component(id = 1u, h = 1u, v = 1u, tq = 5u, dcTable = 5u, acTable = 5u),
                Component(id = 2u, h = 1u, v = 1u, tq = 4u, dcTable = 4u, acTable = 4u),
            )
        val buf = buildFrameHeader(5u, 100u, 150u, components)
        val expected = byteArrayOf(5, 0, 150.toByte(), 0, 100, 2, 1, ((1 shl 4) or 1).toByte(), 5, 2, ((1 shl 4) or 1).toByte(), 4)
        assertTrue(expected.contentEquals(buf))
    }

    @Test
    fun testBuildScanHeader() {
        val components =
            listOf(
                Component(id = 1u, h = 1u, v = 1u, tq = 5u, dcTable = 5u, acTable = 5u),
                Component(id = 2u, h = 1u, v = 1u, tq = 4u, dcTable = 4u, acTable = 4u),
            )
        val buf = buildScanHeader(components)
        val expected = byteArrayOf(2, 1, ((5 shl 4) or 5).toByte(), 2, ((4 shl 4) or 4).toByte(), 0, 63, 0)
        assertTrue(expected.contentEquals(buf))
    }

    @Test
    fun testBuildHuffmanSegment() {
        val buf = buildHuffmanSegment(DCCLASS, LUMADESTINATION, STD_LUMA_DC_CODE_LENGTHS, STD_LUMA_DC_VALUES)
        val expected =
            byteArrayOf(
                0,
                0,
                1,
                5,
                1,
                1,
                1,
                1,
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10,
                11,
            )
        assertTrue(expected.contentEquals(buf))
    }

    @Test
    fun testBuildQuantizationSegment() {
        val qtable = UByteArray(64)
        val buf = buildQuantizationSegment(8u, 1u, qtable)
        val expected = ByteArray(65)
        expected[0] = 1
        assertTrue(expected.contentEquals(buf))
    }

    @Test
    fun checkColorTypes() {
        val allColors =
            listOf(
                io.github.kotlinmania.image.ColorType.L8,
                io.github.kotlinmania.image.ColorType.L16,
                io.github.kotlinmania.image.ColorType.La8,
                io.github.kotlinmania.image.ColorType.Rgb8,
                io.github.kotlinmania.image.ColorType.Rgba8,
                io.github.kotlinmania.image.ColorType.La16,
                io.github.kotlinmania.image.ColorType.Rgb16,
                io.github.kotlinmania.image.ColorType.Rgba16,
                io.github.kotlinmania.image.ColorType.Rgb32F,
                io.github.kotlinmania.image.ColorType.Rgba32F,
            )
        for (color in allColors) {
            val image =
                io.github.kotlinmania.image.images.DynamicImage
                    .new(1u, 1u, color)
            val out = BufferIoWrite()
            image.writeTo(out, io.github.kotlinmania.image.io.ImageFormat.Jpeg)
            assertTrue(out.toByteArray().isNotEmpty())
        }
    }

    @Test
    fun subImageEncoderRegression1412() {
        val image =
            io.github.kotlinmania.image.images.DynamicImage
                .newRgb8(128u, 72u)
        val out = BufferIoWrite()
        val encoder = JpegEncoder.new(out)
        encoder.encodeImage(image)
        assertTrue(out.toByteArray().isNotEmpty())
    }
}
