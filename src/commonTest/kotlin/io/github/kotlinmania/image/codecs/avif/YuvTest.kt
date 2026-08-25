// port-lint: tests codecs/avif/yuv.rs
package io.github.kotlinmania.image.codecs.avif

import io.github.kotlinmania.image.ImageError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class YuvTest {
    @Test
    fun testYuvChromaRanges() {
        val tvRange8 = YuvIntensityRange.Tv.getYuvRange(8u)
        assertEquals(16u, tvRange8.biasY)
        assertEquals(128u, tvRange8.biasUv)
        assertEquals(219u, tvRange8.rangeY)
        assertEquals(224u, tvRange8.rangeUv)

        val pcRange8 = YuvIntensityRange.Pc.getYuvRange(8u)
        assertEquals(0u, pcRange8.biasY)
        assertEquals(128u, pcRange8.biasUv)
        assertEquals(255u, pcRange8.rangeY)
        assertEquals(255u, pcRange8.rangeUv)
    }

    @Test
    fun testStandardMatrixCoefficients() {
        val (kr601, kb601) = YuvStandardMatrix.Bt601.getKrKb()
        assertEquals(0.299f, kr601)
        assertEquals(0.114f, kb601)

        val (kr709, kb709) = YuvStandardMatrix.Bt709.getKrKb()
        assertEquals(0.2126f, kr709)
        assertEquals(0.0722f, kb709)

        val (kr2020, kb2020) = YuvStandardMatrix.Bt2020.getKrKb()
        assertEquals(0.2627f, kr2020)
        assertEquals(0.0593f, kb2020)
    }

    @Test
    fun testPreconditionFailures() {
        val smallYPlane = ByteArray(10)
        assertFailsWith<ImageError.Decoding> {
            checkYuvPlanePreconditions(smallYPlane, PlaneDefinition.Y, 4, 4)
        }

        val smallRgb = ByteArray(10)
        assertFailsWith<ImageError.Decoding> {
            checkRgbPreconditions(smallRgb, 16, 4)
        }
    }

    @Test
    fun testYuv400ToRgba8PcRange() {
        val width = 2
        val height = 2
        val yPlane = byteArrayOf(10, 20, 30, 40)
        val dummy = ByteArray(0)
        val image =
            YuvPlanarImage(
                yPlane = yPlane,
                yStride = width,
                uPlane = dummy,
                uStride = 0,
                vPlane = dummy,
                vStride = 0,
                width = width,
                height = height,
            )

        val rgba = ByteArray(width * height * 4)
        yuv400ToRgba8(image, rgba, YuvIntensityRange.Pc, YuvStandardMatrix.Bt709)

        for (i in 0 until 4) {
            val expected = yPlane[i]
            assertEquals(expected, rgba[i * 4])
            assertEquals(expected, rgba[i * 4 + 1])
            assertEquals(expected, rgba[i * 4 + 2])
            assertEquals(255.toByte(), rgba[i * 4 + 3])
        }
    }

    @Test
    fun testYuv444ToRgba8() {
        val width = 2
        val height = 2
        val yPlane = byteArrayOf(128.toByte(), 128.toByte(), 128.toByte(), 128.toByte())
        val uPlane = byteArrayOf(128.toByte(), 128.toByte(), 128.toByte(), 128.toByte())
        val vPlane = byteArrayOf(128.toByte(), 128.toByte(), 128.toByte(), 128.toByte())

        val image =
            YuvPlanarImage(
                yPlane = yPlane,
                yStride = width,
                uPlane = uPlane,
                uStride = width,
                vPlane = vPlane,
                vStride = width,
                width = width,
                height = height,
            )

        val rgba = ByteArray(width * height * 4)
        yuv444ToRgba8(image, rgba, YuvIntensityRange.Pc, YuvStandardMatrix.Bt709)

        // Neutral chroma with mid luma produces gray RGB
        for (i in 0 until 4) {
            assertTrue((rgba[i * 4].toInt() and 0xFF) in 126..130)
            assertTrue((rgba[i * 4 + 1].toInt() and 0xFF) in 126..130)
            assertTrue((rgba[i * 4 + 2].toInt() and 0xFF) in 126..130)
            assertEquals(255.toByte(), rgba[i * 4 + 3])
        }
    }

    @Test
    fun testYuv420ToRgba8OddDimensions() {
        val width = 3
        val height = 3
        val yPlane = ByteArray(9) { 128.toByte() }
        val chromaStride = 2
        val chromaHeight = 2
        val uPlane = ByteArray(chromaStride * chromaHeight) { 128.toByte() }
        val vPlane = ByteArray(chromaStride * chromaHeight) { 128.toByte() }

        val image =
            YuvPlanarImage(
                yPlane = yPlane,
                yStride = width,
                uPlane = uPlane,
                uStride = chromaStride,
                vPlane = vPlane,
                vStride = chromaStride,
                width = width,
                height = height,
            )

        val rgba = ByteArray(width * height * 4)
        yuv420ToRgba8(image, rgba, YuvIntensityRange.Pc, YuvStandardMatrix.Bt709)

        for (i in 0 until 9) {
            assertEquals(255.toByte(), rgba[i * 4 + 3])
        }
    }

    @Test
    fun testGbrToRgba8Pc() {
        val width = 2
        val height = 2
        // GBR planar: yPlane = G, uPlane = B, vPlane = R
        val gPlane = byteArrayOf(10, 20, 30, 40)
        val bPlane = byteArrayOf(50, 60, 70, 80)
        val rPlane = byteArrayOf(90, 100, 110, 120)

        val image =
            YuvPlanarImage(
                yPlane = gPlane,
                yStride = width,
                uPlane = bPlane,
                uStride = width,
                vPlane = rPlane,
                vStride = width,
                width = width,
                height = height,
            )

        val rgba = ByteArray(width * height * 4)
        gbrToRgba8(image, rgba, YuvIntensityRange.Pc)

        for (i in 0 until 4) {
            assertEquals(rPlane[i], rgba[i * 4])
            assertEquals(gPlane[i], rgba[i * 4 + 1])
            assertEquals(bPlane[i], rgba[i * 4 + 2])
            assertEquals(255.toByte(), rgba[i * 4 + 3])
        }
    }
}
