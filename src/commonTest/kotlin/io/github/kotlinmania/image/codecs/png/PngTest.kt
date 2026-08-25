// port-lint: tests codecs/png.rs
package io.github.kotlinmania.image.codecs.png

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PngTest {

    @Test
    fun testPngSignature() {
        val expected = byteArrayOf(
            137.toByte(), 80, 78, 71, 13, 10, 26, 10,
        )
        assertContentEquals(expected, PNG_SIGNATURE)
    }

    @Test
    fun testEncodePngRgb8() {
        val out = BufferIoWrite()
        val encoder = PngEncoder(out)
        // 2x2 RGB image
        val data = byteArrayOf(
            255.toByte(), 0, 0,
            0, 255.toByte(), 0,
            0, 0, 255.toByte(),
            255.toByte(), 255.toByte(), 255.toByte(),
        )
        encoder.writeImage(data, 2u, 2u, ExtendedColorType.Rgb8)

        val bytes = out.toByteArray()
        assertTrue(bytes.size > 8)
        // Signature check
        assertContentEquals(PNG_SIGNATURE, bytes.copyOfRange(0, 8))
    }

    @Test
    fun testEncodePngL8() {
        val out = BufferIoWrite()
        val encoder = PngEncoder(out)
        // 2x2 L8 image
        val data = byteArrayOf(0, 128.toByte(), 200.toByte(), 255.toByte())
        encoder.writeImage(data, 2u, 2u, ExtendedColorType.L8)

        val bytes = out.toByteArray()
        assertTrue(bytes.size > 8)
        assertContentEquals(PNG_SIGNATURE, bytes.copyOfRange(0, 8))
    }
}
