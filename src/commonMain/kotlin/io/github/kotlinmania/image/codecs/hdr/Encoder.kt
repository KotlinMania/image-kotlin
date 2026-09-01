// port-lint: source codecs/hdr/encoder.rs
package io.github.kotlinmania.image.codecs.hdr

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.writeAll
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.truncate

/**
 * Pixel item type for HDR encoding sequences.
 */
public typealias Item = Rgbe8Pixel

/**
 * Radiance HDR encoder.
 */
public class HdrEncoder internal constructor(
    private val writer: IoWrite,
) : ImageEncoder {
    internal constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        when (colorType) {
            ExtendedColorType.Rgb32F -> {
                val bytesPerPixel = colorType.bitsPerPixel().toInt() / 8
                val w = width.toInt()
                val h = height.toInt()
                val pixelCount = w * h
                val rgbePixels = ArrayList<Rgbe8Pixel>(pixelCount)
                for (i in 0 until pixelCount) {
                    val offset = i * bytesPerPixel
                    val b0 = buf[offset].toInt() and 0xFF
                    val b1 = buf[offset + 1].toInt() and 0xFF
                    val b2 = buf[offset + 2].toInt() and 0xFF
                    val b3 = buf[offset + 3].toInt() and 0xFF
                    val r = Float.fromBits(b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24))

                    val b4 = buf[offset + 4].toInt() and 0xFF
                    val b5 = buf[offset + 5].toInt() and 0xFF
                    val b6 = buf[offset + 6].toInt() and 0xFF
                    val b7 = buf[offset + 7].toInt() and 0xFF
                    val g = Float.fromBits(b4 or (b5 shl 8) or (b6 shl 16) or (b7 shl 24))

                    val b8 = buf[offset + 8].toInt() and 0xFF
                    val b9 = buf[offset + 9].toInt() and 0xFF
                    val b10 = buf[offset + 10].toInt() and 0xFF
                    val b11 = buf[offset + 11].toInt() and 0xFF
                    val b = Float.fromBits(b8 or (b9 shl 8) or (b10 shl 16) or (b11 shl 24))

                    rgbePixels.add(toRgbe8(Rgb(r, g, b)))
                }
                encodePixels(rgbePixels, w, h)
            }
            else -> {
                throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Hdr),
                        UnsupportedErrorKind.Color(colorType),
                    ),
                )
            }
        }
    }

    /**
     * Encodes the image [buf] that has dimensions [width] and [height] and [ExtendedColorType] [colorType].
     */
    public fun encode(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        writeImage(buf, width, height, colorType)
    }

    /**
     * Encodes the image [rgb] that has dimensions [width] and [height].
     */
    internal fun encode(rgb: List<Rgb<Float>>, width: Int, height: Int) {
        val pixels = ArrayList<Rgbe8Pixel>(rgb.size)
        for (p in rgb) {
            pixels.add(toRgbe8(p))
        }
        encodePixels(pixels, width, height)
    }

    /**
     * Encodes the image [flattenedRgbePixels] that has dimensions [width] and [height].
     */
    public fun encodePixels(
        flattenedRgbePixels: List<Rgbe8Pixel>,
        width: Int,
        height: Int,
    ) {
        require(flattenedRgbePixels.size >= width * height) {
            "not enough pixels provided: have ${flattenedRgbePixels.size}, need ${width * height}"
        }

        writer.writeAll(SIGNATURE)
        writer.writeAll("\n".encodeToByteArray())
        writer.writeAll("# Rust HDR encoder\n".encodeToByteArray())
        writer.writeAll("FORMAT=32-bit_rle_rgbe\n\n".encodeToByteArray())
        writer.writeAll("-Y $height +X $width\n".encodeToByteArray())

        if (width !in 8..32768) {
            for (pixel in flattenedRgbePixels) {
                writeRgbe8(writer, pixel)
            }
        } else {
            val marker = rgbe8(2, 2, width / 256, width % 256)
            val bufr = ByteArray(width)
            val bufg = ByteArray(width)
            val bufb = ByteArray(width)
            val bufe = ByteArray(width)
            val rleBuf = ArrayList<Byte>(width)

            var pixelIndex = 0
            for (scanlineIndex in 0 until height) {
                for (x in 0 until width) {
                    val pixel = flattenedRgbePixels[pixelIndex++]
                    bufr[x] = pixel.r.toByte()
                    bufg[x] = pixel.g.toByte()
                    bufb[x] = pixel.b.toByte()
                    bufe[x] = pixel.e.toByte()
                }

                writeRgbe8(writer, marker)

                rleBuf.clear()
                rleCompress(bufr, rleBuf)
                writer.writeAll(rleBuf.toByteArray())

                rleBuf.clear()
                rleCompress(bufg, rleBuf)
                writer.writeAll(rleBuf.toByteArray())

                rleBuf.clear()
                rleCompress(bufb, rleBuf)
                writer.writeAll(rleBuf.toByteArray())

                rleBuf.clear()
                rleCompress(bufe, rleBuf)
                writer.writeAll(rleBuf.toByteArray())
            }
        }
    }

    public companion object {
        /**
         * Creates encoder.
         */
        public fun new(w: IoWrite): HdrEncoder = HdrEncoder(w)
    }
}

internal sealed class RunOrNot {
    data class Run(
        val c: UByte,
        val len: Int,
    ) : RunOrNot()

    data class Norun(
        val idx: Int,
        val len: Int,
    ) : RunOrNot()
}

private const val RUN_MAX_LEN: Int = 127
private const val NORUN_MAX_LEN: Int = 128

internal class RunIterator(
    private val data: ByteArray,
) {
    private var curidx: Int = 0

    fun next(): RunOrNot? {
        if (curidx == data.size) {
            return null
        }
        val cv = data[curidx]
        var crun = 0
        while (curidx + crun < data.size && crun < RUN_MAX_LEN && data[curidx + crun] == cv) {
            crun++
        }
        val ret =
            if (crun > 2) {
                RunOrNot.Run(cv.toUByte(), crun)
            } else {
                RunOrNot.Norun(curidx, crun)
            }
        curidx += crun
        return ret
    }
}

internal class NorunCombineIterator(
    data: ByteArray,
) {
    private val runiter: RunIterator = RunIterator(data)
    private var prev: RunOrNot? = null

    fun next(): RunOrNot? {
        while (true) {
            val p = prev
            prev = null
            if (p != null) {
                when (p) {
                    is RunOrNot.Run -> {
                        return p
                    }
                    is RunOrNot.Norun -> {
                        when (val nextItem = runiter.next()) {
                            is RunOrNot.Norun -> {
                                val clen = p.len + nextItem.len
                                when {
                                    clen == NORUN_MAX_LEN -> return RunOrNot.Norun(p.idx, clen)
                                    clen > NORUN_MAX_LEN -> {
                                        prev = RunOrNot.Norun(p.idx + NORUN_MAX_LEN, clen - NORUN_MAX_LEN)
                                        return RunOrNot.Norun(p.idx, NORUN_MAX_LEN)
                                    }
                                    else -> {
                                        prev = RunOrNot.Norun(p.idx, p.len + nextItem.len)
                                    }
                                }
                            }
                            is RunOrNot.Run -> {
                                prev = nextItem
                                return RunOrNot.Norun(p.idx, p.len)
                            }
                            null -> {
                                return RunOrNot.Norun(p.idx, p.len)
                            }
                        }
                    }
                }
            } else {
                when (val nextItem = runiter.next()) {
                    is RunOrNot.Norun -> {
                        prev = RunOrNot.Norun(nextItem.idx, nextItem.len)
                    }
                    is RunOrNot.Run -> {
                        return nextItem
                    }
                    null -> {
                        return null
                    }
                }
            }
        }
    }
}

internal fun rleCompress(data: ByteArray, rle: MutableList<Byte>) {
    rle.clear()
    if (data.isEmpty()) {
        rle.add(0)
        return
    }
    val iterator = NorunCombineIterator(data)
    while (true) {
        val rnr = iterator.next() ?: break
        when (rnr) {
            is RunOrNot.Run -> {
                check(rnr.len <= 127)
                rle.add((128 + rnr.len).toByte())
                rle.add(rnr.c.toByte())
            }
            is RunOrNot.Norun -> {
                check(rnr.len <= 128)
                rle.add(rnr.len.toByte())
                for (i in 0 until rnr.len) {
                    rle.add(data[rnr.idx + i])
                }
            }
        }
    }
}

internal fun writeRgbe8(w: IoWrite, v: Rgbe8Pixel) {
    w.writeAll(byteArrayOf(v.r.toByte(), v.g.toByte(), v.b.toByte(), v.e.toByte()))
}

/**
 * Converts `Rgb<Float>` into `Rgbe8Pixel`.
 */
public fun toRgbe8(pix: Rgb<Float>): Rgbe8Pixel {
    val mx = maxOf(pix.r, maxOf(pix.g, pix.b))
    return if (mx <= 0.0f) {
        Rgbe8Pixel(0u, 0u, 0u, 0u)
    } else {
        val exp = floor(log2(mx)).toInt() + 1
        val mul = 2.0.pow(exp).toFloat()
        val cr = truncate(pix.r / mul * 256.0f).toInt().coerceIn(0, 255).toUByte()
        val cg = truncate(pix.g / mul * 256.0f).toInt().coerceIn(0, 255).toUByte()
        val cb = truncate(pix.b / mul * 256.0f).toInt().coerceIn(0, 255).toUByte()
        Rgbe8Pixel(
            r = cr,
            g = cg,
            b = cb,
            e = (exp + 128).toUByte(),
        )
    }
}
