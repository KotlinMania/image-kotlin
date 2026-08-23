// port-lint: tests imageops/sample.rs
package io.github.kotlinmania.image.imageops

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
