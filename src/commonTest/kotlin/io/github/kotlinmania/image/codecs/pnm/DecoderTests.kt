// port-lint: tests image/src/codecs/pnm/decoder.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DecoderTests {
    @Test
    fun pamBlackAndWhite() {
        val pamData =
            (
                "P7\n" +
                    "WIDTH 4\n" +
                    "HEIGHT 4\n" +
                    "DEPTH 1\n" +
                    "MAXVAL 1\n" +
                    "TUPLTYPE BLACKANDWHITE\n" +
                    "# Comment line\n" +
                    "ENDHDR\n"
            ).encodeToByteArray() +
                byteArrayOf(1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1)

        val decoder = PnmDecoder(pamData)
        assertEquals(ColorType.L8, decoder.colorType())
        assertEquals(ExtendedColorType.L1, decoder.originalColorType())
        assertEquals(Pair(4u, 4u), decoder.dimensions())
        assertEquals(PnmSubtype.ArbitraryMap, decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected =
            byteArrayOf(
                0xFF.toByte(),
                0x00,
                0x00,
                0xFF.toByte(),
                0xFF.toByte(),
                0x00,
                0x00,
                0xFF.toByte(),
                0xFF.toByte(),
                0x00,
                0x00,
                0xFF.toByte(),
                0xFF.toByte(),
                0x00,
                0x00,
                0xFF.toByte(),
            )
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pamBlackAndWhiteAlpha() {
        val pamData =
            (
                "P7\n" +
                    "WIDTH 2\n" +
                    "HEIGHT 2\n" +
                    "DEPTH 2\n" +
                    "MAXVAL 1\n" +
                    "TUPLTYPE BLACKANDWHITE_ALPHA\n" +
                    "# Comment line\n" +
                    "ENDHDR\n"
            ).encodeToByteArray() +
                byteArrayOf(1, 0, 0, 1, 1, 0, 0, 1)

        val decoder = PnmDecoder(pamData)
        assertEquals(ColorType.La8, decoder.colorType())
        assertEquals(ExtendedColorType.La1, decoder.originalColorType())
        assertEquals(Pair(2u, 2u), decoder.dimensions())
        assertEquals(PnmSubtype.ArbitraryMap, decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected =
            byteArrayOf(
                0xFF.toByte(),
                0x00,
                0x00,
                0xFF.toByte(),
                0xFF.toByte(),
                0x00,
                0x00,
                0xFF.toByte(),
            )
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pamGrayscale() {
        val pamData =
            (
                "P7\n" +
                    "WIDTH 4\n" +
                    "HEIGHT 4\n" +
                    "DEPTH 1\n" +
                    "MAXVAL 255\n" +
                    "TUPLTYPE GRAYSCALE\n" +
                    "# Comment line\n" +
                    "ENDHDR\n"
            ).encodeToByteArray() +
                byteArrayOf(
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                )

        val decoder = PnmDecoder(pamData)
        assertEquals(ColorType.L8, decoder.colorType())
        assertEquals(Pair(4u, 4u), decoder.dimensions())
        assertEquals(PnmSubtype.ArbitraryMap, decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected =
            byteArrayOf(
                0xde.toByte(),
                0xad.toByte(),
                0xbe.toByte(),
                0xef.toByte(),
                0xde.toByte(),
                0xad.toByte(),
                0xbe.toByte(),
                0xef.toByte(),
                0xde.toByte(),
                0xad.toByte(),
                0xbe.toByte(),
                0xef.toByte(),
                0xde.toByte(),
                0xad.toByte(),
                0xbe.toByte(),
                0xef.toByte(),
            )
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pamGrayscaleAlpha() {
        val pamData =
            (
                "P7\n" +
                    "HEIGHT 1\n" +
                    "WIDTH 2\n" +
                    "MAXVAL 65535\n" +
                    "DEPTH 2\n" +
                    "TUPLTYPE GRAYSCALE_ALPHA\n" +
                    "# Comment line\n" +
                    "ENDHDR\n"
            ).encodeToByteArray() +
                byteArrayOf(
                    0xdc.toByte(),
                    0xba.toByte(),
                    0x32.toByte(),
                    0x10.toByte(),
                    0xdc.toByte(),
                    0xba.toByte(),
                    0x32.toByte(),
                    0x10.toByte(),
                )

        val decoder = PnmDecoder(pamData)
        assertEquals(ColorType.La16, decoder.colorType())
        assertEquals(ExtendedColorType.La16, decoder.originalColorType())
        assertEquals(Pair(2u, 1u), decoder.dimensions())
        assertEquals(PnmSubtype.ArbitraryMap, decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected =
            byteArrayOf(
                0xba.toByte(),
                0xdc.toByte(),
                0x10.toByte(),
                0x32.toByte(),
                0xba.toByte(),
                0xdc.toByte(),
                0x10.toByte(),
                0x32.toByte(),
            )
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pamRgb() {
        val pamData =
            (
                "P7\n" +
                    "# Comment line\n" +
                    "MAXVAL 255\n" +
                    "TUPLTYPE RGB\n" +
                    "DEPTH 3\n" +
                    "WIDTH 2\n" +
                    "HEIGHT 2\n" +
                    "ENDHDR\n"
            ).encodeToByteArray() +
                byteArrayOf(
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                )

        val decoder = PnmDecoder(pamData)
        assertEquals(ColorType.Rgb8, decoder.colorType())
        assertEquals(Pair(2u, 2u), decoder.dimensions())
        assertEquals(PnmSubtype.ArbitraryMap, decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected =
            byteArrayOf(
                0xde.toByte(),
                0xad.toByte(),
                0xbe.toByte(),
                0xef.toByte(),
                0xde.toByte(),
                0xad.toByte(),
                0xbe.toByte(),
                0xef.toByte(),
                0xde.toByte(),
                0xad.toByte(),
                0xbe.toByte(),
                0xef.toByte(),
            )
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pamRgbAlpha() {
        val pamData =
            (
                "P7\n" +
                    "WIDTH 1\n" +
                    "HEIGHT 3\n" +
                    "DEPTH 4\n" +
                    "MAXVAL 15\n" +
                    "TUPLTYPE RGB_ALPHA\n" +
                    "# Comment line\n" +
                    "ENDHDR\n"
            ).encodeToByteArray() +
                byteArrayOf(
                    0x00,
                    0x01,
                    0x02,
                    0x03,
                    0x0a,
                    0x0b,
                    0x0c,
                    0x0d,
                    0x05,
                    0x06,
                    0x07,
                    0x08,
                )

        val decoder = PnmDecoder(pamData)
        assertEquals(ColorType.Rgba8, decoder.colorType())
        assertEquals(ExtendedColorType.Rgba8, decoder.originalColorType())
        assertEquals(Pair(1u, 3u), decoder.dimensions())
        assertEquals(PnmSubtype.ArbitraryMap, decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected =
            byteArrayOf(
                0x00,
                0x11,
                0x22,
                0x33,
                0xaa.toByte(),
                0xbb.toByte(),
                0xcc.toByte(),
                0xdd.toByte(),
                0x55,
                0x66,
                0x77,
                0x88.toByte(),
            )
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pbmBinary() {
        val pbmBinary = "P4 6 2\n".encodeToByteArray() + byteArrayOf(0b01101100.toByte(), 0b10110111.toByte())
        val decoder = PnmDecoder(pbmBinary)
        assertEquals(ColorType.L8, decoder.colorType())
        assertEquals(ExtendedColorType.L1, decoder.originalColorType())
        assertEquals(Pair(6u, 2u), decoder.dimensions())
        assertEquals(PnmSubtype.Bitmap(SampleEncoding.Binary), decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected = byteArrayOf(255.toByte(), 0, 0, 255.toByte(), 0, 0, 0, 255.toByte(), 0, 0, 255.toByte(), 0)
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pbmAscii() {
        val pbmAscii = "P1 6 2\n 0 1 1 0 1 1\n1 0 1 1 0\t\n\u000b\u000c\r1".encodeToByteArray()
        val decoder = PnmDecoder(pbmAscii)
        assertEquals(ColorType.L8, decoder.colorType())
        assertEquals(ExtendedColorType.L1, decoder.originalColorType())
        assertEquals(Pair(6u, 2u), decoder.dimensions())
        assertEquals(PnmSubtype.Bitmap(SampleEncoding.Ascii), decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected = byteArrayOf(255.toByte(), 0, 0, 255.toByte(), 0, 0, 0, 255.toByte(), 0, 0, 255.toByte(), 0)
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pbmAsciiNoSpace() {
        val pbmAscii = "P1 6 2\n011011101101".encodeToByteArray()
        val decoder = PnmDecoder(pbmAscii)
        assertEquals(ColorType.L8, decoder.colorType())
        assertEquals(ExtendedColorType.L1, decoder.originalColorType())
        assertEquals(Pair(6u, 2u), decoder.dimensions())
        assertEquals(PnmSubtype.Bitmap(SampleEncoding.Ascii), decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val expected = byteArrayOf(255.toByte(), 0, 0, 255.toByte(), 0, 0, 0, 255.toByte(), 0, 0, 255.toByte(), 0)
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun pgmBinary() {
        val elements = ByteArray(16) { it.toByte() }
        val pgmBinary = "P5 4 4 255\n".encodeToByteArray() + elements
        val decoder = PnmDecoder(pgmBinary)
        assertEquals(ColorType.L8, decoder.colorType())
        assertEquals(Pair(4u, 4u), decoder.dimensions())
        assertEquals(PnmSubtype.Graymap(SampleEncoding.Binary), decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        assertTrue(image.contentEquals(elements))
    }

    @Test
    fun pgmAscii() {
        val pgmAscii = "P2 4 4 255\n 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15".encodeToByteArray()
        val decoder = PnmDecoder(pgmAscii)
        assertEquals(ColorType.L8, decoder.colorType())
        assertEquals(Pair(4u, 4u), decoder.dimensions())
        assertEquals(PnmSubtype.Graymap(SampleEncoding.Ascii), decoder.subtype())

        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val elements = ByteArray(16) { it.toByte() }
        assertTrue(image.contentEquals(elements))
    }

    @Test
    fun ppmAscii() {
        val ascii = "P3 1 1 2000\n0 1000 2000".encodeToByteArray()
        val decoder = PnmDecoder(ascii)
        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
        val v0 = 0
        val v1 = (65535 / 2 + 1)
        val v2 = 65535
        val expected =
            byteArrayOf(
                (v0 and 0xFF).toByte(),
                ((v0 ushr 8) and 0xFF).toByte(),
                (v1 and 0xFF).toByte(),
                ((v1 ushr 8) and 0xFF).toByte(),
                (v2 and 0xFF).toByte(),
                ((v2 ushr 8) and 0xFF).toByte(),
            )
        assertTrue(image.contentEquals(expected))
    }

    @Test
    fun dimensionOverflow() {
        val pamData =
            (
                "P7\n" +
                    "# Comment line\n" +
                    "MAXVAL 255\n" +
                    "TUPLTYPE RGB\n" +
                    "DEPTH 3\n" +
                    "WIDTH 4294967295\n" +
                    "HEIGHT 4294967295\n" +
                    "ENDHDR\n"
            ).encodeToByteArray() +
                byteArrayOf(
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                    0xde.toByte(),
                    0xad.toByte(),
                    0xbe.toByte(),
                    0xef.toByte(),
                    0xde.toByte(),
                )

        assertFailsWith<ImageError> {
            PnmDecoder(pamData)
        }
    }

    @Test
    fun dataTooShort() {
        val data = "P3 16 16 1\n".encodeToByteArray()
        val decoder = PnmDecoder(data)
        val image = ByteArray(decoder.totalBytes().toInt())
        assertFailsWith<ImageError> {
            decoder.readImage(image)
        }
    }

    @Test
    fun noIntegersWithPlus() {
        val data = "P3 +1 1 1\n".encodeToByteArray()
        assertFailsWith<ImageError> {
            PnmDecoder(data)
        }
    }

    @Test
    fun incompletePnmHeader() {
        val data = "P5 2 3 \n".encodeToByteArray()
        assertFailsWith<ImageError> {
            PnmDecoder(data)
        }
    }

    @Test
    fun leadingZeros() {
        val data = "P2 03 00000000000002 00100\n011 22 033\n44 055 66\n".encodeToByteArray()
        val decoder = PnmDecoder(data)
        val image = ByteArray(decoder.totalBytes().toInt())
        decoder.readImage(image)
    }

    @Test
    fun headerOverflow() {
        val data = "P1 4294967295 4294967297\n".encodeToByteArray()
        assertFailsWith<ImageError> {
            PnmDecoder(data)
        }
    }

    @Test
    fun headerLargeDimension() {
        val data = "P4 1 01234567890\n".encodeToByteArray()
        val decoder = PnmDecoder(data)
        assertEquals(Pair(1u, 1234567890u), decoder.dimensions())
    }
}
