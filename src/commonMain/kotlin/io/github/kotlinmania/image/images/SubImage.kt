// port-lint: source images/sub_image.rs
package io.github.kotlinmania.image.images

/**
 * A view into a rectangular region of another image buffer.
 */
public class SubImage<P>(
    private val image: GenericImageView<P>,
    private var xOffset: UInt,
    private var yOffset: UInt,
    private var width: UInt,
    private var height: UInt,
) : GenericImage<P> {
    public fun offsets(): Pair<UInt, UInt> = Pair(xOffset, yOffset)

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    public fun changeBounds(x: UInt, y: UInt, newWidth: UInt, newHeight: UInt) {
        require(x.toULong() + newWidth.toULong() <= image.width().toULong()) {
            "SubImage bounds out of parent range"
        }
        require(y.toULong() + newHeight.toULong() <= image.height().toULong()) {
            "SubImage bounds out of parent range"
        }
        xOffset = x
        yOffset = y
        width = newWidth
        height = newHeight
    }

    override fun getPixel(x: UInt, y: UInt): P {
        require(x < width && y < height) { "Coordinates ($x, $y) out of sub-image bounds ($width, $height)" }
        return image.getPixel(xOffset + x, yOffset + y)
    }

    override fun putPixel(x: UInt, y: UInt, pixel: P) {
        require(x < width && y < height) { "Coordinates ($x, $y) out of sub-image bounds ($width, $height)" }
        val img = image as? GenericImage<P> ?: throw UnsupportedOperationException("Underlying image is immutable")
        img.putPixel(xOffset + x, yOffset + y, pixel)
    }

    override fun blendPixel(x: UInt, y: UInt, pixel: P) {
        require(x < width && y < height) { "Coordinates ($x, $y) out of sub-image bounds ($width, $height)" }
        val img = image as? GenericImage<P> ?: throw UnsupportedOperationException("Underlying image is immutable")
        img.blendPixel(xOffset + x, yOffset + y, pixel)
    }

    public fun inner(): GenericImageView<P> = image

    public fun innerMut(): GenericImage<P>? = image as? GenericImage<P>

    public fun deref(): GenericImageView<P> = image

    public fun derefMut(): GenericImage<P>? = image as? GenericImage<P>

    public fun toImage(): GenericImage<P> {
        val target: GenericImage<P> = bufferWithDimensions(width, height)
        for (y in 0u until height) {
            for (x in 0u until width) {
                target.putPixel(x, y, getPixel(x, y))
            }
        }
        return target
    }

    override fun view(x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> {
        require(x.toULong() + width.toULong() <= this.width.toULong()) { "View width out of bounds" }
        require(y.toULong() + height.toULong() <= this.height.toULong()) { "View height out of bounds" }
        return SubImage(image, xOffset + x, yOffset + y, width, height)
    }

    override fun subImage(x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> {
        require(x.toULong() + width.toULong() <= this.width.toULong()) { "SubImage width out of bounds" }
        require(y.toULong() + height.toULong() <= this.height.toULong()) { "SubImage height out of bounds" }
        return SubImage(image, xOffset + x, yOffset + y, width, height)
    }

    public fun xOffset(): UInt = xOffset

    public fun yOffset(): UInt = yOffset

    public fun xStride(): UInt = width

    public fun yStride(): UInt = height

    public fun getPixelMut(x: UInt, y: UInt): P = getPixel(x, y)

    public fun bufferWithDimensions(width: UInt, height: UInt): GenericImage<P> {
        val imgBuf = image as? ImageBuffer<P, *>
        return if (imgBuf != null) {
            imgBuf.bufferWithDimensions(width, height)
        } else {
            image.view(0u, 0u, width, height)
        }
    }

    public fun bufferLike(): GenericImage<P> = bufferWithDimensions(width, height)

    public fun colorSpace(): io.github.kotlinmania.image.metadata.Cicp =
        (image as? ImageBuffer<*, *>)?.colorSpace() ?: io.github.kotlinmania.image.metadata.Cicp.SRGB

    public companion object {
        public fun <P> new(image: GenericImageView<P>, x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> =
            SubImage(image, x, y, width, height)
    }
}

/**
 * The inner type of [SubImage] that holds positional offsets and strides.
 */
public data class SubImageInner<I>(
    public val image: I,
    public val xOffset: UInt,
    public val yOffset: UInt,
    public val xStride: UInt,
    public val yStride: UInt,
)

public typealias DerefPixel<I> = Any?
public typealias DerefSubpixel<I> = Any?
public typealias Target = Any?
