// port-lint: source images/sub_image.rs
package io.github.kotlinmania.image.images

/**
 * A view into another image.
 *
 * Instances of this class can be created using:
 * - [GenericImage.subImage] to create a mutable view,
 * - [GenericImageView.view] to create an immutable view,
 * - [SubImage.new] to instantiate the view directly.
 */
public class SubImage<P> internal constructor(
    private val inner: SubImageInner<GenericImageView<P>>,
) : GenericImage<P> {
    public constructor(
        image: GenericImageView<P>,
        x: UInt,
        y: UInt,
        width: UInt,
        height: UInt,
    ) : this(
        SubImageInner(
            image = image,
            xoffset = x,
            yoffset = y,
            xstride = width,
            ystride = height,
        ),
    )

    public val image: GenericImageView<P>
        get() = inner.image

    public var xOffset: UInt
        get() = inner.xoffset
        set(value) {
            inner.xoffset = value
        }

    public var yOffset: UInt
        get() = inner.yoffset
        set(value) {
            inner.yoffset = value
        }

    public var width: UInt
        get() = inner.xstride
        set(value) {
            inner.xstride = value
        }

    public var height: UInt
        get() = inner.ystride
        set(value) {
            inner.ystride = value
        }

    /**
     * Change the coordinates of this subimage.
     */
    public fun changeBounds(x: UInt, y: UInt, width: UInt, height: UInt) {
        inner.xoffset = x
        inner.yoffset = y
        inner.xstride = width
        inner.ystride = height
    }

    /**
     * The offsets of this subimage relative to the underlying image.
     */
    public fun offsets(): Pair<UInt, UInt> {
        return Pair(inner.xoffset, inner.yoffset)
    }

    /**
     * Convert this subimage to an [ImageBuffer].
     */
    public fun toImage(): GenericImage<P> {
        val borrowed = inner.image
        val out: GenericImage<P> = borrowed.bufferWithDimensions(inner.xstride, inner.ystride)

        for (y in 0u until inner.ystride) {
            for (x in 0u until inner.xstride) {
                val p = borrowed.getPixel(x + inner.xoffset, y + inner.yoffset)
                out.putPixel(x, y, p)
            }
        }

        return out
    }

    /**
     * Create a sub-view of the image.
     *
     * The coordinates given are relative to the current view on the underlying image.
     */
    override fun view(x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> {
        check(x.toULong() + width.toULong() <= inner.xstride.toULong())
        check(y.toULong() + height.toULong() <= inner.ystride.toULong())
        val newX = inner.xoffset + x
        val newY = inner.yoffset + y
        return SubImage.new(inner.image, newX, newY, width, height)
    }

    /**
     * Get a reference to the underlying image.
     */
    public fun inner(): GenericImageView<P> {
        return inner.image
    }

    /**
     * Create a mutable sub-view of the image.
     *
     * The coordinates given are relative to the current view on the underlying image.
     */
    override fun subImage(x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> {
        check(x.toULong() + width.toULong() <= inner.xstride.toULong())
        check(y.toULong() + height.toULong() <= inner.ystride.toULong())
        val newX = inner.xoffset + x
        val newY = inner.yoffset + y
        return SubImage.new(inner.image, newX, newY, width, height)
    }

    /**
     * Get a mutable reference to the underlying image.
     */
    public fun innerMut(): GenericImage<P>? {
        return inner.image as? GenericImage<P>
    }

    /**
     * Dereferences to the underlying image view.
     */
    public fun deref(): SubImageInner<GenericImageView<P>> {
        return inner
    }

    /**
     * Dereferences to the underlying mutable image.
     */
    public fun derefMut(): SubImageInner<GenericImageView<P>> {
        return inner
    }

    override fun dimensions(): Pair<UInt, UInt> {
        return Pair(inner.xstride, inner.ystride)
    }

    override fun getPixel(x: UInt, y: UInt): P {
        return inner.image.getPixel(x + inner.xoffset, y + inner.yoffset)
    }

    /**
     * Create a buffer with the dimensions of this sub-image.
     */
    override fun bufferWithDimensions(width: UInt, height: UInt): ImageBuffer<P, ByteArray> {
        return inner.image.bufferWithDimensions(width, height)
    }

    public fun getPixelMut(x: UInt, y: UInt): P {
        val img = inner.image as? GenericImage<P>
        return img?.getPixel(x + inner.xoffset, y + inner.yoffset) ?: inner.image.getPixel(x + inner.xoffset, y + inner.yoffset)
    }

    override fun putPixel(x: UInt, y: UInt, pixel: P) {
        val img = inner.image as? GenericImage<P>
        img?.putPixel(x + inner.xoffset, y + inner.yoffset, pixel)
    }

    /**
     * Blend the pixel directly at the specified coordinate.
     */
    override fun blendPixel(x: UInt, y: UInt, pixel: P) {
        val img = inner.image as? GenericImage<P>
        img?.blendPixel(x + inner.xoffset, y + inner.yoffset, pixel)
    }

    override fun bufferLike(): ImageBuffer<P, ByteArray> = bufferWithDimensions(inner.xstride, inner.ystride)

    public fun colorSpace(): io.github.kotlinmania.image.metadata.Cicp =
        (inner.image as? ImageBuffer<*, *>)?.colorSpace() ?: io.github.kotlinmania.image.metadata.Cicp.SRGB

    public companion object {
        /**
         * Construct a new subimage.
         * The coordinates set the position of the top left corner of the [SubImage].
         */
        public fun <P> new(image: GenericImageView<P>, x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> {
            return SubImage(
                inner = SubImageInner(
                    image = image,
                    xoffset = x,
                    yoffset = y,
                    xstride = width,
                    ystride = height,
                ),
            )
        }
    }
}

/**
 * The inner type of [SubImage] that holds positional offsets and strides.
 */
public data class SubImageInner<I>(
    public val image: I,
    public var xoffset: UInt,
    public var yoffset: UInt,
    public var xstride: UInt,
    public var ystride: UInt,
)

public typealias DerefPixel<I> = Any?
public typealias DerefSubpixel<I> = Any?
public typealias Target = Any?
