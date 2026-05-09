// port-lint: source math/utils.rs
package io.github.kotlinmania.image.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTest {
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
