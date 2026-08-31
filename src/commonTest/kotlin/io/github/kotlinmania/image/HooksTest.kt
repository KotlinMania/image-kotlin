// port-lint: tests hooks.rs
package io.github.kotlinmania.image

import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.SeekFrom
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HooksTest {
    private class DummyReadSeek(
        val buffer: BufferIoRead,
    ) : ReadSeek {
        override fun read(buffer: ByteArray, offset: Int, count: Int): Int =
            this.buffer.read(buffer, offset, count)

        override fun seek(pos: SeekFrom): Long =
            this.buffer.seek(pos)
    }

    private class DummyDecoder : ImageDecoder {
        override fun dimensions(): Pair<UInt, UInt> = Pair(1u, 1u)

        override fun colorType(): ColorType = ColorType.Rgb8

        override fun readImage(buf: ByteArray) {
            // no-op
        }
    }

    @Test
    fun genericReaderOperations() {
        val data = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val readSeek = DummyReadSeek(BufferIoRead(data))
        val reader = GenericReader(readSeek)

        val firstTwo = ByteArray(2)
        reader.readExact(firstTwo)
        assertContentEquals(byteArrayOf(1, 2), firstTwo)

        val outList = mutableListOf<Byte>()
        val readCount = reader.readToEnd(outList)
        assertEquals(6, readCount)
        assertContentEquals(listOf<Byte>(3, 4, 5, 6, 7, 8), outList)

        reader.rewind()
        assertEquals(0L, reader.streamPosition())

        val stringBuf = StringBuilder()
        val textData = "hello\nworld\n".encodeToByteArray()
        val textReader = GenericReader(DummyReadSeek(BufferIoRead(textData)))
        textReader.readLine(stringBuf)
        assertEquals("hello\n", stringBuf.toString())
    }

    @Test
    fun registerAndQueryDecodingHook() {
        val ext = "custom_fmt_test"
        assertFalse(decodingHookRegistered(ext))

        val registered =
            registerDecodingHook(ext) {
                DummyDecoder()
            }
        assertTrue(registered)
        assertTrue(decodingHookRegistered(ext))

        val secondAttempt =
            registerDecodingHook(ext) {
                DummyDecoder()
            }
        assertFalse(secondAttempt)
    }

    @Test
    fun registerFormatDetectionHookAndGuess() {
        val ext = "custom_magic_fmt"
        val signature = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        registerFormatDetectionHook(ext, signature)

        val matchingData = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00)
        assertEquals(ext, guessFormatWithHooks(matchingData))

        val nonMatchingData = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
        assertNull(guessFormatWithHooks(nonMatchingData))
    }

    @Test
    fun registerFormatDetectionHookWithMask() {
        val ext = "custom_riff_webp"
        val signature =
            byteArrayOf(
                'r'.code.toByte(),
                'i'.code.toByte(),
                'f'.code.toByte(),
                'f'.code.toByte(),
                0,
                0,
                0,
                0,
                'w'.code.toByte(),
                'e'.code.toByte(),
                'b'.code.toByte(),
                'p'.code.toByte(),
            )
        val mask =
            byteArrayOf(
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0,
                0,
                0,
                0,
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
            )
        registerFormatDetectionHook(ext, signature, mask)

        val sampleWebp =
            byteArrayOf(
                'r'.code.toByte(),
                'i'.code.toByte(),
                'f'.code.toByte(),
                'f'.code.toByte(),
                0x24,
                0x00,
                0x00,
                0x00,
                'w'.code.toByte(),
                'e'.code.toByte(),
                'b'.code.toByte(),
                'p'.code.toByte(),
            )
        assertEquals(ext, guessFormatWithHooks(sampleWebp))
    }
}
