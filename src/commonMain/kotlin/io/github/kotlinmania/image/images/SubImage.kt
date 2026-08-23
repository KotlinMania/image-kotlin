// port-lint: source images/sub_image.rs
package io.github.kotlinmania.image.images

/**
 * A view into a rectangular region of another image buffer.
 */
public class SubImage(
    private val buffer: ByteArray,
    private val parentWidth: UInt,
    private val parentHeight: UInt,
    private val channels: Int,
    private var xOffset: UInt,
    private var yOffset: UInt,
    private var width: UInt,
    private var height: UInt,
) {
    public fun offsets(): Pair<UInt, UInt> = Pair(xOffset, yOffset)

    public fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    public fun changeBounds(x: UInt, y: UInt, newWidth: UInt, newHeight: UInt) {
        require(x + newWidth <= parentWidth && y + newHeight <= parentHeight) {
            "SubImage bounds out of parent range"
        }
        xOffset = x
        yOffset = y
        width = newWidth
        height = newHeight
    }

    public fun getPixel(x: UInt, y: UInt): ByteArray {
        require(x < width && y < height) { "Coordinates ($x, $y) out of sub-image bounds ($width, $height)" }
        val absX = xOffset + x
        val absY = yOffset + y
        val idx = (absY.toLong() * parentWidth.toLong() + absX.toLong()) * channels
        return buffer.copyOfRange(idx.toInt(), idx.toInt() + channels)
    }

    public fun setPixel(x: UInt, y: UInt, pixel: ByteArray) {
        require(x < width && y < height) { "Coordinates ($x, $y) out of sub-image bounds ($width, $height)" }
        require(pixel.size == channels) { "Pixel channel size mismatch" }
        val absX = xOffset + x
        val absY = yOffset + y
        val idx = (absY.toLong() * parentWidth.toLong() + absX.toLong()) * channels
        pixel.copyInto(buffer, destinationOffset = idx.toInt())
    }

    public fun toByteArray(): ByteArray {
        val out = ByteArray((width * height).toInt() * channels)
        for (y in 0u until height) {
            for (x in 0u until width) {
                val px = getPixel(x, y)
                val outIdx = (y.toLong() * width.toLong() + x.toLong()) * channels
                px.copyInto(out, destinationOffset = outIdx.toInt())
            }
        }
        return out
    }
}
