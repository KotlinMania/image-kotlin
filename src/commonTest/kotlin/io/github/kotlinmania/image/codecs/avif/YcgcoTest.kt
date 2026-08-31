// port-lint: tests image/src/codecs/avif/ycgco.rs
package io.github.kotlinmania.image.codecs.avif

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YcgcoTest {
    @Test
    fun testYcgco444FullRange() {
        val width = 2
        val height = 2
        val yPlane = byteArrayOf(128.toByte(), 128.toByte(), 128.toByte(), 128.toByte())
        val uPlane = byteArrayOf(128.toByte(), 128.toByte(), 128.toByte(), 128.toByte()) // Cg = 0
        val vPlane = byteArrayOf(128.toByte(), 128.toByte(), 128.toByte(), 128.toByte()) // Co = 0

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
        ycgco444ToRgba8(image, rgba, YuvIntensityRange.Pc)

        for (i in 0 until 4) {
            assertEquals(128.toByte(), rgba[i * 4])
            assertEquals(128.toByte(), rgba[i * 4 + 1])
            assertEquals(128.toByte(), rgba[i * 4 + 2])
            assertEquals(255.toByte(), rgba[i * 4 + 3])
        }
    }

    @Test
    fun testYcgco420OddDimensions() {
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
        ycgco420ToRgba8(image, rgba, YuvIntensityRange.Pc)

        for (i in 0 until 9) {
            assertEquals(255.toByte(), rgba[i * 4 + 3])
        }
    }

    @Test
    fun testYcgco422LimitedRange() {
        val width = 4
        val height = 2
        val yPlane = ByteArray(8) { 128.toByte() }
        val chromaStride = 2
        val uPlane = ByteArray(chromaStride * height) { 128.toByte() }
        val vPlane = ByteArray(chromaStride * height) { 128.toByte() }

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
        ycgco422ToRgba8(image, rgba, YuvIntensityRange.Tv)

        for (i in 0 until 8) {
            assertEquals(255.toByte(), rgba[i * 4 + 3])
            assertTrue((rgba[i * 4].toInt() and 0xFF) in 0..255)
        }
    }
}
