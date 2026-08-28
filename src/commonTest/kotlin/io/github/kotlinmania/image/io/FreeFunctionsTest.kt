// port-lint: tests io/free_functions.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
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

        override fun readImage(buf: ByteArray) {
            for (i in buf.indices) {
                buf[i] = (i % 256).toByte()
            }
        }

        override fun readImageBoxed(buf: ByteArray) {
            readImage(buf)
        }

        fun seekScanline(n: ULong) {
            scanlineNumber = n.toLong()
        }

        fun readScanline(buf: ByteArray, data: ByteArray) {
            val bytesRead = scanlineNumber * scanlineBytes
            if (bytesRead >= data.size) return
            val len = minOf(scanlineBytes.toLong(), data.size - bytesRead).toInt()
            data.copyInto(buf, destinationOffset = 0, startIndex = bytesRead.toInt(), endIndex = bytesRead.toInt() + len)
            scanlineNumber += 1
        }
    }

    @Test
    fun testDecoderToVec() {
        val decoder = MockDecoder()
        val vec = decoderToVec(decoder)
        assertEquals(25, vec.size)
        assertEquals(0, vec[0])
        assertEquals(1, vec[1])
    }

    @Test
    fun testSaveBufferWithFormat() {
        val writer = BufferIoWrite()
        val raw = ByteArray(32) { (it + 1).toByte() }
        saveBufferWithFormat(writer, raw, 2u, 2u, ExtendedColorType.Rgba16, ImageFormat.Farbfeld)
        val written = writer.toByteArray()
        assertEquals(8 + 8 + 32, written.size) // magic (8) + header (8) + 4 rgba16 pixels in farbfeld
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
            var output = ByteArray(26)
            var decoder = MockDecoder(scanlineNumber = 0, scanlineBytes = scanlineBytes)
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

            output = ByteArray(26)
            decoder = MockDecoder(scanlineNumber = 0, scanlineBytes = scanlineBytes)
            loadRect(
                3u,
                2u,
                1u,
                1u,
                output,
                1,
                decoder,
                scanlineBytes,
                ::seekScanline,
                ::readScanline,
            )
            assertContentEquals(byteArrayOf(13, 0), output.copyOfRange(0, 2))

            output = ByteArray(26)
            decoder = MockDecoder(scanlineNumber = 0, scanlineBytes = scanlineBytes)
            loadRect(
                3u,
                2u,
                2u,
                2u,
                output,
                2,
                decoder,
                scanlineBytes,
                ::seekScanline,
                ::readScanline,
            )
            assertContentEquals(byteArrayOf(13, 14, 18, 19, 0), output.copyOfRange(0, 5))

            output = ByteArray(26)
            decoder = MockDecoder(scanlineNumber = 0, scanlineBytes = scanlineBytes)
            loadRect(
                1u,
                1u,
                2u,
                4u,
                output,
                2,
                decoder,
                scanlineBytes,
                ::seekScanline,
                ::readScanline,
            )
            assertContentEquals(byteArrayOf(6, 7, 11, 12, 16, 17, 21, 22, 0), output.copyOfRange(0, 9))
        }
    }

    @Test
    fun testLoadRectSingleScanline() {
        val data = ByteArray(25) { it.toByte() }
        val output = ByteArray(26)
        var seeks = 0
        val decoder = MockDecoder()

        fun seekScanline(d: ImageDecoder, n: ULong) {
            seeks += 1
            assertEquals(0uL, n)
            assertEquals(1, seeks)
        }

        fun readScanline(d: ImageDecoder, buf: ByteArray) {
            data.copyInto(buf)
        }

        loadRect(
            1u,
            1u,
            2u,
            4u,
            output,
            2,
            decoder,
            data.size,
            ::seekScanline,
            ::readScanline,
        )
        assertContentEquals(byteArrayOf(6, 7, 11, 12, 16, 17, 21, 22, 0), output.copyOfRange(0, 9))
    }
}
