// port-lint: source src/codecs/jpeg/entropy.rs
package io.github.kotlinmania.image.codecs.jpeg

import kotlin.test.Test
import kotlin.test.assertEquals

class EntropyTest {
    /**
     * JPEG Baseline DC luminance Huffman table from ITU-T81 Annex K.3.3.1.
     * Exercises [buildHuffLutConst] against a well-known reference table:
     *   bits  = {0,1,5,1,1,1,1,1,1,0,0,0,0,0,0,0}
     *   huffval = {0..0x0B}
     * which expands per the JPEG spec to the (size, code) pairs asserted below.
     */
    @Test
    fun baselineDcLuminanceTableMatchesSpec() {
        val bits =
            byteArrayOf(
                0,
                1,
                5,
                1,
                1,
                1,
                1,
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
            ).toUByteArray()
        val huffval =
            byteArrayOf(
                0x00,
                0x01,
                0x02,
                0x03,
                0x04,
                0x05,
                0x06,
                0x07,
                0x08,
                0x09,
                0x0A,
                0x0B,
            ).toUByteArray()

        val lut = buildHuffLutConst(bits, huffval)

        val expected: List<Pair<UByte, UShort>> =
            listOf(
                2.toUByte() to 0x000.toUShort(),
                3.toUByte() to 0x002.toUShort(),
                3.toUByte() to 0x003.toUShort(),
                3.toUByte() to 0x004.toUShort(),
                3.toUByte() to 0x005.toUShort(),
                3.toUByte() to 0x006.toUShort(),
                4.toUByte() to 0x00E.toUShort(),
                5.toUByte() to 0x01E.toUShort(),
                6.toUByte() to 0x03E.toUShort(),
                7.toUByte() to 0x07E.toUShort(),
                8.toUByte() to 0x0FE.toUShort(),
                9.toUByte() to 0x1FE.toUShort(),
            )

        for ((index, pair) in expected.withIndex()) {
            assertEquals(pair, lut[index], "lut[$index]")
        }
    }

    @Test
    fun unusedEntriesAreSentinel() {
        val bits = UByteArray(16)
        val huffval = UByteArray(0)
        val lut = buildHuffLutConst(bits, huffval)
        assertEquals(256, lut.size)
        for (entry in lut) {
            assertEquals(17.toUByte() to 0.toUShort(), entry)
        }
    }
}
