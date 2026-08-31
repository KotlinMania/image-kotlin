// port-lint: tests image/src/codecs/farbfeld.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FarbfeldTest {
    @Test
    fun testDimensionOverflow() {
        val header =
            byteArrayOf(
                0x66,
                0x61,
                0x72,
                0x62,
                0x66,
                0x65,
                0x6c,
                0x64, // farbfeld
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
            )
        assertFailsWith<ImageError> {
            FarbfeldDecoder(header)
        }
    }

    @Test
    fun testEncodeDecodeRoundtrip() {
        val width = 2u
        val height = 2u
        // 2x2 RGBA16 -> 4 pixels * 8 bytes/pixel = 32 bytes
        val originalData = ByteArray(32) { (it * 7).toByte() }

        val sink = BufferIoWrite()
        val encoder = FarbfeldEncoder(sink)
        encoder.encode(originalData, width, height)

        val encodedBytes = sink.toByteArray()
        assertEquals(8 + 8 + 32, encodedBytes.size)

        val decoder = FarbfeldDecoder(encodedBytes)
        assertEquals(Pair(width, height), decoder.dimensions())
        assertEquals(ColorType.Rgba16, decoder.colorType())
        assertEquals(32uL, decoder.totalBytes())

        val decodedData = ByteArray(32)
        decoder.readImage(decodedData)
        assertTrue(originalData.contentEquals(decodedData))
    }

    @Test
    fun testInvalidMagicThrows() {
        val badHeader = ByteArray(16) { 0 }
        assertFailsWith<ImageError> {
            FarbfeldDecoder(badHeader)
        }
    }

    private val rectangleIn =
        byteArrayOf(
            0x66,
            0x61,
            0x72,
            0x62,
            0x66,
            0x65,
            0x6c,
            0x64, // "farbfeld"
            0x00,
            0x00,
            0x00,
            0x02,
            0x00,
            0x00,
            0x00,
            0x03, // 2 x 3
            0xFF.toByte(),
            0x01,
            0xFE.toByte(),
            0x02,
            0xFD.toByte(),
            0x03,
            0xFC.toByte(),
            0x04,
            0xFB.toByte(),
            0x05,
            0xFA.toByte(),
            0x06,
            0xF9.toByte(),
            0x07,
            0xF8.toByte(),
            0x08,
            0xF7.toByte(),
            0x09,
            0xF6.toByte(),
            0x0A,
            0xF5.toByte(),
            0x0B,
            0xF4.toByte(),
            0x0C,
            0xF3.toByte(),
            0x0D,
            0xF2.toByte(),
            0x0E,
            0xF1.toByte(),
            0x0F,
            0xF0.toByte(),
            0x10,
            0xEF.toByte(),
            0x11,
            0xEE.toByte(),
            0x12,
            0xED.toByte(),
            0x13,
            0xEC.toByte(),
            0x14,
            0xEB.toByte(),
            0x15,
            0xEA.toByte(),
            0x16,
            0xE9.toByte(),
            0x17,
            0xE8.toByte(),
            0x18,
        )

    private fun assertReadRect(x: UInt, y: UInt, width: UInt, height: UInt, expWide: UShortArray) {
        val outBuf = ByteArray(64)
        val decoder = FarbfeldDecoder(rectangleIn)
        decoder.readRect(x, y, width, height, outBuf, width.toInt() * 8)
        val exp = ByteArray(expWide.size * 2)
        for (i in expWide.indices) {
            val v = expWide[i].toInt()
            exp[i * 2] = (v ushr 8).toByte()
            exp[i * 2 + 1] = v.toByte()
        }
        for (i in exp.indices) {
            assertEquals(exp[i], outBuf[i])
        }
    }

    @Test
    fun dimensionOverflow() {
        val header =
            byteArrayOf(
                0x66,
                0x61,
                0x72,
                0x62,
                0x66,
                0x65,
                0x6c,
                0x64, // farbfeld
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
            )
        assertFailsWith<ImageError> {
            FarbfeldDecoder(header)
        }
    }

    @Test
    fun readRect1x2() {
        val exp = ushortArrayOf(0xF30Du, 0xF20Eu, 0xF10Fu, 0xF010u, 0xEB15u, 0xEA16u, 0xE917u, 0xE818u)
        assertReadRect(1u, 1u, 1u, 2u, exp)
    }

    @Test
    fun readRect2x2() {
        val exp =
            ushortArrayOf(
                0xFF01u,
                0xFE02u,
                0xFD03u,
                0xFC04u,
                0xFB05u,
                0xFA06u,
                0xF907u,
                0xF808u,
                0xF709u,
                0xF60Au,
                0xF50Bu,
                0xF40Cu,
                0xF30Du,
                0xF20Eu,
                0xF10Fu,
                0xF010u,
            )
        assertReadRect(0u, 0u, 2u, 2u, exp)
    }

    @Test
    fun readRect2x1() {
        val exp = ushortArrayOf(0xEF11u, 0xEE12u, 0xED13u, 0xEC14u, 0xEB15u, 0xEA16u, 0xE917u, 0xE818u)
        assertReadRect(0u, 2u, 2u, 1u, exp)
    }

    @Test
    fun readRect2x3() {
        val exp =
            ushortArrayOf(
                0xFF01u,
                0xFE02u,
                0xFD03u,
                0xFC04u,
                0xFB05u,
                0xFA06u,
                0xF907u,
                0xF808u,
                0xF709u,
                0xF60Au,
                0xF50Bu,
                0xF40Cu,
                0xF30Du,
                0xF20Eu,
                0xF10Fu,
                0xF010u,
                0xEF11u,
                0xEE12u,
                0xED13u,
                0xEC14u,
                0xEB15u,
                0xEA16u,
                0xE917u,
                0xE818u,
            )
        assertReadRect(0u, 0u, 2u, 3u, exp)
    }

    @Test
    fun readRectInStream() {
        val expWide = ushortArrayOf(0xEF11u, 0xEE12u, 0xED13u, 0xEC14u)
        val prologue = "This is a 31-byte-long prologue".encodeToByteArray()
        val input = prologue + rectangleIn
        val decoder = FarbfeldDecoder(input.copyOfRange(31, input.size))
        val outBuf = ByteArray(64)
        decoder.readRect(0u, 2u, 1u, 1u, outBuf, 8)
        val exp = ByteArray(expWide.size * 2)
        for (i in expWide.indices) {
            val v = expWide[i].toInt()
            exp[i * 2] = (v ushr 8).toByte()
            exp[i * 2 + 1] = v.toByte()
        }
        for (i in exp.indices) {
            assertEquals(exp[i], outBuf[i])
        }
    }
}

