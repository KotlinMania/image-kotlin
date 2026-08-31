// port-lint: tests image/src/images/flat.rs
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
        val data = ByteArray(16) { it.toByte() }
        val img = ImageBuffer.createGray(4u, 4u, data)!!
        val sub = img.subImage(1u, 1u, 2u, 2u)
        assertEquals(Pair(1u, 1u), sub.offsets())
        assertEquals(Pair(2u, 2u), sub.dimensions())

        val p00 = sub.getPixel(0u, 0u)
        assertEquals(5u.toUByte(), p00.l)

        val subImg = sub.toImage() as ImageBuffer<*, *>
        assertEquals(4, subImg.asRaw().size)
        val raw = subImg.asRaw()
        assertEquals(5.toByte(), raw[0])
        assertEquals(6.toByte(), raw[1])
        assertEquals(9.toByte(), raw[2])
        assertEquals(10.toByte(), raw[3])
    }
}
