// port-lint: source images/sub_image.rs
package io.github.kotlinmania.image.images

/**
 * A view into a rectangular region of another image buffer.
 */
public class SubImage<P>(
    private val image: GenericImage<P>,
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
        image.putPixel(xOffset + x, yOffset + y, pixel)
    }

    override fun blendPixel(x: UInt, y: UInt, pixel: P) {
        require(x < width && y < height) { "Coordinates ($x, $y) out of sub-image bounds ($width, $height)" }
        image.blendPixel(xOffset + x, yOffset + y, pixel)
    }

    public fun inner(): GenericImage<P> = image

    public fun innerMut(): GenericImage<P> = image

    public fun deref(): GenericImage<P> = image

    public fun derefMut(): GenericImage<P> = image

    @Suppress("UNCHECKED_CAST")
    public fun toImage(): GenericImage<P> {
        val target =
            (image as? ImageBuffer<P, *>)?.bufferWithDimensions(width, height)
                ?: (ImageBuffer.createRgba(width, height) as GenericImage<P>)
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

    public companion object {
        public fun <P> new(image: GenericImage<P>, x: UInt, y: UInt, width: UInt, height: UInt): SubImage<P> =
            SubImage(image, x, y, width, height)
    }
}
