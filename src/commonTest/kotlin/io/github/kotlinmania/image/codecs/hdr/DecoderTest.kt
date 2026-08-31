// port-lint: tests codecs/hdr/decoder.rs
package io.github.kotlinmania.image.codecs.hdr

import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoRead
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DecoderTest {
    @Test
    fun splitAtFirstTest() {
        assertNull(splitAtFirst("", "="))
        assertNull(splitAtFirst("=", "="))
        assertNull(splitAtFirst("= ", "="))
        assertEquals(Pair(" ", " "), splitAtFirst(" = ", "="))
        assertEquals(Pair("EXPOSURE", " "), splitAtFirst("EXPOSURE= ", "="))
        assertEquals(Pair("EXPOSURE", " ="), splitAtFirst("EXPOSURE= =", "="))
        assertEquals(Pair("EXPOSURE", " ="), splitAtFirst("EXPOSURE== =", "=="))
        assertNull(splitAtFirst("EXPOSURE", ""))
    }

    @Test
    fun readLineU8Test() {
        val buf = "One\nTwo\nThree\nFour\n\n\n".encodeToByteArray()
        val input = BufferIoRead(buf)
        assertEquals("One", readLineU8(input)?.decodeToString())
        assertEquals("Two", readLineU8(input)?.decodeToString())
        assertEquals("Three", readLineU8(input)?.decodeToString())
        assertEquals("Four", readLineU8(input)?.decodeToString())
        assertEquals("", readLineU8(input)?.decodeToString())
        assertEquals("", readLineU8(input)?.decodeToString())
        assertNull(readLineU8(input))
    }

    @Test
    fun dimensionOverflow() {
        val data = "#?RADIANCE\nFORMAT=32-bit_rle_rgbe\n\n -Y 4294967295 +X 4294967295".encodeToByteArray()

        assertFailsWith<ImageError> {
            HdrDecoder.new(BufferIoRead(data))
        }
        assertFailsWith<ImageError> {
            HdrDecoder.newNonstrict(BufferIoRead(data))
        }
    }
}
