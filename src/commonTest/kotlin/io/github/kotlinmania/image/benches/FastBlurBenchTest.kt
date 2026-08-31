// port-lint: tests image/benches/fast_blur.rs
package io.github.kotlinmania.image.benches

import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.imageops.fastBlur
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class FastBlurBenchTest {
    @Test
    fun benchFastBlur() {
        val src = ImageBuffer.fromPixel(1024u, 768u, Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()))
        val result = fastBlur(src.asRaw(), 1024, 768, 3, 50.0f)
        assertEquals(1024 * 768 * 3, result.size)
    }
}
