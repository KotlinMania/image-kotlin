// port-lint: tests codecs/hdr/encoder.rs
package io.github.kotlinmania.image.codecs.hdr

import io.github.kotlinmania.image.Rgb
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncoderTest {
    @Test
    fun toRgbe8Test() {
        val testCases = listOf(rgbe8(0, 0, 0, 0), rgbe8(1, 1, 128, 128))
        for (pix in testCases) {
            assertEquals(pix, toRgbe8(pix.toHdr()))
        }
        for (mc in 128 until 255) {
            var pix = rgbe8(mc, mc, mc, 100)
            assertEquals(pix, toRgbe8(pix.toHdr()))
            pix = rgbe8(mc, 0, mc, 130)
            assertEquals(pix, toRgbe8(pix.toHdr()))
            pix = rgbe8(0, 0, mc, 140)
            assertEquals(pix, toRgbe8(pix.toHdr()))
            pix = rgbe8(1, 0, mc, 150)
            assertEquals(pix, toRgbe8(pix.toHdr()))
            pix = rgbe8(1, mc, 10, 128)
            assertEquals(pix, toRgbe8(pix.toHdr()))
            for (c in 0 until 255) {
                pix = rgbe8(1, mc, c, if (c == 0) 1 else c)
                assertEquals(pix, toRgbe8(pix.toHdr()))
            }
        }

        fun relativeDist(a: Rgb<Float>, b: Rgb<Float>): Float {
            val maxDiff = maxOf(abs(a.r - b.r), maxOf(abs(a.g - b.g), abs(a.b - b.b)))
            val maxVal = maxOf(a.r, maxOf(a.g, maxOf(a.b, maxOf(b.r, maxOf(b.g, b.b)))))
            return if (maxVal == 0.0f) 0.0f else maxDiff / maxVal
        }
        val testValues =
            listOf(
                0.000001f,
                0.00002f,
                0.0003f,
                0.004f,
                0.05f,
                0.6f,
                7.0f,
                80.0f,
                900.0f,
                1000.0f,
                20000.0f,
                300000.0f,
            )
        for (r in testValues) {
            for (g in testValues) {
                for (b in testValues) {
                    val c1 = Rgb(r, g, b)
                    val c2 = toRgbe8(c1).toHdr()
                    val relDist = relativeDist(c1, c2)
                    assertTrue(
                        relDist <= 1.0f / 128.0f,
                        "Relative distance ($relDist) exceeds 1/128 for $c1 and $c2",
                    )
                }
            }
        }
    }

    @Test
    fun runIteratorTest() {
        val data0 = byteArrayOf()
        val runIter0 = RunIterator(data0)
        assertNull(runIter0.next())

        val data1 = byteArrayOf(5)
        val runIter1 = RunIterator(data1)
        assertEquals(RunOrNot.Norun(0, 1), runIter1.next())
        assertNull(runIter1.next())

        val data2 = byteArrayOf(1, 1)
        val runIter2 = RunIterator(data2)
        assertEquals(RunOrNot.Norun(0, 2), runIter2.next())
        assertNull(runIter2.next())

        val data3 = byteArrayOf(0, 0, 0)
        val runIter3 = RunIterator(data3)
        assertEquals(RunOrNot.Run(0u, 3), runIter3.next())
        assertNull(runIter3.next())

        val data4 = byteArrayOf(0, 0, 1, 1)
        val runIter4 = RunIterator(data4)
        assertEquals(RunOrNot.Norun(0, 2), runIter4.next())
        assertEquals(RunOrNot.Norun(2, 2), runIter4.next())
        assertNull(runIter4.next())

        val data5 = byteArrayOf(0, 0, 0, 1, 1)
        val runIter5 = RunIterator(data5)
        assertEquals(RunOrNot.Run(0u, 3), runIter5.next())
        assertEquals(RunOrNot.Norun(3, 2), runIter5.next())
        assertNull(runIter5.next())

        val data6 = byteArrayOf(1, 2, 2, 2)
        val runIter6 = RunIterator(data6)
        assertEquals(RunOrNot.Norun(0, 1), runIter6.next())
        assertEquals(RunOrNot.Run(2u, 3), runIter6.next())
        assertNull(runIter6.next())

        val data7 = byteArrayOf(1, 1, 2, 2, 2)
        val runIter7 = RunIterator(data7)
        assertEquals(RunOrNot.Norun(0, 2), runIter7.next())
        assertEquals(RunOrNot.Run(2u, 3), runIter7.next())
        assertNull(runIter7.next())

        val data8 = ByteArray(128) { 2 }
        val runIter8 = RunIterator(data8)
        assertEquals(RunOrNot.Run(2u, 127), runIter8.next())
        assertEquals(RunOrNot.Norun(127, 1), runIter8.next())
        assertNull(runIter8.next())

        val data9 = ByteArray(129) { 2 }
        val runIter9 = RunIterator(data9)
        assertEquals(RunOrNot.Run(2u, 127), runIter9.next())
        assertEquals(RunOrNot.Norun(127, 2), runIter9.next())
        assertNull(runIter9.next())

        val data10 = ByteArray(130) { 2 }
        val runIter10 = RunIterator(data10)
        assertEquals(RunOrNot.Run(2u, 127), runIter10.next())
        assertEquals(RunOrNot.Run(2u, 3), runIter10.next())
        assertNull(runIter10.next())
    }

    @Test
    fun norunCombineTest() {
        val v0 = byteArrayOf()
        val rsi0 = NorunCombineIterator(v0)
        assertNull(rsi0.next())

        val v1 = byteArrayOf(1)
        val rsi1 = NorunCombineIterator(v1)
        assertEquals(RunOrNot.Norun(0, 1), rsi1.next())
        assertNull(rsi1.next())

        val v2 = byteArrayOf(2, 2)
        val rsi2 = NorunCombineIterator(v2)
        assertEquals(RunOrNot.Norun(0, 2), rsi2.next())
        assertNull(rsi2.next())

        val v3 = byteArrayOf(3, 3, 3)
        val rsi3 = NorunCombineIterator(v3)
        assertEquals(RunOrNot.Run(3u, 3), rsi3.next())
        assertNull(rsi3.next())

        val v4 = byteArrayOf(4, 4, 3, 3, 3)
        val rsi4 = NorunCombineIterator(v4)
        assertEquals(RunOrNot.Norun(0, 2), rsi4.next())
        assertEquals(RunOrNot.Run(3u, 3), rsi4.next())
        assertNull(rsi4.next())

        val v5 = ByteArray(400) { 40 }
        val rsi5 = NorunCombineIterator(v5)
        assertEquals(RunOrNot.Run(40u, 127), rsi5.next())
        assertEquals(RunOrNot.Run(40u, 127), rsi5.next())
        assertEquals(RunOrNot.Run(40u, 127), rsi5.next())
        assertEquals(RunOrNot.Run(40u, 19), rsi5.next())
        assertNull(rsi5.next())

        val v6 = ByteArray(3) { 5 } + ByteArray(129) { 6 } + byteArrayOf(7, 3, 7, 10, 255.toByte())
        val rsi6 = NorunCombineIterator(v6)
        assertEquals(RunOrNot.Run(5u, 3), rsi6.next())
        assertEquals(RunOrNot.Run(6u, 127), rsi6.next())
        assertEquals(RunOrNot.Norun(130, 7), rsi6.next())
        assertNull(rsi6.next())

        val v7 = ByteArray(2) { 5 } + ByteArray(129) { 6 } + byteArrayOf(7, 3, 7, 7, 255.toByte())
        val rsi7 = NorunCombineIterator(v7)
        assertEquals(RunOrNot.Norun(0, 2), rsi7.next())
        assertEquals(RunOrNot.Run(6u, 127), rsi7.next())
        assertEquals(RunOrNot.Norun(129, 7), rsi7.next())
        assertNull(rsi7.next())

        val v8 = ByteArray(257) { (it % 2).toByte() }
        val rsi8 = NorunCombineIterator(v8)
        assertEquals(RunOrNot.Norun(0, 128), rsi8.next())
        assertEquals(RunOrNot.Norun(128, 128), rsi8.next())
        assertEquals(RunOrNot.Norun(256, 1), rsi8.next())
        assertNull(rsi8.next())
    }
}
