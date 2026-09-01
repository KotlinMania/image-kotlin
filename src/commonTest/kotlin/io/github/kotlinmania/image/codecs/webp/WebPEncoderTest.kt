// port-lint: tests codecs/webp/encoder.rs
package io.github.kotlinmania.image.codecs.webp

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebPEncoderTest {
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
}
