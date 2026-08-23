// port-lint: source animation.rs
package io.github.kotlinmania.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AnimationTest {
    @Test
    fun testDelaySimple() {
        val second = Delay.fromNumerDenomMs(1000u, 1u)
        assertEquals(1.seconds, second.toDuration())
    }

    @Test
    fun testDelayEquivalence() {
        val delay10ms = Delay.fromNumerDenomMs(10u, 1u)
        val delay10000us = Delay.fromNumerDenomMs(10000u, 1000u)
        assertEquals(delay10ms, delay10000us)
        assertEquals(10.milliseconds, delay10ms.toDuration())
    }

    @Test
    fun testFramesCollection() {
        val frame1 = Frame(ByteArray(4), 1u, 1u, Delay.fromNumerDenomMs(100u, 1u))
        val frame2 = Frame(ByteArray(4), 1u, 1u, Delay.fromNumerDenomMs(200u, 1u))
        val frames = Frames(listOf(frame1, frame2).iterator())
        val collected = frames.collectFrames()
        assertEquals(2, collected.size)
        assertEquals(100u, collected[0].delay().ratio.numer)
        assertEquals(200u, collected[1].delay().ratio.numer)
    }

    @Test
    fun testRatioOrdering() {
        val r1 = Ratio.create(1u, 3u)
        val r2 = Ratio.create(1u, 2u)
        val r3 = Ratio.create(2u, 6u)
        assertTrue(r1 < r2)
        assertEquals(0, r1.compareTo(r3))
    }
}
