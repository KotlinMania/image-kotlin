// port-lint: tests imageops/sample.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SampleTest {
    @Test
    fun testResizeNearest() {
        val src = byteArrayOf(10, 20, 30, 40)
        val dst = resize(src, 2, 2, 4, 4, 1, FilterType.Nearest)
        assertEquals(16, dst.size)
        assertEquals(10.toByte(), dst[0])
        assertEquals(40.toByte(), dst[15])
    }

    @Test
    fun testResizeTriangle() {
        val src = byteArrayOf(0, 100, 100, 0)
        val dst = resize(src, 2, 2, 4, 4, 1, FilterType.Triangle)
        assertEquals(16, dst.size)
        for (b in dst) {
            val v = b.toInt() and 0xFF
            assertTrue(v in 0..100)
        }
    }

    @Test
    fun testThumbnail() {
        val src = ByteArray(100 * 200 * 3) { 128.toByte() }
        val thumb = thumbnail(src, 100, 200, 50, 50, 3)
        // 100x200 scaled into 50x50 maintains 1:2 aspect ratio -> 25x50
        assertEquals(25 * 50 * 3, thumb.size)
    }

    @Test
    fun testSampleBilinearCorrectness() {
        val img = ImageBuffer.createRgba(2u, 2u)
        img.putPixel(0u, 0u, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()))
        img.putPixel(0u, 1u, Rgba(0u.toUByte(), 255u.toUByte(), 0u.toUByte(), 0u.toUByte()))
        img.putPixel(1u, 0u, Rgba(0u.toUByte(), 0u.toUByte(), 255u.toUByte(), 0u.toUByte()))
        img.putPixel(1u, 1u, Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()))

        assertEquals(
            Rgba(64u.toUByte(), 64u.toUByte(), 64u.toUByte(), 64u.toUByte()),
            sampleBilinear(img, 0.5f, 0.5f),
        )
        assertEquals(
            Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()),
            sampleBilinear(img, 0.0f, 0.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 255u.toUByte(), 0u.toUByte(), 0u.toUByte()),
            sampleBilinear(img, 0.0f, 1.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 255u.toUByte(), 0u.toUByte()),
            sampleBilinear(img, 1.0f, 0.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()),
            sampleBilinear(img, 1.0f, 1.0f),
        )

        assertEquals(
            Rgba(128u.toUByte(), 0u.toUByte(), 128u.toUByte(), 0u.toUByte()),
            sampleBilinear(img, 0.5f, 0.0f),
        )
        assertEquals(
            Rgba(128u.toUByte(), 128u.toUByte(), 0u.toUByte(), 0u.toUByte()),
            sampleBilinear(img, 0.0f, 0.5f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 128u.toUByte(), 0u.toUByte(), 128u.toUByte()),
            sampleBilinear(img, 0.5f, 1.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 128u.toUByte(), 128u.toUByte()),
            sampleBilinear(img, 1.0f, 0.5f),
        )
    }

    @Test
    fun testSampleNearestCorrectness() {
        val img = ImageBuffer.createRgba(2u, 2u)
        img.putPixel(0u, 0u, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()))
        img.putPixel(0u, 1u, Rgba(0u.toUByte(), 255u.toUByte(), 0u.toUByte(), 0u.toUByte()))
        img.putPixel(1u, 0u, Rgba(0u.toUByte(), 0u.toUByte(), 255u.toUByte(), 0u.toUByte()))
        img.putPixel(1u, 1u, Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()))

        assertEquals(
            Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()),
            sampleNearest(img, 0.0f, 0.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 255u.toUByte(), 0u.toUByte(), 0u.toUByte()),
            sampleNearest(img, 0.0f, 1.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 255u.toUByte(), 0u.toUByte()),
            sampleNearest(img, 1.0f, 0.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()),
            sampleNearest(img, 1.0f, 1.0f),
        )

        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()),
            sampleNearest(img, 0.5f, 0.5f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 255u.toUByte(), 0u.toUByte()),
            sampleNearest(img, 0.5f, 0.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 255u.toUByte(), 0u.toUByte(), 0u.toUByte()),
            sampleNearest(img, 0.0f, 0.5f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()),
            sampleNearest(img, 0.5f, 1.0f),
        )
        assertEquals(
            Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()),
            sampleNearest(img, 1.0f, 0.5f),
        )
    }

    @Test
    fun testOutOfBoundsSampling() {
        val img = ImageBuffer.createRgba(2u, 2u)
        assertNull(sampleBilinear(img, 1.2f, 0.5f))
        assertNull(sampleBilinear(img, 0.5f, 1.2f))
        assertNull(sampleBilinear(img, -0.1f, 0.2f))
        assertNull(sampleNearest(img, 1.2f, 0.5f))
        assertNull(sampleNearest(img, -0.1f, 0.2f))
    }

    @Test
    fun testGaussianBlurParameters() {
        val p3 = GaussianBlurParameters.SMOOTHING_3
        assertEquals(3u, p3.xAxisKernelSize)
        assertEquals(3u, p3.yAxisKernelSize)

        val fromSigma = GaussianBlurParameters.newFromSigma(1.5f)
        assertTrue(fromSigma.xAxisKernelSize >= 3u)
        assertTrue(fromSigma.xAxisKernelSize % 2u != 0u)

        val fromRadius = GaussianBlurParameters.newFromRadius(2.0f)
        assertEquals(5u, fromRadius.xAxisKernelSize)

        val kernel1d = getGaussianKernel1d(5, 1.0f)
        assertEquals(5, kernel1d.size)
        var sum = 0.0f
        for (v in kernel1d) sum += v
        assertTrue(kotlin.math.abs(sum - 1.0f) < 1e-4f)
    }

    @Test
    fun testIssue186() {
        val img = ByteArray(100 * 100 * 3)
        val resized = resize(img, 100, 100, 50, 50, 3, FilterType.Lanczos3)
        assertEquals(50 * 50 * 3, resized.size)
    }

    @Test
    fun testBug1600() {
        val img = ByteArray(629 * 627 * 4) { 255.toByte() }
        val res = resize(img, 629, 627, 22, 22, 4, FilterType.Lanczos3)
        assertTrue(res.any { it != 0.toByte() })
    }

    @Test
    fun testIssue2340() {
        val empty = ByteArray(0)
        val res = resize(empty, 0, 0, 1, 1, 1, FilterType.Lanczos3)
        assertEquals(0, res.size)
    }

    @Test
    fun testFilter3x3() {
        val img = ByteArray(9) { 10.toByte() }
        val kernel =
            floatArrayOf(
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
            )
        val filtered = filter3x3(img, 3, 3, 1, kernel)
        assertEquals(9, filtered.size)
    }

    @Test
    fun testUnsharpen() {
        val img = ByteArray(16) { 50.toByte() }
        val sharpened = unsharpen(img, 4, 4, 1, 1.0f, 5)
        assertEquals(16, sharpened.size)
    }

    @Test
    fun testGaussianBlurDynImage() {
        val rgb =
            ImageBuffer.createRgb(4u, 4u) { x, y ->
                Rgb((x * 40u).toUByte(), (y * 40u).toUByte(), 128u.toUByte())
            }
        val dyn =
            io.github.kotlinmania.image.images.DynamicImage
                .ImageRgb8(rgb)
        val blurred = gaussianBlurDynImage(dyn, GaussianBlurParameters.SMOOTHING_3)
        assertEquals(4u, blurred.width())
        assertEquals(4u, blurred.height())
        assertTrue(blurred.asBytes().isNotEmpty())
    }
}
