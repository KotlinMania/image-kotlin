// port-lint: tests io/decoder.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecoderTest {
    private class OverflowDecoder : ImageDecoder {
        override fun colorType(): ColorType = ColorType.Rgb8

        override fun dimensions(): Pair<UInt, UInt> = Pair(0xFFFFFFFFu, 0xFFFFFFFFu)

        override fun readImage(buf: ByteArray) {
            throw UnsupportedOperationException("Mock decoder does not read pixels")
        }
    }

    @Test
    fun totalBytesOverflow() {
        val decoder = OverflowDecoder()
        assertEquals(ULong.MAX_VALUE, decoder.totalBytes())

        assertFailsWith<ImageError.Limits> {
            decoderToVec(decoder)
        }
    }
}
