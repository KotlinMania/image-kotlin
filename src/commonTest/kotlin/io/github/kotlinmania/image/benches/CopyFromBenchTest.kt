// port-lint: tests image/benches/copy_from.rs
package io.github.kotlinmania.image.benches

import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class CopyFromBenchTest {
    @Test
    fun benchCopyFrom() {
        val src = ImageBuffer.fromPixel(2048u, 2048u, Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()))
        val dst = ImageBuffer.fromPixel(2048u, 2048u, Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()))

        dst.copyFrom(src, 0u, 0u)
        assertEquals(2048u, dst.width())
        assertEquals(2048u, dst.height())
    }
}
