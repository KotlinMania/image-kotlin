// port-lint: tests blur.rs
package io.github.kotlinmania.image.benches

import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.imageops.GaussianBlurParameters
import io.github.kotlinmania.image.imageops.blurAdvanced
import io.github.kotlinmania.image.imageops.fastBlur
import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class BlurBenchTest {
    @Test
    fun benchFastBlur() {
        val width = 128
        val height = 96
        val src = ImageBuffer.fromPixel(width.toUInt(), height.toUInt(), Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()))
        val dynamic = DynamicImage.ImageRgb8(ImageBuffer.fromPixel(width.toUInt(), height.toUInt(), Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte())))

        val b1 = fastBlur(src.asRaw(), width, height, 3, 3.0f)
        assertEquals(width * height * 3, b1.size)

        val b2 = fastBlur(src.asRaw(), width, height, 3, 7.0f)
        assertEquals(width * height * 3, b2.size)

        val b3 = fastBlur(src.asRaw(), width, height, 3, 50.0f)
        assertEquals(width * height * 3, b3.size)

        val g1 = blurAdvanced(src.asRaw(), width, height, 3, GaussianBlurParameters.newFromSigma(3.0f))
        assertEquals(width * height * 3, g1.size)

        val g2 = blurAdvanced(src.asRaw(), width, height, 3, GaussianBlurParameters.newFromSigma(7.0f))
        assertEquals(width * height * 3, g2.size)

        val g3 = blurAdvanced(src.asRaw(), width, height, 3, GaussianBlurParameters.newFromSigma(50.0f))
        assertEquals(width * height * 3, g3.size)

        val dg1 = dynamic.blurAdvanced(GaussianBlurParameters.newFromSigma(3.0f))
        assertEquals(width.toUInt(), dg1.width())

        val dg2 = dynamic.blurAdvanced(GaussianBlurParameters.newFromSigma(7.0f))
        assertEquals(width.toUInt(), dg2.width())

        val dg3 = dynamic.blurAdvanced(GaussianBlurParameters.newFromSigma(50.0f))
        assertEquals(width.toUInt(), dg3.width())
    }
}
