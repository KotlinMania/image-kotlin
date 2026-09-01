// port-lint: tests codecs/webp/decoder.rs
package io.github.kotlinmania.image.codecs.webp

import io.github.kotlinmania.image.ImageError
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WebPDecoderTest {
    @Test
    fun addWithOverflowSize() {
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

    @Test
    fun testWebpInvalidHeader() {
        assertFailsWith<ImageError.Decoding> {
            WebPDecoder(byteArrayOf(0x00, 0x01, 0x02))
        }
    }
}
