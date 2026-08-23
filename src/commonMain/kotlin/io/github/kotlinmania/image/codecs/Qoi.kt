// port-lint: source codecs/qoi.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.readExact
import io.github.kotlinmania.image.io.writeAll

private const val QOI_MAGIC_0 = 0x71.toByte() // 'q'
private const val QOI_MAGIC_1 = 0x6F.toByte() // 'o'
private const val QOI_MAGIC_2 = 0x69.toByte() // 'i'
private const val QOI_MAGIC_3 = 0x66.toByte() // 'f'

private const val QOI_OP_INDEX = 0x00
private const val QOI_OP_DIFF = 0x40
private const val QOI_OP_LUMA = 0x80
private const val QOI_OP_RUN = 0xC0
private const val QOI_OP_RGB = 0xFE
private const val QOI_OP_RGBA = 0xFF
private const val QOI_MASK_2 = 0xC0

private fun qoiColorHash(r: Int, g: Int, b: Int, a: Int): Int =
    (r * 3 + g * 5 + b * 7 + a * 11) % 64

/**
 * QOI image decoder.
 */
public class QoiDecoder internal constructor(
    private val reader: IoRead,
) : ImageDecoder {
    private val width: UInt
    private val height: UInt
    private val channels: Int
    private val colorspace: Int

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    init {
        val header = ByteArray(14)
        try {
            reader.readExact(header)
        } catch (e: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Qoi),
                    "Failed to read QOI header: ${e.message}",
                ),
            )
        }

        if (header[0] != QOI_MAGIC_0 ||
            header[1] != QOI_MAGIC_1 ||
            header[2] != QOI_MAGIC_2 ||
            header[3] != QOI_MAGIC_3
        ) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Qoi),
                    "Invalid QOI magic",
                ),
            )
        }

        val w =
            (
                (header[4].toInt() and 0xFF shl 24) or
                    (header[5].toInt() and 0xFF shl 16) or
                    (header[6].toInt() and 0xFF shl 8) or
                    (header[7].toInt() and 0xFF)
            ).toUInt()

        val h =
            (
                (header[8].toInt() and 0xFF shl 24) or
                    (header[9].toInt() and 0xFF shl 16) or
                    (header[10].toInt() and 0xFF shl 8) or
                    (header[11].toInt() and 0xFF)
            ).toUInt()

        val ch = header[12].toInt() and 0xFF
        val cs = header[13].toInt() and 0xFF

        if (ch != 3 && ch != 4) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Qoi),
                    "Invalid QOI channel count: $ch",
                ),
            )
        }

        width = w
        height = h
        channels = ch
        colorspace = cs
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType =
        if (channels == 3) ColorType.Rgb8 else ColorType.Rgba8

    override fun readImage(buf: ByteArray) {
        val totalPixels = (width * height).toInt()
        val expectedLen = totalPixels * channels
        require(buf.size >= expectedLen) {
            "Buffer size too small: expected $expectedLen, got ${buf.size}"
        }

        val index = Array(64) { IntArray(4) { 0 } }
        var r = 0
        var g = 0
        var b = 0
        var a = 255
        var run = 0
        var p = 0

        val byteBuf = ByteArray(1)
        for (pxIndex in 0 until totalPixels) {
            if (run > 0) {
                run--
            } else {
                reader.readExact(byteBuf)
                val b1 = byteBuf[0].toInt() and 0xFF

                if (b1 == QOI_OP_RGB) {
                    val rgbBuf = ByteArray(3)
                    reader.readExact(rgbBuf)
                    r = rgbBuf[0].toInt() and 0xFF
                    g = rgbBuf[1].toInt() and 0xFF
                    b = rgbBuf[2].toInt() and 0xFF
                } else if (b1 == QOI_OP_RGBA) {
                    val rgbaBuf = ByteArray(4)
                    reader.readExact(rgbaBuf)
                    r = rgbaBuf[0].toInt() and 0xFF
                    g = rgbaBuf[1].toInt() and 0xFF
                    b = rgbaBuf[2].toInt() and 0xFF
                    a = rgbaBuf[3].toInt() and 0xFF
                } else if ((b1 and QOI_MASK_2) == QOI_OP_INDEX) {
                    val idx = b1 and 0x3F
                    r = index[idx][0]
                    g = index[idx][1]
                    b = index[idx][2]
                    a = index[idx][3]
                } else if ((b1 and QOI_MASK_2) == QOI_OP_DIFF) {
                    val dr = ((b1 ushr 4) and 0x03) - 2
                    val dg = ((b1 ushr 2) and 0x03) - 2
                    val db = (b1 and 0x03) - 2
                    r = (r + dr) and 0xFF
                    g = (g + dg) and 0xFF
                    b = (b + db) and 0xFF
                } else if ((b1 and QOI_MASK_2) == QOI_OP_LUMA) {
                    reader.readExact(byteBuf)
                    val b2 = byteBuf[0].toInt() and 0xFF
                    val dg = (b1 and 0x3F) - 32
                    val drDg = ((b2 ushr 4) and 0x0F) - 8
                    val dbDg = (b2 and 0x0F) - 8
                    r = (r + dg + drDg) and 0xFF
                    g = (g + dg) and 0xFF
                    b = (b + dg + dbDg) and 0xFF
                } else if ((b1 and QOI_MASK_2) == QOI_OP_RUN) {
                    run = b1 and 0x3F
                }

                val idx = qoiColorHash(r, g, b, a)
                index[idx][0] = r
                index[idx][1] = g
                index[idx][2] = b
                index[idx][3] = a
            }

            buf[p++] = r.toByte()
            buf[p++] = g.toByte()
            buf[p++] = b.toByte()
            if (channels == 4) {
                buf[p++] = a.toByte()
            }
        }
    }
}

/**
 * QOI image encoder.
 */
public class QoiEncoder internal constructor(
    private val writer: IoWrite,
) : ImageEncoder {
    internal constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    /**
     * Encodes the image and writes to writer.
     */
    public fun encode(data: ByteArray, width: UInt, height: UInt, channels: Int = 4) {
        val totalPixels = (width * height).toInt()
        val expectedLen = totalPixels * channels
        require(data.size == expectedLen) {
            "Invalid buffer length: expected $expectedLen got ${data.size}"
        }

        val header = ByteArray(14)
        header[0] = QOI_MAGIC_0
        header[1] = QOI_MAGIC_1
        header[2] = QOI_MAGIC_2
        header[3] = QOI_MAGIC_3

        val w = width.toInt()
        header[4] = (w ushr 24).toByte()
        header[5] = (w ushr 16).toByte()
        header[6] = (w ushr 8).toByte()
        header[7] = w.toByte()

        val h = height.toInt()
        header[8] = (h ushr 24).toByte()
        header[9] = (h ushr 16).toByte()
        header[10] = (h ushr 8).toByte()
        header[11] = h.toByte()

        header[12] = channels.toByte()
        header[13] = 0 // sRGB

        writer.writeAll(header)

        val index = Array(64) { IntArray(4) { 0 } }
        var run = 0
        var prevR = 0
        var prevG = 0
        var prevB = 0
        var prevA = 255

        var p = 0
        for (i in 0 until totalPixels) {
            val r = data[p++].toInt() and 0xFF
            val g = data[p++].toInt() and 0xFF
            val b = data[p++].toInt() and 0xFF
            val a = if (channels == 4) data[p++].toInt() and 0xFF else 255

            if (r == prevR && g == prevG && b == prevB && a == prevA) {
                run++
                if (run == 62 || i == totalPixels - 1) {
                    writer.writeAll(byteArrayOf((QOI_OP_RUN or (run - 1)).toByte()))
                    run = 0
                }
            } else {
                if (run > 0) {
                    writer.writeAll(byteArrayOf((QOI_OP_RUN or (run - 1)).toByte()))
                    run = 0
                }

                val idx = qoiColorHash(r, g, b, a)
                if (index[idx][0] == r && index[idx][1] == g && index[idx][2] == b && index[idx][3] == a) {
                    writer.writeAll(byteArrayOf((QOI_OP_INDEX or idx).toByte()))
                } else {
                    index[idx][0] = r
                    index[idx][1] = g
                    index[idx][2] = b
                    index[idx][3] = a

                    if (a == prevA) {
                        val dr = ((r - prevR + 256) % 256).let { if (it > 127) it - 256 else it }
                        val dg = ((g - prevG + 256) % 256).let { if (it > 127) it - 256 else it }
                        val db = ((b - prevB + 256) % 256).let { if (it > 127) it - 256 else it }

                        val drDg = dr - dg
                        val dbDg = db - dg

                        if (dr in -2..1 && dg in -2..1 && db in -2..1) {
                            val byte = QOI_OP_DIFF or ((dr + 2) shl 4) or ((dg + 2) shl 2) or (db + 2)
                            writer.writeAll(byteArrayOf(byte.toByte()))
                        } else if (dg in -32..31 && drDg in -8..7 && dbDg in -8..7) {
                            val byte1 = QOI_OP_LUMA or (dg + 32)
                            val byte2 = ((drDg + 8) shl 4) or (dbDg + 8)
                            writer.writeAll(byteArrayOf(byte1.toByte(), byte2.toByte()))
                        } else {
                            writer.writeAll(byteArrayOf(QOI_OP_RGB.toByte(), r.toByte(), g.toByte(), b.toByte()))
                        }
                    } else {
                        writer.writeAll(byteArrayOf(QOI_OP_RGBA.toByte(), r.toByte(), g.toByte(), b.toByte(), a.toByte()))
                    }
                }

                prevR = r
                prevG = g
                prevB = b
                prevA = a
            }
        }

        // QOI end marker
        val endMarker = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1)
        writer.writeAll(endMarker)
    }

    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        val channels =
            when (colorType) {
                ExtendedColorType.Rgb8 -> 3
                ExtendedColorType.Rgba8 -> 4
                else -> throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Qoi),
                        UnsupportedErrorKind.Color(colorType),
                    ),
                )
            }
        encode(buf, width, height, channels)
    }
}
