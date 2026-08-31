// port-lint: tests image/src/images/flat.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.LumaA
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlatTest {
    @Test
    fun aliasingView() {
        val buffer =
            FlatSamples(
                samples = byteArrayOf(42),
                layout =
                    SampleLayout(
                        channels = 3u,
                        channelStride = 0,
                        width = 100u,
                        widthStride = 0,
                        height = 100u,
                        heightStride = 0,
                    ),
                colorHint = null,
            )

        val view = buffer.asViewRgb().getOrThrow()
        var count = 0
        val expected = Rgb(42u.toUByte(), 42u.toUByte(), 42u.toUByte())
        for (y in 0u until view.height()) {
            for (x in 0u until view.width()) {
                val pixel = view.getPixel(x, y)
                assertEquals(expected, pixel)
                count++
            }
        }
        assertEquals(100 * 100, count)
    }

    @Test
    fun mutableView() {
        val buffer =
            FlatSamples(
                samples = ByteArray(18),
                layout =
                    SampleLayout(
                        channels = 2u,
                        channelStride = 1,
                        width = 3u,
                        widthStride = 2,
                        height = 3u,
                        heightStride = 6,
                    ),
                colorHint = null,
            )

        val view = buffer.asViewMutLumaA().getOrThrow()
        assertEquals(Pair(3u, 3u), view.dimensions())
        for (i in 0 until 9) {
            view.putPixel((i % 3).toUInt(), (i / 3).toUInt(), LumaA((2 * i).toUByte(), (2 * i + 1).toUByte()))
        }

        for ((idx, sample) in buffer.samples.withIndex()) {
            assertEquals(idx, sample.toInt())
        }
    }

    @Test
    fun normalForms() {
        val pixelPacked =
            FlatSamples(
                samples = ByteArray(0),
                layout =
                    SampleLayout(
                        channels = 2u,
                        channelStride = 1,
                        width = 3u,
                        widthStride = 9,
                        height = 3u,
                        heightStride = 28,
                    ),
                colorHint = null,
            )
        assertTrue(pixelPacked.isNormal(NormalForm.PixelPacked))

        val imagePacked =
            FlatSamples(
                samples = ByteArray(0),
                layout =
                    SampleLayout(
                        channels = 2u,
                        channelStride = 8,
                        width = 4u,
                        widthStride = 1,
                        height = 2u,
                        heightStride = 4,
                    ),
                colorHint = null,
            )
        assertTrue(imagePacked.isNormal(NormalForm.ImagePacked))

        val rowMajorPacked =
            FlatSamples(
                samples = ByteArray(0),
                layout =
                    SampleLayout(
                        channels = 2u,
                        channelStride = 1,
                        width = 4u,
                        widthStride = 2,
                        height = 2u,
                        heightStride = 8,
                    ),
                colorHint = null,
            )
        assertTrue(rowMajorPacked.isNormal(NormalForm.RowMajorPacked))

        val columnMajorPacked =
            FlatSamples(
                samples = ByteArray(0),
                layout =
                    SampleLayout(
                        channels = 2u,
                        channelStride = 1,
                        width = 4u,
                        widthStride = 4,
                        height = 2u,
                        heightStride = 2,
                    ),
                colorHint = null,
            )
        assertTrue(columnMajorPacked.isNormal(NormalForm.ColumnMajorPacked))
    }

    @Test
    fun imageBufferConversion() {
        val expectedLayout =
            SampleLayout(
                channels = 2u,
                channelStride = 1,
                width = 4u,
                widthStride = 2,
                height = 2u,
                heightStride = 8,
            )

        val initial = ImageBuffer.createGrayAlpha(expectedLayout.width, expectedLayout.height, ByteArray(16))!!
        val buffer = initial.intoFlatSamples()

        assertEquals(expectedLayout, buffer.layout)

        val converted = buffer.tryIntoGrayAlphaImage().getOrThrow()
        assertEquals(expectedLayout.width, converted.width())
        assertEquals(expectedLayout.height, converted.height())
    }

    @Test
    fun monocolorFactoryAndBounds() {
        val flatRgb = FlatSamples.withMonocolor(Rgb(10u.toUByte(), 20u.toUByte(), 30u.toUByte()), 5u, 5u)
        assertEquals(3u.toUByte(), flatRgb.layout.channels)
        assertEquals(5u, flatRgb.layout.width)
        assertEquals(5u, flatRgb.layout.height)
        assertTrue(flatRgb.inBounds(0u.toUByte(), 0u, 0u))
        assertTrue(flatRgb.inBounds(2u.toUByte(), 4u, 4u))
        assertFalse(flatRgb.inBounds(3u.toUByte(), 0u, 0u))
        assertFalse(flatRgb.inBounds(0u.toUByte(), 5u, 0u))

        val flatRgba = FlatSamples.withMonocolor(Rgba(10u.toUByte(), 20u.toUByte(), 30u.toUByte(), 40u.toUByte()), 3u, 3u)
        assertEquals(4u.toUByte(), flatRgba.layout.channels)

        val flatLuma = FlatSamples.withMonocolor(Luma(128u.toUByte()), 2u, 2u)
        assertEquals(1u.toUByte(), flatLuma.layout.channels)

        val flatLumaA = FlatSamples.withMonocolor(LumaA(128u.toUByte(), 255u.toUByte()), 2u, 2u)
        assertEquals(2u.toUByte(), flatLumaA.layout.channels)
    }
}
