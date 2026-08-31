// port-lint: tests io/limits.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ColorType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LimitsTest {
    @Test
    fun testDefaultLimits() {
        val limits = Limits()
        assertEquals(512uL * 1024uL * 1024uL, limits.maxAlloc)
    }

    @Test
    fun testNoLimits() {
        val limits = Limits.noLimits()
        assertEquals(null, limits.maxImageWidth)
        assertEquals(null, limits.maxImageHeight)
        assertEquals(null, limits.maxAlloc)
    }

    @Test
    fun testCheckDimensions() {
        val limits = Limits(maxImageWidth = 100u, maxImageHeight = 100u)
        assertTrue(limits.checkDimensions(50u, 50u).isSuccess)
        assertTrue(limits.checkDimensions(150u, 50u).isFailure)
        assertTrue(limits.checkDimensions(50u, 150u).isFailure)
    }

    @Test
    fun testReserveAndFree() {
        val limits = Limits(maxAlloc = 1000uL)
        assertTrue(limits.reserve(400uL).isSuccess)
        assertEquals(600uL, limits.maxAlloc)
        assertTrue(limits.reserve(700uL).isFailure)

        limits.free(200uL)
        assertEquals(800uL, limits.maxAlloc)
    }

    @Test
    fun testReserveBuffer() {
        val limits = Limits(maxImageWidth = 100u, maxImageHeight = 100u, maxAlloc = 1000uL)
        assertTrue(limits.reserveBuffer(10u, 10u, ColorType.Rgb8).isSuccess) // 10 * 10 * 3 = 300
        assertEquals(700uL, limits.maxAlloc)
    }
}
