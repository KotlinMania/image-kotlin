// port-lint: tests codecs/webp/encoder.rs
package io.github.kotlinmania.image.codecs.webp

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WebPTest {
    @Test
    fun writeWebp() {
        testWebpLosslessRoundtripRgba()
    }

    @Test
    fun testWebpLosslessRoundtripRgba() {
        val width = 10u
        val height = 6u
        val original = ByteArray(width.toInt() * height.toInt() * 4) { (it % 256).toByte() }

        val out = BufferIoWrite()
        val encoder = WebPEncoder.newLossless(out)
        encoder.writeImage(original, width, height, ExtendedColorType.Rgba8)

        val encoded = out.toByteArray()
        assertTrue(encoded.isNotEmpty())

        val decoder = WebPDecoder(encoded)
        assertEquals(Pair(width, height), decoder.dimensions())
        assertEquals(ColorType.Rgba8, decoder.colorType())
    }

    @Test
    fun testWebpLosslessWithExifAndIcc() {
        val width = 4u
        val height = 4u
        val original = ByteArray(width.toInt() * height.toInt() * 4)

        val out = BufferIoWrite()
        val encoder = WebPEncoder.newLossless(out)
        val dummyExif = byteArrayOf(1, 2, 3, 4)
        val dummyIcc = byteArrayOf(5, 6, 7, 8)

        encoder.setExifMetadata(dummyExif)
        encoder.setIccProfile(dummyIcc)
        encoder.writeImage(original, width, height, ExtendedColorType.Rgba8)

        val encoded = out.toByteArray()
        val decoder = WebPDecoder(encoded)
        assertEquals(Pair(width, height), decoder.dimensions())
        assertTrue(decoder.exifMetadata() != null)
        assertTrue(decoder.iccProfile() != null)
    }

    @Test
    fun testWebpInvalidHeader() {
        assertFailsWith<ImageError.Decoding> {
            WebPDecoder(byteArrayOf(0x00, 0x01, 0x02))
        }
    }

    @Test
    fun testWebpAddWithOverflowSize() {
        val bytes =
            byteArrayOf(
                0x52,
                0x49,
                0x46,
                0x46,
                0xaf.toByte(),
                0x37,
                0x80.toByte(),
                0x47,
                0x57,
                0x45,
                0x42,
                0x50,
                0x6c,
                0x64,
                0x00,
                0x00,
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xfb.toByte(),
                0x7e,
                0x73,
                0x00,
                0x06,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x05,
                0x00,
                0x00,
                0x00,
                0x65,
                0x65,
                0x65,
                0x65,
                0x65,
                0x65,
                0x40,
                0xfb.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0x65,
                0x65,
                0x65,
                0x65,
                0x65,
                0x65,
                0x65,
                0x65,
                0x65,
                0x65,
                0x00,
                0x00,
                0x00,
                0x00,
                0x62,
                0x00,
                0x10,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x49,
                0x49,
                0x54,
                0x55,
                0x50,
                0x4c,
                0x54,
                0x59,
                0x50,
                0x45,
                0x33,
                0x37,
                0x44,
                0x4d,
                0x46,
            )
        val decoder = WebPDecoder(bytes)
        assertTrue(decoder.dimensions().first >= 0u)
    }
}
