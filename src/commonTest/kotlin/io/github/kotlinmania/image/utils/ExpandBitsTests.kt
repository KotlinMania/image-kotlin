// port-lint: source src/utils/mod.rs (#[cfg(test)] mod test::gray_to_luma8_skip)
package io.github.kotlinmania.image.utils

import kotlin.test.Test
import kotlin.test.assertContentEquals

class ExpandBitsTests {
    private fun bytes(vararg values: Int): ByteArray =
        ByteArray(values.size) { values[it].toByte() }

    @Test
    fun grayToLuma8Skip() {
        // Bit depth 1, skip is more than half a byte
        assertContentEquals(
            bytes(255, 255, 255, 255, 0, 0, 0, 0, 255, 255, 0, 0, 0, 0, 255, 255, 255, 255, 255, 255),
            expandBits(1, 10u, bytes(0b1111_0000, 0b1100_0000, 0b0000_1111, 0b1100_0000)),
        )
        // Bit depth 2, skip is more than half a byte
        assertContentEquals(
            bytes(255, 255, 0, 0, 255, 0, 0, 255, 255, 255),
            expandBits(2, 5u, bytes(0b1111_0000, 0b1100_0000, 0b0000_1111, 0b1100_0000)),
        )
        // Bit depth 2, skip is 0
        assertContentEquals(
            bytes(255, 255, 0, 0, 0, 0, 255, 255),
            expandBits(2, 4u, bytes(0b1111_0000, 0b0000_1111)),
        )
        // Bit depth 4, skip is half a byte
        assertContentEquals(
            bytes(255, 0),
            expandBits(4, 1u, bytes(0b1111_0011, 0b0000_1100)),
        )
    }
}
