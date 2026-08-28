// port-lint: tests imageops/affine.rs
package io.github.kotlinmania.image.imageops

import kotlin.test.Test
import kotlin.test.assertEquals

class AffineTest {
    @Test
    fun testRotate90() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(10, 0, 11, 1, 12, 2)
        val actual = rotate90(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())
    }

    @Test
    fun testRotate180() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(12, 11, 10, 2, 1, 0)
        val actual = rotate180(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())
    }

    @Test
    fun testRotate270() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(2, 12, 1, 11, 0, 10)
        val actual = rotate270(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())
    }

    @Test
    fun testRotate180InPlace() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(12, 11, 10, 2, 1, 0)
        rotate180InPlace(image, 3, 2, 1)
        assertEquals(expected.toList(), image.toList())
    }

    @Test
    fun testFlipHorizontal() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(2, 1, 0, 12, 11, 10)
        val actual = flipHorizontal(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())
    }

    @Test
    fun testFlipVertical() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(10, 11, 12, 0, 1, 2)
        val actual = flipVertical(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())
    }

    @Test
    fun testFlipHorizontalInPlace() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(2, 1, 0, 12, 11, 10)
        flipHorizontalInPlace(image, 3, 2, 1)
        assertEquals(expected.toList(), image.toList())
    }

    @Test
    fun testFlipVerticalInPlace() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(10, 11, 12, 0, 1, 2)
        flipVerticalInPlace(image, 3, 2, 1)
        assertEquals(expected.toList(), image.toList())
    }

    private fun <P> pixelDiffs(
        left: List<Triple<UInt, UInt, P>>,
        right: List<Triple<UInt, UInt, P>>,
    ): List<Pair<Triple<UInt, UInt, P>, Triple<UInt, UInt, P>>> {
        return left.zip(right).filter { (p, q) -> p != q }
    }
}
