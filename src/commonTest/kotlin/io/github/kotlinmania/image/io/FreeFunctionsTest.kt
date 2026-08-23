// port-lint: tests io/free_functions.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ColorType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FreeFunctionsTest {
    @Test
    fun testGuessFormat() {
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals(ImageFormat.Png, guessFormat(pngBytes))

        val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertEquals(ImageFormat.Jpeg, guessFormat(jpegBytes))

        val gifBytes = "GIF89a".encodeToByteArray()
        assertEquals(ImageFormat.Gif, guessFormat(gifBytes))
    }

    private class MockDecoder(
        var scanlineNumber: Long = 0,
        val scanlineBytes: Int = 5,
    ) : ImageDecoder {
        override fun dimensions(): Pair<UInt, UInt> = Pair(5u, 5u)

        override fun colorType(): ColorType = ColorType.L8

        override fun readImage(buf: ByteArray) {}
    }

    @Test
    fun testLoadRect() {
        val data = ByteArray(25) { it.toByte() }

        fun seekScanline(d: ImageDecoder, n: ULong) {
            (d as MockDecoder).scanlineNumber = n.toLong()
        }

        fun readScanline(d: ImageDecoder, buf: ByteArray) {
            val mock = d as MockDecoder
            val bytesRead = mock.scanlineNumber * mock.scanlineBytes
            if (bytesRead >= 25) return
            val len = minOf(mock.scanlineBytes.toLong(), 25 - bytesRead).toInt()
            data.copyInto(buf, destinationOffset = 0, startIndex = bytesRead.toInt(), endIndex = bytesRead.toInt() + len)
            mock.scanlineNumber += 1
        }

        for (scanlineBytes in 1 until 30) {
            val output = ByteArray(26)
            val decoder = MockDecoder(scanlineNumber = 0, scanlineBytes = scanlineBytes)
            loadRect(
                0u,
                0u,
                5u,
                5u,
                output,
                5,
                decoder,
                scanlineBytes,
                ::seekScanline,
                ::readScanline,
            )
            assertContentEquals(data, output.copyOfRange(0, 25))
            assertEquals(0, output[25])
        }
    }
}
