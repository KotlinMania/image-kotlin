// port-lint: tests imageops/affine.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.GrayImage
import io.github.kotlinmania.image.images.GenericImageView
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AffineTest {
    private fun <P> assertPixelsEq(actual: GenericImageView<P>, expected: GenericImageView<P>) {
        val actualDim = actual.dimensions()
        val expectedDim = expected.dimensions()
        assertEquals(expectedDim, actualDim, "dimensions do not match")
        val diffs = pixelDiffs(actual, expected)
        assertTrue(diffs.isEmpty(), "pixels do not match: $diffs")
    }

    @Test
    fun testRotate90() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(10, 0, 11, 1, 12, 2)
        val actual = rotate90(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())

        val imageBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(2u, 3u, byteArrayOf(10, 0, 11, 1, 12, 2))!!
        assertPixelsEq(rotate90(imageBuf), expectedBuf)
    }

    @Test
    fun testRotate180() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(12, 11, 10, 2, 1, 0)
        val actual = rotate180(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())

        val imageBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(12, 11, 10, 2, 1, 0))!!
        assertPixelsEq(rotate180(imageBuf), expectedBuf)
    }

    @Test
    fun testRotate270() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(2, 12, 1, 11, 0, 10)
        val actual = rotate270(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())

        val imageBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(2u, 3u, byteArrayOf(2, 12, 1, 11, 0, 10))!!
        assertPixelsEq(rotate270(imageBuf), expectedBuf)
    }

    @Test
    fun testRotate180InPlace() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(12, 11, 10, 2, 1, 0)
        rotate180InPlace(image, 3, 2, 1)
        assertEquals(expected.toList(), image.toList())

        val imageBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(12, 11, 10, 2, 1, 0))!!
        rotate180InPlace(imageBuf)
        assertPixelsEq(imageBuf, expectedBuf)
    }

    @Test
    fun testFlipHorizontal() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(2, 1, 0, 12, 11, 10)
        val actual = flipHorizontal(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())

        val imageBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(2, 1, 0, 12, 11, 10))!!
        assertPixelsEq(flipHorizontal(imageBuf), expectedBuf)
    }

    @Test
    fun testFlipVertical() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(10, 11, 12, 0, 1, 2)
        val actual = flipVertical(image, 3, 2, 1)
        assertEquals(expected.toList(), actual.toList())

        val imageBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(10, 11, 12, 0, 1, 2))!!
        assertPixelsEq(flipVertical(imageBuf), expectedBuf)
    }

    @Test
    fun testFlipHorizontalInPlace() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(2, 1, 0, 12, 11, 10)
        flipHorizontalInPlace(image, 3, 2, 1)
        assertEquals(expected.toList(), image.toList())

        val imageBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(2, 1, 0, 12, 11, 10))!!
        flipHorizontalInPlace(imageBuf)
        assertPixelsEq(imageBuf, expectedBuf)
    }

    @Test
    fun testFlipVerticalInPlace() {
        val image = byteArrayOf(0, 1, 2, 10, 11, 12)
        val expected = byteArrayOf(10, 11, 12, 0, 1, 2)
        flipVerticalInPlace(image, 3, 2, 1)
        assertEquals(expected.toList(), image.toList())

        val imageBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(0, 1, 2, 10, 11, 12))!!
        val expectedBuf: GrayImage =
            ImageBuffer.createGray(3u, 2u, byteArrayOf(10, 11, 12, 0, 1, 2))!!
        flipVerticalInPlace(imageBuf)
        assertPixelsEq(imageBuf, expectedBuf)
    }
}

