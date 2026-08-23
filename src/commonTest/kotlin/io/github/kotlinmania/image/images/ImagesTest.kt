// port-lint: tests images/flat.rs
package io.github.kotlinmania.image.images

import kotlin.test.Test
import kotlin.test.assertEquals

class ImagesTest {
    @Test
    fun testSampleLayoutRowMajor() {
        val layout = SampleLayout.rowMajorPacked(3u, 10u, 20u)
        assertEquals(3u, layout.channels)
        assertEquals(1, layout.channelStride)
        assertEquals(3, layout.widthStride)
        assertEquals(30, layout.heightStride)
        assertEquals(600L, layout.totalSamples())
    }

    @Test
    fun testSubImageExtraction() {
        val parentW = 4u
        val parentH = 4u
        val channels = 1
        val data = ByteArray(16) { it.toByte() }
        val sub = SubImage(data, parentW, parentH, channels, 1u, 1u, 2u, 2u)
        assertEquals(Pair(1u, 1u), sub.offsets())
        assertEquals(Pair(2u, 2u), sub.dimensions())

        val p00 = sub.getPixel(0u, 0u)
        assertEquals(5.toByte(), p00[0])

        val subData = sub.toByteArray()
        assertEquals(4, subData.size)
        assertEquals(5.toByte(), subData[0])
        assertEquals(6.toByte(), subData[1])
        assertEquals(9.toByte(), subData[2])
        assertEquals(10.toByte(), subData[3])
    }
}
