// port-lint: tests images/sub_image.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.metadata.Cicp
import kotlin.test.Test
import kotlin.test.assertEquals

class SubImageTest {
    @Test
    fun preservesColorSpace() {
        val buffer = ImageBuffer.createRgba(16u, 16u)
        buffer.putPixel(0u, 0u, Rgba(0xFFu, 0u, 0u, 255u))
        buffer.setRgbPrimaries(Cicp.DISPLAY_P3.primaries)

        val view = buffer.view(0u, 0u, 16u, 16u)
        val result = view.bufferLike()

        assertEquals(buffer.colorSpace(), (result as? ImageBuffer<*, *>)?.colorSpace())
    }

    @Test
    fun deepPreservesColorSpace() {
        val buffer = ImageBuffer.createRgba(16u, 16u)
        buffer.putPixel(0u, 0u, Rgba(0xFFu, 0u, 0u, 255u))
        buffer.setRgbPrimaries(Cicp.DISPLAY_P3.primaries)

        val view = buffer.view(0u, 0u, 16u, 16u)
        val deepView = view.view(0u, 0u, 16u, 16u)
        val result = deepView.bufferLike()

        assertEquals(buffer.colorSpace(), (result as? ImageBuffer<*, *>)?.colorSpace())
    }
}
