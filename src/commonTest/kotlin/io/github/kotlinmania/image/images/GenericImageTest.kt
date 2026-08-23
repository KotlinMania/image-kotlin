// port-lint: tests images/generic_image.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.math.Rect
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenericImageTest {
    @Test
    fun testImageAlphaBlending() {
        val target = ImageBuffer.createRgba(1u, 1u)
        target.putPixel(0u, 0u, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()))
        assertEquals(Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()), target.getPixel(0u, 0u))

        target.blendPixel(0u, 0u, Rgba(0u.toUByte(), 255u.toUByte(), 0u.toUByte(), 255u.toUByte()))
        assertEquals(Rgba(0u.toUByte(), 255u.toUByte(), 0u.toUByte(), 255u.toUByte()), target.getPixel(0u, 0u))

        // Blending an alpha channel onto a solid background
        target.blendPixel(0u, 0u, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 127u.toUByte()))
        val blended = target.getPixel(0u, 0u)
        assertEquals(127u.toUByte(), blended.r)
        assertTrue(blended.g == 127u.toUByte() || blended.g == 128u.toUByte())
        assertEquals(0u.toUByte(), blended.b)
        assertEquals(255u.toUByte(), blended.a)

        // Blending two alpha channels
        target.putPixel(0u, 0u, Rgba(0u.toUByte(), 255u.toUByte(), 0u.toUByte(), 127u.toUByte()))
        target.blendPixel(0u, 0u, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 127u.toUByte()))
        assertEquals(Rgba(169u.toUByte(), 85u.toUByte(), 0u.toUByte(), 190u.toUByte()), target.getPixel(0u, 0u))
    }

    @Test
    fun testInBounds() {
        val target = ImageBuffer.createRgba(2u, 2u)
        target.putPixel(0u, 0u, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()))

        assertTrue(target.inBounds(0u, 0u))
        assertTrue(target.inBounds(1u, 0u))
        assertTrue(target.inBounds(0u, 1u))
        assertTrue(target.inBounds(1u, 1u))

        assertFalse(target.inBounds(2u, 0u))
        assertFalse(target.inBounds(0u, 2u))
        assertFalse(target.inBounds(2u, 2u))
    }

    @Test
    fun testCanSubimageCloneNonmut() {
        val source = ImageBuffer.createRgba(3u, 3u)
        source.putPixel(1u, 1u, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()))

        val cloned = source.view(1u, 1u, 1u, 1u).toImage()
        assertEquals(source.getPixel(1u, 1u), cloned.getPixel(0u, 0u))
    }

    @Test
    fun testCanNestViews() {
        val source = ImageBuffer.createRgba(3u, 3u)
        for (y in 0u until 3u) {
            for (x in 0u until 3u) {
                source.putPixel(x, y, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()))
            }
        }

        val sub1 = source.subImage(0u, 0u, 2u, 2u)
        val sub2 = sub1.subImage(1u, 1u, 1u, 1u)
        sub2.putPixel(0u, 0u, Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()))

        assertEquals(Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()), source.getPixel(1u, 1u))

        val view1 = source.view(0u, 0u, 2u, 2u)
        assertEquals(source.getPixel(1u, 1u), view1.getPixel(1u, 1u))

        val view2 = view1.view(1u, 1u, 1u, 1u)
        assertEquals(source.getPixel(1u, 1u), view2.getPixel(0u, 0u))
    }

    @Test
    fun testViewOutOfBounds() {
        val source = ImageBuffer.createRgba(3u, 3u)
        assertFailsWith<IllegalArgumentException> {
            source.view(1u, 1u, 3u, 3u)
        }
    }

    @Test
    fun testViewCoordinatesOutOfBounds() {
        val source = ImageBuffer.createRgba(3u, 3u)
        assertFailsWith<IllegalArgumentException> {
            source.view(3u, 3u, 3u, 3u)
        }
    }

    @Test
    fun testViewWidthOutOfBounds() {
        val source = ImageBuffer.createRgba(3u, 3u)
        assertFailsWith<IllegalArgumentException> {
            source.view(1u, 1u, 3u, 2u)
        }
    }

    @Test
    fun testViewHeightOutOfBounds() {
        val source = ImageBuffer.createRgba(3u, 3u)
        assertFailsWith<IllegalArgumentException> {
            source.view(1u, 1u, 2u, 3u)
        }
    }

    @Test
    fun testViewXOutOfBounds() {
        val source = ImageBuffer.createRgba(3u, 3u)
        assertFailsWith<IllegalArgumentException> {
            source.view(3u, 1u, 3u, 3u)
        }
    }

    @Test
    fun testViewYOutOfBounds() {
        val source = ImageBuffer.createRgba(3u, 3u)
        assertFailsWith<IllegalArgumentException> {
            source.view(1u, 3u, 3u, 3u)
        }
    }

    @Test
    fun testViewInBounds() {
        val source = ImageBuffer.createRgba(3u, 3u)
        source.view(0u, 0u, 3u, 3u)
        source.view(1u, 1u, 2u, 2u)
        source.view(2u, 2u, 0u, 0u)
    }

    @Test
    fun testCopySubImage() {
        val source = ImageBuffer.createRgba(3u, 3u)
        val view = source.view(0u, 0u, 3u, 3u)
        view.toImage()
    }

    @Test
    fun testGenericImageCopyWithinOob() {
        val image = ImageBuffer.createGray(4u, 4u, ByteArray(16))!!
        assertFalse(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(0u, 0u, 5u, 4u), 0u, 0u))
        assertFalse(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(0u, 0u, 4u, 5u), 0u, 0u))
        assertFalse(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(1u, 0u, 4u, 4u), 0u, 0u))
        assertFalse(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(0u, 0u, 4u, 4u), 1u, 0u))
        assertFalse(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(0u, 1u, 4u, 4u), 0u, 0u))
        assertFalse(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(0u, 0u, 4u, 4u), 0u, 1u))
        assertFalse(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(1u, 1u, 4u, 4u), 0u, 0u))
    }

    @Test
    fun testGenericImageCopyWithinTl() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val expected = byteArrayOf(0, 1, 2, 3, 4, 0, 1, 2, 8, 4, 5, 6, 12, 8, 9, 10)
        val image = ImageBuffer.createGray(4u, 4u, data)!!
        assertTrue(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(0u, 0u, 3u, 3u), 1u, 1u))
        assertContentEquals(expected, image.intoRaw())
    }

    @Test
    fun testGenericImageCopyWithinTr() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val expected = byteArrayOf(0, 1, 2, 3, 1, 2, 3, 7, 5, 6, 7, 11, 9, 10, 11, 15)
        val image = ImageBuffer.createGray(4u, 4u, data)!!
        assertTrue(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(1u, 0u, 3u, 3u), 0u, 1u))
        assertContentEquals(expected, image.intoRaw())
    }

    @Test
    fun testGenericImageCopyWithinBl() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val expected = byteArrayOf(0, 4, 5, 6, 4, 8, 9, 10, 8, 12, 13, 14, 12, 13, 14, 15)
        val image = ImageBuffer.createGray(4u, 4u, data)!!
        assertTrue(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(0u, 1u, 3u, 3u), 1u, 0u))
        assertContentEquals(expected, image.intoRaw())
    }

    @Test
    fun testGenericImageCopyWithinBr() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val expected = byteArrayOf(5, 6, 7, 3, 9, 10, 11, 7, 13, 14, 15, 11, 12, 13, 14, 15)
        val image = ImageBuffer.createGray(4u, 4u, data)!!
        assertTrue(image.subImage(0u, 0u, 4u, 4u).copyWithin(Rect(1u, 1u, 3u, 3u), 0u, 0u))
        assertContentEquals(expected, image.intoRaw())
    }
}
