// port-lint: source math/utils.rs
package io.github.kotlinmania.image.math

import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTest {
    /**
     * Translated from upstream's `quickcheck!` block. The original Rust
     * property test ranges over arbitrary `u32` pairs; the Kotlin port
     * exercises the same invariant over a deterministic sample drawn with
     * a fixed-seed `kotlin.random.Random`, plus boundary values that the
     * Rust generator is statistically unlikely to hit.
     */
    @Test
    fun resizeBoundsCorrectlyWidth() {
        val random = Random(seed = 0xC0FFEEL)
        val samples = buildList {
            add(0u to 0u)
            add(0u to 1u)
            add(1u to 0u)
            add(1u to 1u)
            add(1u to UInt.MAX_VALUE)
            add(UInt.MAX_VALUE to 1u)
            add(UInt.MAX_VALUE to UInt.MAX_VALUE)
            repeat(256) { add(random.nextInt().toUInt() to random.nextInt().toUInt()) }
        }
        for ((oldW, newW) in samples) {
            if (oldW == 0u || newW == 0u) continue
            // In this case, the scaling is limited by scaling of height.
            // We could check that case separately but it does not conform to the same expectation.
            if (newW.toULong() * 400UL >= oldW.toULong() * UInt.MAX_VALUE.toULong()) continue

            val result = resizeDimensions(oldW, 400u, newW, UInt.MAX_VALUE, false)
            val exact = (400.0 * newW.toDouble() / oldW.toDouble()).roundToLong().toUInt()
            assertEquals(newW, result.first)
            assertEquals(maxOf(exact, 1u), result.second)
        }
    }

    /**
     * Translated from upstream's `quickcheck!` block. See the note on
     * [resizeBoundsCorrectlyWidth].
     */
    @Test
    fun resizeBoundsCorrectlyHeight() {
        val random = Random(seed = 0xBADF00DL)
        val samples = buildList {
            add(0u to 0u)
            add(0u to 1u)
            add(1u to 0u)
            add(1u to 1u)
            add(1u to UInt.MAX_VALUE)
            add(UInt.MAX_VALUE to 1u)
            add(UInt.MAX_VALUE to UInt.MAX_VALUE)
            repeat(256) { add(random.nextInt().toUInt() to random.nextInt().toUInt()) }
        }
        for ((oldH, newH) in samples) {
            if (oldH == 0u || newH == 0u) continue
            // In this case, the scaling is limited by scaling of width.
            // We could check that case separately but it does not conform to the same expectation.
            if (400UL * newH.toULong() >= oldH.toULong() * UInt.MAX_VALUE.toULong()) continue

            val result = resizeDimensions(400u, oldH, UInt.MAX_VALUE, newH, false)
            val exact = (400.0 * newH.toDouble() / oldH.toDouble()).roundToLong().toUInt()
            assertEquals(newH, result.second)
            assertEquals(maxOf(exact, 1u), result.first)
        }
    }

    @Test
    fun resizeHandlesFill() {
        var result = resizeDimensions(100u, 200u, 200u, 500u, true)
        assertTrue(result.first == 250u)
        assertTrue(result.second == 500u)

        result = resizeDimensions(200u, 100u, 500u, 200u, true)
        assertTrue(result.first == 500u)
        assertTrue(result.second == 250u)
    }

    @Test
    fun resizeNeverRoundsToZero() {
        val result = resizeDimensions(1u, 150u, 128u, 128u, false)
        assertTrue(result.first > 0u)
        assertTrue(result.second > 0u)
    }

    @Test
    fun resizeHandlesOverflow() {
        var result = resizeDimensions(100u, UInt.MAX_VALUE, 200u, UInt.MAX_VALUE, true)
        assertTrue(result.first == 100u)
        assertTrue(result.second == UInt.MAX_VALUE)

        result = resizeDimensions(UInt.MAX_VALUE, 100u, UInt.MAX_VALUE, 200u, true)
        assertTrue(result.first == UInt.MAX_VALUE)
        assertTrue(result.second == 100u)
    }

    @Test
    fun resizeRounds() {
        // Only truncation will result in (3840, 2229) and (2160, 3719)
        var result = resizeDimensions(4264u, 2476u, 3840u, 2160u, true)
        assertEquals(3840u to 2230u, result)

        result = resizeDimensions(2476u, 4264u, 2160u, 3840u, false)
        assertEquals(2160u to 3720u, result)
    }

    @Test
    fun resizeHandlesZero() {
        var result = resizeDimensions(0u, 100u, 100u, 100u, false)
        assertEquals(1u to 100u, result)

        result = resizeDimensions(100u, 0u, 100u, 100u, false)
        assertEquals(100u to 1u, result)

        result = resizeDimensions(100u, 100u, 0u, 100u, false)
        assertEquals(1u to 1u, result)

        result = resizeDimensions(100u, 100u, 100u, 0u, false)
        assertEquals(1u to 1u, result)
    }
}
