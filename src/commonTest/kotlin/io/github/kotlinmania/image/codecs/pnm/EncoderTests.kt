// port-lint: tests image/src/codecs/pnm/encoder.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EncoderTests {
    @Test
    fun pbmAllowsBlack() {
        val data = ByteArray(50 * 50) { 0 }
        val writer = BufferIoWrite()
        val encoder = PnmEncoder(writer).withSubtype(PnmSubtype.Bitmap(SampleEncoding.Ascii))
        encoder.encode(data, 50u, 50u, ExtendedColorType.L8)
        val written = writer.toByteArray()
        assertTrue(written.isNotEmpty())
    }

    @Test
    fun pbmAllowsWhite() {
        val data = ByteArray(50 * 50) { 1 }
        val writer = BufferIoWrite()
        val encoder = PnmEncoder(writer).withSubtype(PnmSubtype.Bitmap(SampleEncoding.Ascii))
        encoder.encode(data, 50u, 50u, ExtendedColorType.L8)
        val written = writer.toByteArray()
        assertTrue(written.isNotEmpty())
    }

    @Test
    fun pbmVerifiesPixels() {
        val data = ByteArray(50 * 50) { 255.toByte() }
        val writer = BufferIoWrite()
        val encoder = PnmEncoder(writer).withSubtype(PnmSubtype.Bitmap(SampleEncoding.Ascii))
        assertFailsWith<ImageError> {
            encoder.encode(data, 50u, 50u, ExtendedColorType.L8)
        }
    }
}
