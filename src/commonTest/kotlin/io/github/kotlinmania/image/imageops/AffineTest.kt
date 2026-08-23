// port-lint: source imageops/affine.rs
package io.github.kotlinmania.image.imageops

import kotlin.test.Test
import kotlin.test.assertEquals

class AffineTest {
    @Test
    fun testRotate90() {
        // 2x3 image, 1 channel:
        // [1, 2]
        // [3, 4]
        // [5, 6]
        // Rotated 90 deg clockwise -> 3x2 image:
        // [5, 3, 1]
        // [6, 4, 2]
        val img = byteArrayOf(1, 2, 3, 4, 5, 6)
        val rot = rotate90(img, 2, 3, 1)
        val expected = byteArrayOf(5, 3, 1, 6, 4, 2)
        assertEquals(expected.toList(), rot.toList())
    }

    @Test
    fun testRotate180() {
        val img = byteArrayOf(1, 2, 3, 4)
        val rot = rotate180(img, 2, 2, 1)
        val expected = byteArrayOf(4, 3, 2, 1)
        assertEquals(expected.toList(), rot.toList())
    }

    @Test
    fun testFlipHorizontal() {
        val img = byteArrayOf(1, 2, 3, 4)
        val flipped = flipHorizontal(img, 2, 2, 1)
        val expected = byteArrayOf(2, 1, 4, 3)
        assertEquals(expected.toList(), flipped.toList())
    }

    @Test
    fun testFlipVertical() {
        val img = byteArrayOf(1, 2, 3, 4)
        val flipped = flipVertical(img, 2, 2, 1)
        val expected = byteArrayOf(3, 4, 1, 2)
        assertEquals(expected.toList(), flipped.toList())
    }
}
