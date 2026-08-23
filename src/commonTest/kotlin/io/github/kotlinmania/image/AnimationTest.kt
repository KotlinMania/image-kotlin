// port-lint: tests animation.rs
package io.github.kotlinmania.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class AnimationTest {
    @Test
    fun testDelaySimple() {
        val second = Delay.fromNumerDenomMs(1000u, 1u)
        assertEquals(1.seconds, second.toDuration())
    }

    @Test
    fun testFps30() {
        val thirtieth = Delay.fromNumerDenomMs(1000u, 30u)
        val duration = thirtieth.toDuration()
        assertEquals(0L, duration.inWholeSeconds)
        assertEquals(33L, duration.inWholeMilliseconds)
        assertEquals(33_333_333L, duration.inWholeNanoseconds % 1_000_000_000L)
    }

    @Test
    fun testDurationOutlier() {
        val oob = 0xFFFF_FFFFL.seconds
        val delay = Delay.fromSaturatingDuration(oob)
        assertEquals(Pair(0xFFFF_FFFFu, 1u), delay.numerDenomMs())
    }

    @Test
    fun testDurationApprox() {
        val oob = 0xFFFF_FFFFL.milliseconds + 1.microseconds
        val delay1 = Delay.fromSaturatingDuration(oob)
        assertEquals(Pair(0xFFFF_FFFFu, 1u), delay1.numerDenomMs())

        val inbounds = 0xFFFF_FFFFL.milliseconds - 1.microseconds
        val delay2 = Delay.fromSaturatingDuration(inbounds)
        assertEquals(Pair(0xFFFF_FFFFu, 1u), delay2.numerDenomMs())

        val fine = (0xFFFF_FFFFL / 1000L).milliseconds + (0xFFFF_FFFFL % 1000L).microseconds
        val delay3 = Delay.fromSaturatingDuration(fine)
        assertEquals(Ratio.create(0xFFFF_FFFFu, 1000u), delay3.ratio)
    }

    @Test
    fun testPrecise() {
        val exceed = 333.seconds + 333_333_333.nanoseconds
        val delay = Delay.fromSaturatingDuration(exceed)
        assertEquals(exceed, delay.toDuration())
    }

    @Test
    fun testSmall() {
        val delay = Delay.fromNumerDenomMs((1 shl 16).toUInt(), ((1 shl 16) + 1).toUInt())
        val duration = delay.toDuration()
        assertEquals(0L, duration.inWholeMilliseconds)
        val delay2 = Delay.fromSaturatingDuration(duration)
        assertEquals(0u, delay2.ratio.toInteger())
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
