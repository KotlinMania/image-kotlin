// port-lint: tests codecs/jpeg/decoder.rs
package io.github.kotlinmania.image.codecs.jpeg

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.Limits
import io.github.kotlinmania.image.metadata.Orientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JpegDecoderTest {
    @Test
    fun testJpegRoundtripRgb() {
        val width = 8u
        val height = 8u
        val original = ByteArray(width.toInt() * height.toInt() * 3) { (it % 256).toByte() }

        val out = BufferIoWrite()
        val encoder = JpegEncoder(out, 100u)
        encoder.writeImage(original, width, height, ExtendedColorType.Rgb8)

        val encoded = out.toByteArray()
        val decoder = JpegDecoder(encoded)

        assertEquals(Pair(width, height), decoder.dimensions())
        assertEquals(ColorType.Rgb8, decoder.colorType())
        assertEquals((width * height * 3u).toULong(), decoder.totalBytes())

        val decoded = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(decoded)
        assertEquals(original.size, decoded.size)
    }

    @Test
    fun testJpegRoundtripGrayscale() {
        val width = 8u
        val height = 8u
        val original = ByteArray(width.toInt() * height.toInt()) { (it * 30 % 256).toByte() }

        val out = BufferIoWrite()
        val encoder = JpegEncoder(out, 100u)
        encoder.writeImage(original, width, height, ExtendedColorType.L8)

        val encoded = out.toByteArray()
        val decoder = JpegDecoder(encoded)

        assertEquals(Pair(width, height), decoder.dimensions())
        assertEquals(ColorType.L8, decoder.colorType())

        val decoded = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(decoded)
        assertEquals(original.size, decoded.size)
    }

    @Test
    fun testJpegInvalidHeader() {
        assertFailsWith<ImageError.Decoding> {
            JpegDecoder(byteArrayOf(0x00, 0x01, 0x02))
        }
    }

    @Test
    fun testLimitsEnforcement() {
        val width = 8u
        val height = 8u
        val original = ByteArray(width.toInt() * height.toInt() * 3)

        val out = BufferIoWrite()
        val encoder = JpegEncoder(out, 100u)
        encoder.writeImage(original, width, height, ExtendedColorType.Rgb8)

        val decoder = JpegDecoder(out.toByteArray())
        val strictLimits = Limits(maxImageWidth = 4u, maxImageHeight = 4u)

        assertFailsWith<ImageError.Limits> {
            decoder.setLimits(strictLimits)
        }
    }

    @Test
    fun testExifOrientation() {
        testExifOrientationDefault()
    }

    @Test
    fun testExifOrientationDefault() {
        val width = 8u
        val height = 8u
        val original = ByteArray(width.toInt() * height.toInt() * 3)

        val out = BufferIoWrite()
        val encoder = JpegEncoder(out, 100u)
        encoder.writeImage(original, width, height, ExtendedColorType.Rgb8)

        val decoder = JpegDecoder(out.toByteArray())
        assertEquals(Orientation.NoTransforms, decoder.orientation())
    }
}
