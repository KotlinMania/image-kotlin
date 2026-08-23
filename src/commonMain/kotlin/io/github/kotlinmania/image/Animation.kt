// port-lint: source animation.rs
package io.github.kotlinmania.image

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

/**
 * An iterator reading animation frames.
 */
public class Frames(
    private val iterator: Iterator<Frame>,
) : Iterator<Frame> {
    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): Frame = iterator.next()

    /**
     * Collects all frames into a List.
     */
    public fun collectFrames(): List<Frame> = iterator.asSequence().toList()
}

/**
 * A single animation frame.
 */
public class Frame(
    private val buffer: ByteArray,
    private val width: UInt,
    private val height: UInt,
    private val delay: Delay = Delay.fromNumerDenomMs(0u, 1u),
    private val left: UInt = 0u,
    private val top: UInt = 0u,
) {
    public fun delay(): Delay = delay

    public fun left(): UInt = left

    public fun top(): UInt = top

    public fun width(): UInt = width

    public fun height(): UInt = height

    public fun buffer(): ByteArray = buffer

    public fun copy(
        buffer: ByteArray = this.buffer.copyOf(),
        width: UInt = this.width,
        height: UInt = this.height,
        delay: Delay = this.delay,
        left: UInt = this.left,
        top: UInt = this.top,
    ): Frame = Frame(buffer, width, height, delay, left, top)
}

/**
 * Greatest common divisor for unsigned integers.
 */
private fun gcd(a: UInt, b: UInt): UInt {
    var x = a
    var y = b
    while (y != 0u) {
        val temp = y
        y = x % y
        x = temp
    }
    return x
}

/**
 * Internal exact rational representation.
 */
public data class Ratio(
    val numer: UInt,
    val denom: UInt,
) : Comparable<Ratio> {
    init {
        require(denom != 0u) { "Denominator cannot be zero" }
    }

    companion object {
        public fun create(numerator: UInt, denominator: UInt): Ratio {
            require(denominator != 0u) { "Denominator cannot be zero" }
            val divisor = gcd(numerator, denominator)
            return Ratio(numerator / divisor, denominator / divisor)
        }
    }

    public fun toInteger(): UInt = numer / denom

    override fun compareTo(other: Ratio): Int {
        val left = numer.toULong() * other.denom.toULong()
        val right = other.numer.toULong() * denom.toULong()
        return left.compareTo(right)
    }
}

/**
 * The delay of a frame relative to the previous one in milliseconds.
 */
public data class Delay(
    val ratio: Ratio,
) : Comparable<Delay> {
    public fun numerDenomMs(): Pair<UInt, UInt> = Pair(ratio.numer, ratio.denom)

    public fun toDuration(): Duration {
        val ms = ratio.toInteger().toLong()
        val rest = ratio.numer % ratio.denom
        val nanos = (rest.toULong() * 1_000_000uL) / ratio.denom.toULong()
        return ms.milliseconds + nanos.toLong().nanoseconds
    }

    override fun compareTo(other: Delay): Int = ratio.compareTo(other.ratio)

    companion object {
        public fun fromNumerDenomMs(numerator: UInt, denominator: UInt): Delay =
            Delay(Ratio.create(numerator, denominator))

        public fun fromDuration(duration: Duration): Delay {
            val millis = duration.inWholeMilliseconds.coerceIn(0L, UInt.MAX_VALUE.toLong()).toUInt()
            return fromNumerDenomMs(millis, 1u)
        }
    }
}
