// port-lint: source math/utils.rs
package io.github.kotlinmania.image.math

import kotlin.math.roundToLong

/**
 * Shared mathematical utility functions.
 */

/**
 * Uses fused multiply add when available
 *
 * It is important not to call it if FMA flag is not turned on,
 * Rust inserts libc `fmaf` based implementation here if FMA is clearly not available at compile time.
 * This needs for speed only, one rounding error don't do anything useful here, thus it's blocked when
 * we can't detect FMA availability at compile time.
 */
internal fun multiplyAccumulate(acc: Float, a: Float, b: Float): Float = acc + a * b

internal fun multiplyAccumulate(acc: Double, a: Double, b: Double): Double = acc + a * b

/**
 * Calculates the width and height an image should be resized to.
 * This preserves aspect ratio, and based on the `fill` parameter
 * will either fill the dimensions to fit inside the smaller constraint
 * (will overflow the specified bounds on one axis to preserve
 * aspect ratio), or will shrink so that both dimensions are
 * completely contained within the given `width` and `height`,
 * with empty space on one axis.
 */
internal fun resizeDimensions(
    width: UInt,
    height: UInt,
    nwidth: UInt,
    nheight: UInt,
    fill: Boolean,
): Pair<UInt, UInt> {
    val wratio = nwidth.toDouble() / width.toDouble()
    val hratio = nheight.toDouble() / height.toDouble()

    val ratio = if (fill) {
        maxOf(wratio, hratio)
    } else {
        minOf(wratio, hratio)
    }

    val nw = maxOf((width.toDouble() * ratio).roundToLong(), 1L)
    val nh = maxOf((height.toDouble() * ratio).roundToLong(), 1L)

    return if (nw > UInt.MAX_VALUE.toLong()) {
        val ratioMax = UInt.MAX_VALUE.toDouble() / width.toDouble()
        UInt.MAX_VALUE to maxOf((height.toDouble() * ratioMax).roundToLong(), 1L).toUInt()
    } else if (nh > UInt.MAX_VALUE.toLong()) {
        val ratioMax = UInt.MAX_VALUE.toDouble() / height.toDouble()
        maxOf((width.toDouble() * ratioMax).roundToLong(), 1L).toUInt() to UInt.MAX_VALUE
    } else {
        nw.toUInt() to nh.toUInt()
    }
}
