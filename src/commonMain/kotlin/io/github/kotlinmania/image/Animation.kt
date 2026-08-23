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
) : Iterator<Frame> by iterator {
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

        public fun fromDuration(duration: Duration): Delay = fromSaturatingDuration(duration)

        private fun compareFraction(a: Pair<ULong, ULong>, b: Pair<ULong, ULong>): Int {
            val left = a.first * b.second
            val right = b.first * a.second
            return left.compareTo(right)
        }

        private fun absDiffNom(a: Pair<ULong, ULong>, b: Pair<ULong, ULong>): ULong {
            val c0 = a.first * b.second
            val c1 = a.second * b.first
            val d0 = maxOf(c0, c1)
            val d1 = minOf(c0, c1)
            return d0 - d1
        }

        public fun closestBoundedFraction(denomBound: UInt, nom: UInt, denom: UInt): Pair<UInt, UInt> {
            require(denom > 0u)
            require(denomBound > 0u)
            require(nom < denom)

            val exact = Pair(nom.toULong(), denom.toULong())
            var lower = Pair(0uL, 1uL)
            var upper = Pair(1uL, 1uL)
            var guess = Pair(if (nom * 2u > denom) 1uL else 0uL, 1uL)

            while (true) {
                if (compareFraction(guess, exact) == 0) {
                    break
                }
                if (denomBound.toULong() - lower.second < upper.second) {
                    break
                }
                val next = Pair(lower.first + upper.first, lower.second + upper.second)
                if (compareFraction(exact, next) < 0) {
                    upper = next
                } else {
                    lower = next
                }

                val gDiffNom = absDiffNom(guess, exact)
                val nDiffNom = absDiffNom(next, exact)

                val cmpIntegral = (nDiffNom / next.second).compareTo(gDiffNom / guess.second)
                val shouldReplace =
                    when {
                        cmpIntegral < 0 -> true
                        cmpIntegral > 0 -> false
                        else ->
                            compareFraction(
                                Pair(nDiffNom % next.second, next.second),
                                Pair(gDiffNom % guess.second, guess.second),
                            ) < 0
                    }
                if (shouldReplace) {
                    guess = next
                }
            }
            return Pair(guess.first.toUInt(), guess.second.toUInt())
        }

        public fun fromSaturatingDuration(duration: Duration): Delay {
            val millisBound = UInt.MAX_VALUE.toULong()
            val totalMillis =
                duration.inWholeMilliseconds
                    .coerceAtLeast(0L)
                    .toULong()
                    .coerceAtMost(millisBound)
            val totalNanos = duration.inWholeNanoseconds.coerceAtLeast(0L)
            val submillis = ((totalNanos % 1_000_000L).toUInt())

            val maxB =
                if (totalMillis > 0uL) {
                    ((millisBound + 1uL) / (totalMillis + 1uL)).toUInt()
                } else {
                    millisBound.toUInt()
                }
            val millis = totalMillis.toUInt()

            val (a, b) =
                if (submillis > 0u && maxB > 0u) {
                    closestBoundedFraction(maxB, submillis, 1_000_000u)
                } else {
                    Pair(0u, 1u)
                }
            return fromNumerDenomMs(a + b * millis, b)
        }
    }
}
