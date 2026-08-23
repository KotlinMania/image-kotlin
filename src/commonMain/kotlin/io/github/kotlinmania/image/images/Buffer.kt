// port-lint: source images/buffer.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.LumaA
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.blendUByte
import io.github.kotlinmania.image.math.Rect
import io.github.kotlinmania.image.metadata.Cicp
import io.github.kotlinmania.image.metadata.CicpColorPrimaries
import io.github.kotlinmania.image.metadata.CicpRgb
import io.github.kotlinmania.image.metadata.CicpTransferCharacteristics

/**
 * Generic image buffer parameterized by its Pixel type and container.
 */
public class ImageBuffer<P, Container>(
    public val width: UInt,
    public val height: UInt,
    public val data: ByteArray,
    private val channelCount: Int,
    private val pixelReader: (ByteArray, Int) -> P,
    private val pixelWriter: (ByteArray, Int, P) -> Unit,
    private val pixelBlender: (ByteArray, Int, P) -> Unit = pixelWriter,
) : GenericImage<P> {
    public var color: CicpRgb = Cicp.SRGB.intoRgb()

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun width(): UInt = width

    override fun height(): UInt = height

    public fun asRaw(): ByteArray = data

    public fun intoRaw(): ByteArray = data

    public fun clone(): ImageBuffer<P, ByteArray> {
        val copyData = data.copyOf()
        val copy = ImageBuffer<P, ByteArray>(width, height, copyData, channelCount, pixelReader, pixelWriter, pixelBlender)
        copy.color = color
        return copy
    }

    public fun bufferWithDimensions(w: UInt, h: UInt): ImageBuffer<P, ByteArray> {
        val buf = ByteArray((w.toLong() * h.toLong() * channelCount.toLong()).toInt())
        val img = ImageBuffer<P, ByteArray>(w, h, buf, channelCount, pixelReader, pixelWriter, pixelBlender)
        img.color = color
        return img
    }

    public fun setRgbPrimaries(primaries: CicpColorPrimaries) {
        color = CicpRgb(primaries, color.transfer, color.luminance)
    }

    public fun setTransferFunction(transfer: CicpTransferCharacteristics) {
        color = CicpRgb(color.primaries, transfer, color.luminance)
    }

    public fun colorSpace(): Cicp = color.toCicp()

    public fun setColorSpace(cicp: Cicp) {
        color = cicp.intoRgb()
    }

    override fun getPixel(x: UInt, y: UInt): P {
        require(x < width && y < height) { "Image index ($x, $y) out of bounds ($width, $height)" }
        val idx = (y.toInt() * width.toInt() + x.toInt()) * channelCount
        return pixelReader(data, idx)
    }

    public fun getPixelChecked(x: UInt, y: UInt): P? {
        if (x >= width || y >= height) return null
        val idx = (y.toInt() * width.toInt() + x.toInt()) * channelCount
        return pixelReader(data, idx)
    }

    override fun putPixel(x: UInt, y: UInt, pixel: P) {
        require(x < width && y < height) { "Image index ($x, $y) out of bounds ($width, $height)" }
        val idx = (y.toInt() * width.toInt() + x.toInt()) * channelCount
        pixelWriter(data, idx, pixel)
    }

    override fun blendPixel(x: UInt, y: UInt, pixel: P) {
        require(x < width && y < height) { "Image index ($x, $y) out of bounds ($width, $height)" }
        val idx = (y.toInt() * width.toInt() + x.toInt()) * channelCount
        pixelBlender(data, idx, pixel)
    }

    public fun pixelsMut(): MutableList<P> {
        val total = (width * height).toInt()
        val list = ArrayList<P>(total)
        for (i in 0 until total) {
            list.add(pixelReader(data, i * channelCount))
        }
        return list
    }

    public fun rows(): List<List<P>> {
        if (width == 0u || height == 0u) return emptyList()
        val result = ArrayList<List<P>>(height.toInt())
        for (y in 0u until height) {
            val row = ArrayList<P>(width.toInt())
            for (x in 0u until width) {
                row.add(getPixel(x, y))
            }
            result.add(row)
        }
        return result
    }

    public fun rowsMut(): List<List<P>> = rows()

    public fun enumeratePixels(): List<Triple<UInt, UInt, P>> {
        val total = (width * height).toInt()
        val result = ArrayList<Triple<UInt, UInt, P>>(total)
        for (y in 0u until height) {
            for (x in 0u until width) {
                result.add(Triple(x, y, getPixel(x, y)))
            }
        }
        return result
    }

    public fun enumeratePixelsMut(): List<Triple<UInt, UInt, P>> = enumeratePixels()

    public fun enumerateRows(): List<Pair<UInt, List<P>>> {
        val r = rows()
        return r.mapIndexed { idx, row -> Pair(idx.toUInt(), row) }
    }

    public fun enumerateRowsMut(): List<Pair<UInt, List<P>>> = enumerateRows()

    override fun copyWithin(source: Rect, x: UInt, y: UInt): Boolean {
        val sx = source.x
        val sy = source.y
        val sw = source.width
        val sh = source.height
        val dx = x
        val dy = y

        if (sx >= width || dx >= width || sy >= height || dy >= height) {
            return false
        }

        val maxW = if (dx > sx) dx else sx
        val maxH = if (dy > sy) dy else sy
        if (width - maxW < sw || height - maxH < sh) {
            return false
        }

        val channels = channelCount
        if (sy < dy) {
            for (row in (sh - 1u) downTo 0u) {
                val srcRow = sy + row
                val dstRow = dy + row
                if (sx < dx) {
                    for (col in (sw - 1u) downTo 0u) {
                        val srcIdx = (srcRow.toInt() * width.toInt() + (sx + col).toInt()) * channels
                        val dstIdx = (dstRow.toInt() * width.toInt() + (dx + col).toInt()) * channels
                        for (c in 0 until channels) {
                            data[dstIdx + c] = data[srcIdx + c]
                        }
                    }
                } else {
                    for (col in 0u until sw) {
                        val srcIdx = (srcRow.toInt() * width.toInt() + (sx + col).toInt()) * channels
                        val dstIdx = (dstRow.toInt() * width.toInt() + (dx + col).toInt()) * channels
                        for (c in 0 until channels) {
                            data[dstIdx + c] = data[srcIdx + c]
                        }
                    }
                }
            }
        } else {
            for (row in 0u until sh) {
                val srcRow = sy + row
                val dstRow = dy + row
                if (sx < dx) {
                    for (col in (sw - 1u) downTo 0u) {
                        val srcIdx = (srcRow.toInt() * width.toInt() + (sx + col).toInt()) * channels
                        val dstIdx = (dstRow.toInt() * width.toInt() + (dx + col).toInt()) * channels
                        for (c in 0 until channels) {
                            data[dstIdx + c] = data[srcIdx + c]
                        }
                    }
                } else {
                    for (col in 0u until sw) {
                        val srcIdx = (srcRow.toInt() * width.toInt() + (sx + col).toInt()) * channels
                        val dstIdx = (dstRow.toInt() * width.toInt() + (dx + col).toInt()) * channels
                        for (c in 0 until channels) {
                            data[dstIdx + c] = data[srcIdx + c]
                        }
                    }
                }
            }
        }
        return true
    }

    public companion object {
        public fun <P> new(
            width: UInt,
            height: UInt,
            channels: Int,
            reader: (ByteArray, Int) -> P,
            writer: (ByteArray, Int, P) -> Unit,
            blender: (ByteArray, Int, P) -> Unit = writer,
        ): ImageBuffer<P, ByteArray> {
            val total = (width.toLong() * height.toLong() * channels.toLong()).toInt()
            val buf = ByteArray(total)
            return ImageBuffer(width, height, buf, channels, reader, writer, blender)
        }

        public fun <P> fromRaw(
            width: UInt,
            height: UInt,
            buf: ByteArray,
            channels: Int,
            reader: (ByteArray, Int) -> P,
            writer: (ByteArray, Int, P) -> Unit,
            blender: (ByteArray, Int, P) -> Unit = writer,
        ): ImageBuffer<P, ByteArray>? {
            val expected = width.toLong() * height.toLong() * channels.toLong()
            if (buf.size.toLong() < expected) return null
            return ImageBuffer(width, height, buf, channels, reader, writer, blender)
        }

        public fun createRgb(width: UInt, height: UInt): RgbImage =
            new(
                width,
                height,
                3,
                { arr, idx -> Rgb(arr[idx].toUByte(), arr[idx + 1].toUByte(), arr[idx + 2].toUByte()) },
                { arr, idx, p ->
                    arr[idx] = p.r.toByte()
                    arr[idx + 1] = p.g.toByte()
                    arr[idx + 2] = p.b.toByte()
                },
            )

        public fun createRgb(width: UInt, height: UInt, buf: ByteArray): RgbImage? =
            fromRaw(
                width,
                height,
                buf,
                3,
                { arr, idx -> Rgb(arr[idx].toUByte(), arr[idx + 1].toUByte(), arr[idx + 2].toUByte()) },
                { arr, idx, p ->
                    arr[idx] = p.r.toByte()
                    arr[idx + 1] = p.g.toByte()
                    arr[idx + 2] = p.b.toByte()
                },
            )

        public fun createRgba(width: UInt, height: UInt): RgbaImage =
            new(
                width,
                height,
                4,
                { arr, idx -> Rgba(arr[idx].toUByte(), arr[idx + 1].toUByte(), arr[idx + 2].toUByte(), arr[idx + 3].toUByte()) },
                { arr, idx, p ->
                    arr[idx] = p.r.toByte()
                    arr[idx + 1] = p.g.toByte()
                    arr[idx + 2] = p.b.toByte()
                    arr[idx + 3] = p.a.toByte()
                },
                { arr, idx, p ->
                    val cur = Rgba(arr[idx].toUByte(), arr[idx + 1].toUByte(), arr[idx + 2].toUByte(), arr[idx + 3].toUByte())
                    cur.blendUByte(p)
                    arr[idx] = cur.r.toByte()
                    arr[idx + 1] = cur.g.toByte()
                    arr[idx + 2] = cur.b.toByte()
                    arr[idx + 3] = cur.a.toByte()
                },
            )

        public fun createRgba(width: UInt, height: UInt, buf: ByteArray): RgbaImage? =
            fromRaw(
                width,
                height,
                buf,
                4,
                { arr, idx -> Rgba(arr[idx].toUByte(), arr[idx + 1].toUByte(), arr[idx + 2].toUByte(), arr[idx + 3].toUByte()) },
                { arr, idx, p ->
                    arr[idx] = p.r.toByte()
                    arr[idx + 1] = p.g.toByte()
                    arr[idx + 2] = p.b.toByte()
                    arr[idx + 3] = p.a.toByte()
                },
                { arr, idx, p ->
                    val cur = Rgba(arr[idx].toUByte(), arr[idx + 1].toUByte(), arr[idx + 2].toUByte(), arr[idx + 3].toUByte())
                    cur.blendUByte(p)
                    arr[idx] = cur.r.toByte()
                    arr[idx + 1] = cur.g.toByte()
                    arr[idx + 2] = cur.b.toByte()
                    arr[idx + 3] = cur.a.toByte()
                },
            )

        public fun createGray(width: UInt, height: UInt): GrayImage =
            new(
                width,
                height,
                1,
                { arr, idx -> Luma(arr[idx].toUByte()) },
                { arr, idx, p -> arr[idx] = p.l.toByte() },
            )

        public fun createGray(width: UInt, height: UInt, buf: ByteArray): GrayImage? =
            fromRaw(
                width,
                height,
                buf,
                1,
                { arr, idx -> Luma(arr[idx].toUByte()) },
                { arr, idx, p -> arr[idx] = p.l.toByte() },
            )

        public fun createGrayAlpha(width: UInt, height: UInt): GrayAlphaImage =
            new(
                width,
                height,
                2,
                { arr, idx -> LumaA(arr[idx].toUByte(), arr[idx + 1].toUByte()) },
                { arr, idx, p ->
                    arr[idx] = p.l.toByte()
                    arr[idx + 1] = p.a.toByte()
                },
                { arr, idx, p ->
                    val cur = LumaA(arr[idx].toUByte(), arr[idx + 1].toUByte())
                    cur.blendUByte(p)
                    arr[idx] = cur.l.toByte()
                    arr[idx + 1] = cur.a.toByte()
                },
            )

        public fun createGrayAlpha(width: UInt, height: UInt, buf: ByteArray): GrayAlphaImage? =
            fromRaw(
                width,
                height,
                buf,
                2,
                { arr, idx -> LumaA(arr[idx].toUByte(), arr[idx + 1].toUByte()) },
                { arr, idx, p ->
                    arr[idx] = p.l.toByte()
                    arr[idx + 1] = p.a.toByte()
                },
                { arr, idx, p ->
                    val cur = LumaA(arr[idx].toUByte(), arr[idx + 1].toUByte())
                    cur.blendUByte(p)
                    arr[idx] = cur.l.toByte()
                    arr[idx + 1] = cur.a.toByte()
                },
            )
    }
}

public typealias RgbImage = ImageBuffer<Rgb<UByte>, ByteArray>
public typealias RgbaImage = ImageBuffer<Rgba<UByte>, ByteArray>
public typealias GrayImage = ImageBuffer<Luma<UByte>, ByteArray>
public typealias GrayAlphaImage = ImageBuffer<LumaA<UByte>, ByteArray>
