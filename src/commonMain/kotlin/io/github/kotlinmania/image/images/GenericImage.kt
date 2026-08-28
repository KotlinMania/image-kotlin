// port-lint: source images/generic_image.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.math.Rect

/**
 * Trait to inspect an image.
 */
public interface GenericImageView<P> {
    /** The width and height of this image. */
    public fun dimensions(): Pair<UInt, UInt>

    /** The width of this image. */
    public fun width(): UInt = dimensions().first

    /** The height of this image. */
    public fun height(): UInt = dimensions().second

    /** Returns true if this x, y coordinate is contained inside the image. */
    public fun inBounds(x: UInt, y: UInt): Boolean {
        val (w, h) = dimensions()
        return x < w && y < h
    }

    /** Returns the pixel located at (x, y). Indexed from top left. */
    public fun getPixel(x: UInt, y: UInt): P

    /** Returns the pixel located at (x, y). Indexed from top left. */
    public fun unsafeGetPixel(x: UInt, y: UInt): P = getPixel(x, y)

    /** Returns an Iterator over the pixels of this image. */
    public fun pixels(): Pixels<P> = Pixels(this, width(), height())

    /** Returns a subimage that is an immutable view into this image. */
    public fun view(x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> {
        require(x.toULong() + width.toULong() <= width().toULong()) { "View width out of bounds" }
        require(y.toULong() + height.toULong() <= height().toULong()) { "View height out of bounds" }
        return SubImage(this, x, y, width, height)
    }

    /** Returns a subimage that is a view into this image if within bounds. */
    public fun tryView(x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P>? {
        if (x.toULong() + width.toULong() > width().toULong() || y.toULong() + height.toULong() > height().toULong()) {
            return null
        }
        return SubImage(this as GenericImage<P>, x, y, width, height)
    }

    /** Allocate a buffer with dimensions (width, height) suited for this image. */
    @Suppress("UNCHECKED_CAST")
    public fun bufferWithDimensions(width: UInt, height: UInt): ImageBuffer<P, ByteArray> {
        val imgBuf = this as? ImageBuffer<P, *>
        if (imgBuf != null) {
            return (imgBuf as ImageBuffer<P, ByteArray>).bufferWithDimensions(width, height)
        }
        val subImg = this as? SubImage<P>
        if (subImg != null) {
            val inner = subImg.image
            if (inner is ImageBuffer<P, *>) {
                return (inner as ImageBuffer<P, ByteArray>).bufferWithDimensions(width, height)
            }
        }
        if (width() > 0u && height() > 0u) {
            val p = getPixel(0u, 0u)
            val buf: ImageBuffer<*, ByteArray> = when (p) {
                is io.github.kotlinmania.image.Rgba<*> -> ImageBuffer.createRgba(width, height)
                is io.github.kotlinmania.image.Rgb<*> -> ImageBuffer.createRgb(width, height)
                is io.github.kotlinmania.image.LumaA<*> -> ImageBuffer.createGrayAlpha(width, height)
                is io.github.kotlinmania.image.Luma<*> -> ImageBuffer.createGray(width, height)
                else -> ImageBuffer.createRgba(width, height)
            }
            return buf as ImageBuffer<P, ByteArray>
        }
        return ImageBuffer.createRgba(width, height) as ImageBuffer<P, ByteArray>
    }

    /** Allocate a buffer with dimensions equal to this image. */
    public fun bufferLike(): ImageBuffer<P, ByteArray> = bufferWithDimensions(width(), height())
}

/**
 * Immutable pixel iterator over (x, y, pixel).
 */
public class Pixels<P>(
    private val image: GenericImageView<P>,
    private val width: UInt,
    private val height: UInt,
) : Iterator<Triple<UInt, UInt, P>> {
    private var curX: UInt = 0u
    private var curY: UInt = 0u

    override fun hasNext(): Boolean {
        if (width == 0u || height == 0u) return false
        return curY < height
    }

    override fun next(): Triple<UInt, UInt, P> {
        if (!hasNext()) throw NoSuchElementException("No more pixels")
        val px = image.getPixel(curX, curY)
        val result = Triple(curX, curY, px)
        curX++
        if (curX >= width) {
            curX = 0u
            curY++
        }
        return result
    }
}

/**
 * A trait for manipulating images.
 */
public interface GenericImage<P> : GenericImageView<P> {
    /** Put a pixel at location (x, y). Indexed from top left. */
    public fun putPixel(x: UInt, y: UInt, pixel: P)

    /** Puts a pixel at location (x, y). Indexed from top left. */
    public fun unsafePutPixel(x: UInt, y: UInt, pixel: P) {
        putPixel(x, y, pixel)
    }

    /** Put a pixel at location (x, y), taking into account alpha channels. */
    public fun blendPixel(x: UInt, y: UInt, pixel: P)

    /** Copies all of the pixels from another image into this image. */
    public fun copyFrom(other: GenericImageView<P>, x: UInt, y: UInt): Boolean {
        if (width() < other.width() + x || height() < other.height() + y) {
            return false
        }
        for (k in 0u until other.height()) {
            for (i in 0u until other.width()) {
                val p = other.getPixel(i, k)
                putPixel(i + x, k + y, p)
            }
        }
        return true
    }

    /** Copies all of the pixels from one part of this image to another part of this image. */
    public fun copyWithin(source: Rect, x: UInt, y: UInt): Boolean {
        val sx = source.x
        val sy = source.y
        val sw = source.width
        val sh = source.height
        val dx = x
        val dy = y

        if (sx >= width() || dx >= width() || sy >= height() || dy >= height()) {
            return false
        }

        val maxW = if (dx > sx) dx else sx
        val maxH = if (dy > sy) dy else sy
        if (width() - maxW < sw || height() - maxH < sh) {
            return false
        }

        if (sx < dx && sy < dy) {
            for (currY in (sh - 1u) downTo 0u) {
                for (currX in (sw - 1u) downTo 0u) {
                    val p = getPixel(sx + currX, sy + currY)
                    putPixel(dx + currX, dy + currY, p)
                }
            }
        } else if (sx < dx) {
            for (currY in 0u until sh) {
                for (currX in (sw - 1u) downTo 0u) {
                    val p = getPixel(sx + currX, sy + currY)
                    putPixel(dx + currX, dy + currY, p)
                }
            }
        } else if (sy < dy) {
            for (currY in (sh - 1u) downTo 0u) {
                for (currX in 0u until sw) {
                    val p = getPixel(sx + currX, sy + currY)
                    putPixel(dx + currX, dy + currY, p)
                }
            }
        } else {
            for (currY in 0u until sh) {
                for (currX in 0u until sw) {
                    val p = getPixel(sx + currX, sy + currY)
                    putPixel(dx + currX, dy + currY, p)
                }
            }
        }
        return true
    }

    /** Returns a mutable subimage that is a view into this image. */
    public fun subImage(x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> {
        require(x.toULong() + width.toULong() <= width().toULong()) { "SubImage width out of bounds" }
        require(y.toULong() + height.toULong() <= height().toULong()) { "SubImage height out of bounds" }
        return SubImage(this, x, y, width, height)
    }
}
