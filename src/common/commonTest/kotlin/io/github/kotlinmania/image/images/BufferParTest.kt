// port-lint: tests images/buffer_par.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferParTest {
    private fun testWidthHeight(width: UInt, height: UInt, len: Int) {
        val image = ImageBuffer.createRgb(width, height)

        assertEquals(len, image.parEnumeratePixelsMut().len())
        assertEquals(len, image.parEnumeratePixels().len())
        assertEquals(len, image.parPixelsMut().len())
        assertEquals(len, image.parPixels().len())
    }

    @Test
    fun zeroWidthZeroHeight() {
        testWidthHeight(0u, 0u, 0)
    }

    @Test
    fun zeroWidthNonzeroHeight() {
        testWidthHeight(0u, 2u, 0)
    }

    @Test
    fun nonzeroWidthZeroHeight() {
        testWidthHeight(2u, 0u, 0)
    }

    @Test
    fun iterParity() {
        val image1 =
            ImageBuffer.createRgb(17u, 29u) { x, y ->
                Rgb(
                    ((x + y * 98u + 0u * 27u) % 255u).toUByte(),
                    ((x + y * 98u + 1u * 27u) % 255u).toUByte(),
                    ((x + y * 98u + 2u * 27u) % 255u).toUByte(),
                )
            }
        val image2 = image1.clone()

        assertEquals(
            image1.enumeratePixelsMut(),
            image2.parEnumeratePixelsMut().toList(),
        )
        assertEquals(
            image1.enumeratePixels(),
            image2.parEnumeratePixels().toList(),
        )
        assertEquals(
            image1.pixelsMut(),
            image2.parPixelsMut().toList(),
        )
        assertEquals(
            image1.pixelsMut(),
            image2.parPixels().toList(),
        )
    }

    private fun pixelFunc(): Rgb<UByte> = Rgb(123u.toUByte(), 45u.toUByte(), 67u.toUByte())

    @Test
    fun creation() {
        val s = 16u
        val img = ImageBuffer.createRgb(s, s) { _, _ -> pixelFunc() }
        assertEquals((s * s * 3u).toInt(), img.asRaw().size)
    }

    @Test
    fun creationPar() {
        val s = 16u
        val img = ImageBuffer.createRgb(s, s) { _, _ -> pixelFunc() }
        assertEquals((s * s * 3u).toInt(), img.asRaw().size)
    }
}
