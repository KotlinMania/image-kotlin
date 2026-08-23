// port-lint: source imageops/mod.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.UByteLerp
import io.github.kotlinmania.image.images.GenericImage
import io.github.kotlinmania.image.images.GenericImageView
import io.github.kotlinmania.image.images.SubImage

/**
 * Return a mutable view into an image.
 * The coordinates set the position of the top left corner of the crop.
 */
public fun <P> crop(
    image: GenericImage<P>,
    x: UInt,
    y: UInt,
    width: UInt,
    height: UInt,
): SubImage<P> {
    val (cx, cy, cw, ch) = cropDimms(image, x, y, width, height)
    return SubImage(image, cx, cy, cw, ch)
}

/**
 * Return an immutable view into an image.
 * The coordinates set the position of the top left corner of the crop.
 */
public fun <P> cropImm(
    image: GenericImageView<P>,
    x: UInt,
    y: UInt,
    width: UInt,
    height: UInt,
): SubImage<P> {
    val (cx, cy, cw, ch) = cropDimms(image, x, y, width, height)
    @Suppress("UNCHECKED_CAST")
    return SubImage(image as GenericImage<P>, cx, cy, cw, ch)
}

/**
 * Calculates cropped dimension bounds.
 */
public fun <P> cropDimms(
    image: GenericImageView<P>,
    x: UInt,
    y: UInt,
    width: UInt,
    height: UInt,
): CropDimmsResult {
    val (iwidth, iheight) = image.dimensions()

    val nx = minOf(x, iwidth)
    val ny = minOf(y, iheight)

    val nh = minOf(height, iheight - ny)
    val nw = minOf(width, iwidth - nx)

    return CropDimmsResult(nx, ny, nw, nh)
}

public data class CropDimmsResult(
    public val x: UInt,
    public val y: UInt,
    public val width: UInt,
    public val height: UInt,
)

/**
 * Calculate the region that can be copied from top to bottom.
 */
public fun overlayBounds(
    bottomDimensions: Pair<UInt, UInt>,
    topDimensions: Pair<UInt, UInt>,
    x: UInt,
    y: UInt,
): Pair<UInt, UInt> {
    val (bottomWidth, bottomHeight) = bottomDimensions
    val (topWidth, topHeight) = topDimensions

    val xRange = (topWidth.toULong() + x.toULong()).coerceAtMost(bottomWidth.toULong()).toLong() - x.toLong()
    val actualXRange = xRange.coerceAtLeast(0L).toUInt()

    val yRange = (topHeight.toULong() + y.toULong()).coerceAtMost(bottomHeight.toULong()).toLong() - y.toLong()
    val actualYRange = yRange.coerceAtLeast(0L).toUInt()

    return Pair(actualXRange, actualYRange)
}

public data class OverlayBoundsExtResult(
    public val originBottomX: UInt,
    public val originBottomY: UInt,
    public val originTopX: UInt,
    public val originTopY: UInt,
    public val xRange: UInt,
    public val yRange: UInt,
)

/**
 * Extended overlay bounds calculation with support for negative and overflowing offsets.
 */
public fun overlayBoundsExt(
    bottomDimensions: Pair<UInt, UInt>,
    topDimensions: Pair<UInt, UInt>,
    x: Long,
    y: Long,
): OverlayBoundsExtResult {
    val (bottomWidth, bottomHeight) = bottomDimensions
    val (topWidth, topHeight) = topDimensions

    val bw = bottomWidth.toLong()
    val bh = bottomHeight.toLong()
    val tw = topWidth.toLong()
    val th = topHeight.toLong()

    if (x > bw || y > bh || saturatingAdd(x, tw) <= 0L || saturatingAdd(y, th) <= 0L) {
        return OverlayBoundsExtResult(0u, 0u, 0u, 0u, 0u, 0u)
    }

    val maxX = saturatingAdd(x, tw)
    val maxY = saturatingAdd(y, th)

    val maxInboundsX = maxX.coerceIn(0L, bw).toUInt()
    val maxInboundsY = maxY.coerceIn(0L, bh).toUInt()
    val originBottomX = x.coerceIn(0L, bw).toUInt()
    val originBottomY = y.coerceIn(0L, bh).toUInt()

    val xRange = maxInboundsX - originBottomX
    val yRange = maxInboundsY - originBottomY

    val originTopX = saturatingMul(x, -1L).coerceIn(0L, tw).toUInt()
    val originTopY = saturatingMul(y, -1L).coerceIn(0L, th).toUInt()

    return OverlayBoundsExtResult(
        originBottomX = originBottomX,
        originBottomY = originBottomY,
        originTopX = originTopX,
        originTopY = originTopY,
        xRange = xRange,
        yRange = yRange,
    )
}

private fun saturatingAdd(a: Long, b: Long): Long {
    val res = a + b
    if (((a xor res) and (b xor res)) < 0L) {
        return if (a < 0L) Long.MIN_VALUE else Long.MAX_VALUE
    }
    return res
}

private fun saturatingMul(a: Long, b: Long): Long {
    if (b == -1L && a == Long.MIN_VALUE) return Long.MAX_VALUE
    val res = a * b
    if (a != 0L && res / a != b) {
        return if ((a > 0L) xor (b > 0L)) Long.MIN_VALUE else Long.MAX_VALUE
    }
    return res
}

/**
 * Overlay an image at a given coordinate (x, y).
 */
public fun <P> overlay(
    bottom: GenericImage<P>,
    top: GenericImageView<P>,
    x: Long,
    y: Long,
) {
    val bottomDims = bottom.dimensions()
    val topDims = top.dimensions()

    val bounds = overlayBoundsExt(bottomDims, topDims, x, y)
    for (ry in 0u until bounds.yRange) {
        for (rx in 0u until bounds.xRange) {
            val p = top.getPixel(bounds.originTopX + rx, bounds.originTopY + ry)
            bottom.blendPixel(bounds.originBottomX + rx, bounds.originBottomY + ry, p)
        }
    }
}

/**
 * Tile an image by repeating it multiple times.
 */
public fun <P> tile(
    bottom: GenericImage<P>,
    top: GenericImageView<P>,
) {
    val tw = top.width().toLong()
    val th = top.height().toLong()
    if (tw <= 0L || th <= 0L) return

    val bw = bottom.width().toLong()
    val bh = bottom.height().toLong()

    var x = 0L
    while (x < bw) {
        var y = 0L
        while (y < bh) {
            overlay(bottom, top, x, y)
            y += th
        }
        x += tw
    }
}

/**
 * Fill the image with a linear vertical gradient.
 */
public fun verticalGradient(
    img: GenericImage<Rgb<UByte>>,
    start: Rgb<UByte>,
    stop: Rgb<UByte>,
) {
    val h = img.height()
    if (h == 0u) return
    val denom = if (h > 1u) (h - 1u).toFloat() else 1.0f

    for (y in 0u until h) {
        val ratio = if (h > 1u) y.toFloat() / denom else 0.0f
        val r = UByteLerp.lerp(start.r, stop.r, ratio)
        val g = UByteLerp.lerp(start.g, stop.g, ratio)
        val b = UByteLerp.lerp(start.b, stop.b, ratio)
        val pixel = Rgb(r, g, b)

        for (x in 0u until img.width()) {
            img.putPixel(x, y, pixel)
        }
    }
}

/**
 * Fill the image with a linear horizontal gradient.
 */
public fun horizontalGradient(
    img: GenericImage<Rgb<UByte>>,
    start: Rgb<UByte>,
    stop: Rgb<UByte>,
) {
    val w = img.width()
    if (w == 0u) return
    val denom = if (w > 1u) (w - 1u).toFloat() else 1.0f

    for (x in 0u until w) {
        val ratio = if (w > 1u) x.toFloat() / denom else 0.0f
        val r = UByteLerp.lerp(start.r, stop.r, ratio)
        val g = UByteLerp.lerp(start.g, stop.g, ratio)
        val b = UByteLerp.lerp(start.b, stop.b, ratio)
        val pixel = Rgb(r, g, b)

        for (y in 0u until img.height()) {
            img.putPixel(x, y, pixel)
        }
    }
}

/**
 * Fill the image with a linear vertical gradient (RGBA).
 */
public fun verticalGradientRgba(
    img: GenericImage<Rgba<UByte>>,
    start: Rgba<UByte>,
    stop: Rgba<UByte>,
) {
    val h = img.height()
    if (h == 0u) return
    val denom = if (h > 1u) (h - 1u).toFloat() else 1.0f

    for (y in 0u until h) {
        val ratio = if (h > 1u) y.toFloat() / denom else 0.0f
        val r = UByteLerp.lerp(start.r, stop.r, ratio)
        val g = UByteLerp.lerp(start.g, stop.g, ratio)
        val b = UByteLerp.lerp(start.b, stop.b, ratio)
        val a = UByteLerp.lerp(start.a, stop.a, ratio)
        val pixel = Rgba(r, g, b, a)

        for (x in 0u until img.width()) {
            img.putPixel(x, y, pixel)
        }
    }
}

/**
 * Fill the image with a linear horizontal gradient (RGBA).
 */
public fun horizontalGradientRgba(
    img: GenericImage<Rgba<UByte>>,
    start: Rgba<UByte>,
    stop: Rgba<UByte>,
) {
    val w = img.width()
    if (w == 0u) return
    val denom = if (w > 1u) (w - 1u).toFloat() else 1.0f

    for (x in 0u until w) {
        val ratio = if (w > 1u) x.toFloat() / denom else 0.0f
        val r = UByteLerp.lerp(start.r, stop.r, ratio)
        val g = UByteLerp.lerp(start.g, stop.g, ratio)
        val b = UByteLerp.lerp(start.b, stop.b, ratio)
        val a = UByteLerp.lerp(start.a, stop.a, ratio)
        val pixel = Rgba(r, g, b, a)

        for (y in 0u until img.height()) {
            img.putPixel(x, y, pixel)
        }
    }
}

/**
 * Replace the contents of an image at a given coordinate (x, y).
 */
public fun <P> replace(
    bottom: GenericImage<P>,
    top: GenericImageView<P>,
    x: Long,
    y: Long,
) {
    val bottomDims = bottom.dimensions()
    val topDims = top.dimensions()

    val bounds = overlayBoundsExt(bottomDims, topDims, x, y)
    for (ry in 0u until bounds.yRange) {
        for (rx in 0u until bounds.xRange) {
            val p = top.getPixel(bounds.originTopX + rx, bounds.originTopY + ry)
            bottom.putPixel(bounds.originBottomX + rx, bounds.originBottomY + ry, p)
        }
    }
}
