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
    private val width: UInt,
    private val height: UInt,
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

    public fun pixelsMut(): List<P> {
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

        public fun <P> fromFn(
            width: UInt,
            height: UInt,
            channels: Int,
            reader: (ByteArray, Int) -> P,
            writer: (ByteArray, Int, P) -> Unit,
            blender: (ByteArray, Int, P) -> Unit = writer,
            f: (UInt, UInt) -> P,
        ): ImageBuffer<P, ByteArray> {
            val img = new(width, height, channels, reader, writer, blender)
            for (y in 0u until height) {
                for (x in 0u until width) {
                    img.putPixel(x, y, f(x, y))
                }
            }
            return img
        }

        public fun createRgb(width: UInt, height: UInt, f: (UInt, UInt) -> Rgb<UByte>): RgbImage =
            fromFn(
                width,
                height,
                3,
                { arr, idx -> Rgb(arr[idx].toUByte(), arr[idx + 1].toUByte(), arr[idx + 2].toUByte()) },
                { arr, idx, p ->
                    arr[idx] = p.r.toByte()
                    arr[idx + 1] = p.g.toByte()
                    arr[idx + 2] = p.b.toByte()
                },
                f = f,
            )

        public fun createRgba(width: UInt, height: UInt, f: (UInt, UInt) -> Rgba<UByte>): RgbaImage =
            fromFn(
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
                f = f,
            )

        public fun createGray(width: UInt, height: UInt, f: (UInt, UInt) -> Luma<UByte>): GrayImage =
            fromFn(
                width,
                height,
                1,
                { arr, idx -> Luma(arr[idx].toUByte()) },
                { arr, idx, p -> arr[idx] = p.l.toByte() },
                f = f,
            )

        public fun createGrayAlpha(width: UInt, height: UInt, f: (UInt, UInt) -> LumaA<UByte>): GrayAlphaImage =
            fromFn(
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
                f = f,
            )

        public fun createRgb16(width: UInt, height: UInt): Rgb16Image =
            new(
                width,
                height,
                6,
                { arr, idx -> Rgb(readU16LE(arr, idx), readU16LE(arr, idx + 2), readU16LE(arr, idx + 4)) },
                { arr, idx, p ->
                    writeU16LE(arr, idx, p.r)
                    writeU16LE(arr, idx + 2, p.g)
                    writeU16LE(arr, idx + 4, p.b)
                },
            )

        public fun createRgb16(width: UInt, height: UInt, buf: ByteArray): Rgb16Image? =
            fromRaw(
                width,
                height,
                buf,
                6,
                { arr, idx -> Rgb(readU16LE(arr, idx), readU16LE(arr, idx + 2), readU16LE(arr, idx + 4)) },
                { arr, idx, p ->
                    writeU16LE(arr, idx, p.r)
                    writeU16LE(arr, idx + 2, p.g)
                    writeU16LE(arr, idx + 4, p.b)
                },
            )

        public fun createRgba16(width: UInt, height: UInt): Rgba16Image =
            new(
                width,
                height,
                8,
                { arr, idx -> Rgba(readU16LE(arr, idx), readU16LE(arr, idx + 2), readU16LE(arr, idx + 4), readU16LE(arr, idx + 6)) },
                { arr, idx, p ->
                    writeU16LE(arr, idx, p.r)
                    writeU16LE(arr, idx + 2, p.g)
                    writeU16LE(arr, idx + 4, p.b)
                    writeU16LE(arr, idx + 6, p.a)
                },
            )

        public fun createRgba16(width: UInt, height: UInt, buf: ByteArray): Rgba16Image? =
            fromRaw(
                width,
                height,
                buf,
                8,
                { arr, idx -> Rgba(readU16LE(arr, idx), readU16LE(arr, idx + 2), readU16LE(arr, idx + 4), readU16LE(arr, idx + 6)) },
                { arr, idx, p ->
                    writeU16LE(arr, idx, p.r)
                    writeU16LE(arr, idx + 2, p.g)
                    writeU16LE(arr, idx + 4, p.b)
                    writeU16LE(arr, idx + 6, p.a)
                },
            )

        public fun createGray16(width: UInt, height: UInt): Gray16Image =
            new(
                width,
                height,
                2,
                { arr, idx -> Luma(readU16LE(arr, idx)) },
                { arr, idx, p -> writeU16LE(arr, idx, p.l) },
            )

        public fun createGray16(width: UInt, height: UInt, buf: ByteArray): Gray16Image? =
            fromRaw(
                width,
                height,
                buf,
                2,
                { arr, idx -> Luma(readU16LE(arr, idx)) },
                { arr, idx, p -> writeU16LE(arr, idx, p.l) },
            )

        public fun createGrayAlpha16(width: UInt, height: UInt): GrayAlpha16Image =
            new(
                width,
                height,
                4,
                { arr, idx -> LumaA(readU16LE(arr, idx), readU16LE(arr, idx + 2)) },
                { arr, idx, p ->
                    writeU16LE(arr, idx, p.l)
                    writeU16LE(arr, idx + 2, p.a)
                },
            )

        public fun createGrayAlpha16(width: UInt, height: UInt, buf: ByteArray): GrayAlpha16Image? =
            fromRaw(
                width,
                height,
                buf,
                4,
                { arr, idx -> LumaA(readU16LE(arr, idx), readU16LE(arr, idx + 2)) },
                { arr, idx, p ->
                    writeU16LE(arr, idx, p.l)
                    writeU16LE(arr, idx + 2, p.a)
                },
            )

        public fun createRgb32F(width: UInt, height: UInt): Rgb32FImage =
            new(
                width,
                height,
                12,
                { arr, idx -> Rgb(readF32LE(arr, idx), readF32LE(arr, idx + 4), readF32LE(arr, idx + 8)) },
                { arr, idx, p ->
                    writeF32LE(arr, idx, p.r)
                    writeF32LE(arr, idx + 4, p.g)
                    writeF32LE(arr, idx + 8, p.b)
                },
            )

        public fun createRgb32F(width: UInt, height: UInt, buf: ByteArray): Rgb32FImage? =
            fromRaw(
                width,
                height,
                buf,
                12,
                { arr, idx -> Rgb(readF32LE(arr, idx), readF32LE(arr, idx + 4), readF32LE(arr, idx + 8)) },
                { arr, idx, p ->
                    writeF32LE(arr, idx, p.r)
                    writeF32LE(arr, idx + 4, p.g)
                    writeF32LE(arr, idx + 8, p.b)
                },
            )

        public fun createRgba32F(width: UInt, height: UInt): Rgba32FImage =
            new(
                width,
                height,
                16,
                { arr, idx -> Rgba(readF32LE(arr, idx), readF32LE(arr, idx + 4), readF32LE(arr, idx + 8), readF32LE(arr, idx + 12)) },
                { arr, idx, p ->
                    writeF32LE(arr, idx, p.r)
                    writeF32LE(arr, idx + 4, p.g)
                    writeF32LE(arr, idx + 8, p.b)
                    writeF32LE(arr, idx + 12, p.a)
                },
            )

        public fun createRgba32F(width: UInt, height: UInt, buf: ByteArray): Rgba32FImage? =
            fromRaw(
                width,
                height,
                buf,
                16,
                { arr, idx -> Rgba(readF32LE(arr, idx), readF32LE(arr, idx + 4), readF32LE(arr, idx + 8), readF32LE(arr, idx + 12)) },
                { arr, idx, p ->
                    writeF32LE(arr, idx, p.r)
                    writeF32LE(arr, idx + 4, p.g)
                    writeF32LE(arr, idx + 8, p.b)
                    writeF32LE(arr, idx + 12, p.a)
                },
            )
    }

    public fun copy(): ImageBuffer<P, ByteArray> = clone()
}

public fun createRgb(width: UInt, height: UInt): RgbImage = ImageBuffer.createRgb(width, height)

public fun createRgb(width: UInt, height: UInt, buf: ByteArray): RgbImage =
    ImageBuffer.createRgb(width, height, buf) ?: ImageBuffer.createRgb(width, height)

public fun createRgba(width: UInt, height: UInt): RgbaImage = ImageBuffer.createRgba(width, height)

public fun createRgba(width: UInt, height: UInt, buf: ByteArray): RgbaImage =
    ImageBuffer.createRgba(width, height, buf) ?: ImageBuffer.createRgba(width, height)

public fun createGray(width: UInt, height: UInt): GrayImage = ImageBuffer.createGray(width, height)

public fun createGray(width: UInt, height: UInt, buf: ByteArray): GrayImage =
    ImageBuffer.createGray(width, height, buf) ?: ImageBuffer.createGray(width, height)

public fun createGrayAlpha(width: UInt, height: UInt): GrayAlphaImage = ImageBuffer.createGrayAlpha(width, height)

public fun createGrayAlpha(width: UInt, height: UInt, buf: ByteArray): GrayAlphaImage =
    ImageBuffer.createGrayAlpha(width, height, buf) ?: ImageBuffer.createGrayAlpha(width, height)

public fun createRgb16(width: UInt, height: UInt): Rgb16Image = ImageBuffer.createRgb16(width, height)

public fun createRgb16(width: UInt, height: UInt, buf: ByteArray): Rgb16Image =
    ImageBuffer.createRgb16(width, height, buf) ?: ImageBuffer.createRgb16(width, height)

public fun createRgba16(width: UInt, height: UInt): Rgba16Image = ImageBuffer.createRgba16(width, height)

public fun createRgba16(width: UInt, height: UInt, buf: ByteArray): Rgba16Image =
    ImageBuffer.createRgba16(width, height, buf) ?: ImageBuffer.createRgba16(width, height)

public fun createGray16(width: UInt, height: UInt): Gray16Image = ImageBuffer.createGray16(width, height)

public fun createGray16(width: UInt, height: UInt, buf: ByteArray): Gray16Image =
    ImageBuffer.createGray16(width, height, buf) ?: ImageBuffer.createGray16(width, height)

public fun createGrayAlpha16(width: UInt, height: UInt): GrayAlpha16Image = ImageBuffer.createGrayAlpha16(width, height)

public fun createGrayAlpha16(width: UInt, height: UInt, buf: ByteArray): GrayAlpha16Image =
    ImageBuffer.createGrayAlpha16(width, height, buf) ?: ImageBuffer.createGrayAlpha16(width, height)

public fun createRgb32F(width: UInt, height: UInt): Rgb32FImage = ImageBuffer.createRgb32F(width, height)

public fun createRgb32F(width: UInt, height: UInt, buf: ByteArray): Rgb32FImage =
    ImageBuffer.createRgb32F(width, height, buf) ?: ImageBuffer.createRgb32F(width, height)

public fun createRgba32F(width: UInt, height: UInt): Rgba32FImage = ImageBuffer.createRgba32F(width, height)

public fun createRgba32F(width: UInt, height: UInt, buf: ByteArray): Rgba32FImage =
    ImageBuffer.createRgba32F(width, height, buf) ?: ImageBuffer.createRgba32F(width, height)

private fun readU16LE(arr: ByteArray, idx: Int): UShort =
    ((arr[idx].toInt() and 0xFF) or ((arr[idx + 1].toInt() and 0xFF) shl 8)).toUShort()

private fun writeU16LE(arr: ByteArray, idx: Int, v: UShort) {
    val vInt = v.toInt()
    arr[idx] = (vInt and 0xFF).toByte()
    arr[idx + 1] = ((vInt ushr 8) and 0xFF).toByte()
}

private fun readF32LE(arr: ByteArray, idx: Int): Float {
    val bits =
        (arr[idx].toInt() and 0xFF) or
            ((arr[idx + 1].toInt() and 0xFF) shl 8) or
            ((arr[idx + 2].toInt() and 0xFF) shl 16) or
            ((arr[idx + 3].toInt() and 0xFF) shl 24)
    return Float.fromBits(bits)
}

private fun writeF32LE(arr: ByteArray, idx: Int, v: Float) {
    val bits = v.toRawBits()
    arr[idx] = (bits and 0xFF).toByte()
    arr[idx + 1] = ((bits ushr 8) and 0xFF).toByte()
    arr[idx + 2] = ((bits ushr 16) and 0xFF).toByte()
    arr[idx + 3] = ((bits ushr 24) and 0xFF).toByte()
}

public typealias RgbImage = ImageBuffer<Rgb<UByte>, ByteArray>
public typealias RgbaImage = ImageBuffer<Rgba<UByte>, ByteArray>
public typealias GrayImage = ImageBuffer<Luma<UByte>, ByteArray>
public typealias GrayAlphaImage = ImageBuffer<LumaA<UByte>, ByteArray>

public typealias Rgb16Image = ImageBuffer<Rgb<UShort>, ByteArray>
public typealias Rgba16Image = ImageBuffer<Rgba<UShort>, ByteArray>
public typealias Gray16Image = ImageBuffer<Luma<UShort>, ByteArray>
public typealias GrayAlpha16Image = ImageBuffer<LumaA<UShort>, ByteArray>

public typealias Rgb32FImage = ImageBuffer<Rgb<Float>, ByteArray>
public typealias Rgba32FImage = ImageBuffer<Rgba<Float>, ByteArray>
