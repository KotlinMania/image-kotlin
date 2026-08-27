// port-lint: source codecs/pnm/encoder.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.writeAll

private sealed interface HeaderStrategy {
    data object Dynamic : HeaderStrategy

    data class Subtype(
        val subtype: PnmSubtype,
    ) : HeaderStrategy

    data class Chosen(
        val header: PnmHeader,
    ) : HeaderStrategy
}

public sealed class FlatSamples {
    public data class U8(
        val samples: ByteArray,
    ) : FlatSamples() {
        override fun equals(other: Any?): Boolean =
            other is U8 && samples.contentEquals(other.samples)

        override fun hashCode(): Int = samples.contentHashCode()
    }

    public data class U16(
        val samples: ShortArray,
    ) : FlatSamples() {
        override fun equals(other: Any?): Boolean =
            other is U16 && samples.contentEquals(other.samples)

        override fun hashCode(): Int = samples.contentHashCode()
    }

    public fun size(): Int =
        when (this) {
            is U8 -> samples.size
            is U16 -> samples.size
        }

    public fun allSmaller(maxVal: UInt): Boolean =
        when (this) {
            is U8 -> samples.all { (it.toInt() and 0xFF).toUInt() <= maxVal }
            is U16 -> samples.all { (it.toInt() and 0xFFFF).toUInt() <= maxVal }
        }
}

/**
 * Encodes images to any of the PNM image formats (PBM, PGM, PPM, PAM).
 */
public class PnmEncoder private constructor(
    private val writer: IoWrite,
    private val headerStrategy: HeaderStrategy,
) : ImageEncoder {
    public constructor(writer: IoWrite) : this(writer, HeaderStrategy.Dynamic)

    public fun withSubtype(subtype: PnmSubtype): PnmEncoder =
        PnmEncoder(writer, HeaderStrategy.Subtype(subtype))

    public fun withHeader(header: PnmHeader): PnmEncoder =
        PnmEncoder(writer, HeaderStrategy.Chosen(header))

    public fun withDynamicHeader(): PnmEncoder =
        PnmEncoder(writer, HeaderStrategy.Dynamic)

    public fun encode(
        image: ByteArray,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        val flatSamples =
            when (color) {
                ExtendedColorType.L16,
                ExtendedColorType.La16,
                ExtendedColorType.Rgb16,
                ExtendedColorType.Rgba16,
                -> {
                    val shorts = ShortArray(image.size / 2)
                    for (i in shorts.indices) {
                        val lo = image[i * 2].toInt() and 0xFF
                        val hi = image[i * 2 + 1].toInt() and 0xFF
                        shorts[i] = ((hi shl 8) or lo).toShort()
                    }
                    FlatSamples.U16(shorts)
                }
                else -> FlatSamples.U8(image)
            }

        encodeImpl(flatSamples, width, height, color)
    }

    public fun encode(
        samples: ShortArray,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        encodeImpl(FlatSamples.U16(samples), width, height, color)
    }

    private fun encodeImpl(
        samples: FlatSamples,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        when (headerStrategy) {
            HeaderStrategy.Dynamic -> writeDynamicHeader(samples, width, height, color)
            is HeaderStrategy.Subtype -> writeSubtypedHeader(headerStrategy.subtype, samples, width, height, color)
            is HeaderStrategy.Chosen -> writeWithHeader(writer, headerStrategy.header, samples, width, height, color)
        }
    }

    private fun writeDynamicHeader(
        image: FlatSamples,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        val depth = color.channelCount().toUInt()
        val (maxval, tupltype) =
            when (color) {
                ExtendedColorType.L1 -> Pair(1u, ArbitraryTuplType.BlackAndWhite)
                ExtendedColorType.L8 -> Pair(0xFFu, ArbitraryTuplType.Grayscale)
                ExtendedColorType.L16 -> Pair(0xFFFFu, ArbitraryTuplType.Grayscale)
                ExtendedColorType.La1 -> Pair(1u, ArbitraryTuplType.BlackAndWhiteAlpha)
                ExtendedColorType.La8 -> Pair(0xFFu, ArbitraryTuplType.GrayscaleAlpha)
                ExtendedColorType.La16 -> Pair(0xFFFFu, ArbitraryTuplType.GrayscaleAlpha)
                ExtendedColorType.Rgb8 -> Pair(0xFFu, ArbitraryTuplType.RGB)
                ExtendedColorType.Rgb16 -> Pair(0xFFFFu, ArbitraryTuplType.RGB)
                ExtendedColorType.Rgba8 -> Pair(0xFFu, ArbitraryTuplType.RGBAlpha)
                ExtendedColorType.Rgba16 -> Pair(0xFFFFu, ArbitraryTuplType.RGBAlpha)
                else -> throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Pnm),
                        UnsupportedErrorKind.Color(color),
                    ),
                )
            }

        val header = PnmHeader(ArbitraryHeader(height, width, depth, maxval, tupltype))
        writeWithHeader(writer, header, image, width, height, color)
    }

    private fun writeSubtypedHeader(
        subtype: PnmSubtype,
        image: FlatSamples,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        val header =
            when (subtype) {
                PnmSubtype.ArbitraryMap -> {
                    writeDynamicHeader(image, width, height, color)
                    return
                }
                is PnmSubtype.Pixmap -> {
                    if (color == ExtendedColorType.Rgb8) {
                        PnmHeader(PixmapHeader(subtype.encoding, height, width, 255u))
                    } else {
                        throw ImageError.Unsupported(
                            UnsupportedError(
                                ImageFormatHint.Exact(ImageFormat.Pnm),
                                UnsupportedErrorKind.Color(color),
                            ),
                        )
                    }
                }
                is PnmSubtype.Graymap -> {
                    if (color == ExtendedColorType.L8) {
                        PnmHeader(GraymapHeader(subtype.encoding, height, width, 255u))
                    } else {
                        throw ImageError.Unsupported(
                            UnsupportedError(
                                ImageFormatHint.Exact(ImageFormat.Pnm),
                                UnsupportedErrorKind.Color(color),
                            ),
                        )
                    }
                }
                is PnmSubtype.Bitmap -> {
                    if (color == ExtendedColorType.L8 || color == ExtendedColorType.L1) {
                        PnmHeader(BitmapHeader(subtype.encoding, height, width))
                    } else {
                        throw ImageError.Unsupported(
                            UnsupportedError(
                                ImageFormatHint.Exact(ImageFormat.Pnm),
                                UnsupportedErrorKind.Color(color),
                            ),
                        )
                    }
                }
            }

        writeWithHeader(writer, header, image, width, height, color)
    }

    private fun writeWithHeader(
        writer: IoWrite,
        header: PnmHeader,
        image: FlatSamples,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        if (header.width() != width || header.height() != height) {
            throw ImageError.Parameter(ParameterError(ParameterErrorKind.DimensionMismatch))
        }

        val components = color.channelCount().toUInt()
        when (val rec = header.decoded) {
            is HeaderRecord.Bitmap -> {
                if (color != ExtendedColorType.L1 && color != ExtendedColorType.L8 && color != ExtendedColorType.L16) {
                    throw ImageError.Parameter(ParameterError(ParameterErrorKind.Generic("PBM format only supports luma color types")))
                }
            }
            is HeaderRecord.Graymap -> {
                if (color != ExtendedColorType.L1 && color != ExtendedColorType.L8 && color != ExtendedColorType.L16) {
                    throw ImageError.Parameter(ParameterError(ParameterErrorKind.Generic("PGM format only supports luma color types")))
                }
            }
            is HeaderRecord.Pixmap -> {
                if (color != ExtendedColorType.Rgb8) {
                    throw ImageError.Parameter(ParameterError(ParameterErrorKind.Generic("PPM format only supports ExtendedColorType.Rgb8")))
                }
            }
            is HeaderRecord.Arbitrary -> {
                val depth = rec.header.depth
                val tupltype = rec.header.tupltype
                when {
                    tupltype == ArbitraryTuplType.BlackAndWhite && color == ExtendedColorType.L1 -> Unit
                    tupltype == ArbitraryTuplType.BlackAndWhiteAlpha && color == ExtendedColorType.La8 -> Unit
                    tupltype == ArbitraryTuplType.Grayscale && (color == ExtendedColorType.L1 || color == ExtendedColorType.L8 || color == ExtendedColorType.L16) -> Unit
                    tupltype == ArbitraryTuplType.GrayscaleAlpha && color == ExtendedColorType.La8 -> Unit
                    tupltype == ArbitraryTuplType.RGB && (color == ExtendedColorType.Rgb8 || color == ExtendedColorType.Rgb16) -> Unit
                    tupltype == ArbitraryTuplType.RGBAlpha && (color == ExtendedColorType.Rgba8 || color == ExtendedColorType.Rgba16) -> Unit
                    tupltype == null && depth == components -> Unit
                    tupltype is ArbitraryTuplType.Custom && depth == components -> Unit
                    depth != components -> throw ImageError.Parameter(ParameterError(ParameterErrorKind.Generic("Depth mismatch: header $depth vs. color $components")))
                    else -> throw ImageError.Parameter(ParameterError(ParameterErrorKind.Generic("Invalid color type for selected PAM color type")))
                }
            }
        }

        val headerMaxval = header.maximalSample()
        val maxSample =
            when (color) {
                is ExtendedColorType.Unknown -> if (color.bpp <= 16u) ((1u shl color.bpp.toInt()) - 1u) else 0xFFFFu
                ExtendedColorType.L1 -> 1u
                ExtendedColorType.L8,
                ExtendedColorType.La8,
                ExtendedColorType.Rgb8,
                ExtendedColorType.Rgba8,
                ExtendedColorType.Bgr8,
                ExtendedColorType.Bgra8,
                -> 0xFFu
                ExtendedColorType.L16,
                ExtendedColorType.La16,
                ExtendedColorType.Rgb16,
                ExtendedColorType.Rgba16,
                -> 0xFFFFu
                else -> throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Pnm),
                        UnsupportedErrorKind.Color(color),
                    ),
                )
            }

        if (headerMaxval < maxSample && !image.allSmaller(headerMaxval)) {
            throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Pnm),
                    UnsupportedErrorKind.GenericFeature("Sample value greater than allowed for chosen header"),
                ),
            )
        }

        val expectedLen = (components.toLong() * width.toLong() * height.toLong()).toInt()
        if (image.size() != expectedLen) {
            throw ImageError.Parameter(ParameterError(ParameterErrorKind.DimensionMismatch))
        }

        header.write(writer)
        writeSampleData(writer, header.decoded, image, width)
    }

    private fun writeSampleData(
        writer: IoWrite,
        record: HeaderRecord,
        image: FlatSamples,
        width: UInt,
    ) {
        when (record) {
            is HeaderRecord.Bitmap -> {
                if (record.header.encoding == SampleEncoding.Binary) {
                    writePbmBits(writer, image, width)
                } else {
                    writeAscii(writer, image)
                }
            }
            is HeaderRecord.Graymap -> {
                if (record.header.encoding == SampleEncoding.Binary) {
                    writeBytes(writer, image)
                } else {
                    writeAscii(writer, image)
                }
            }
            is HeaderRecord.Pixmap -> {
                if (record.header.encoding == SampleEncoding.Binary) {
                    writeBytes(writer, image)
                } else {
                    writeAscii(writer, image)
                }
            }
            is HeaderRecord.Arbitrary -> {
                writeBytes(writer, image)
            }
        }
    }

    private fun writePbmBits(
        writer: IoWrite,
        image: FlatSamples,
        width: UInt,
    ) {
        val w = width.toInt()
        val lineWidth = (w - 1) / 8 + 1
        val lineBuf = ByteArray(lineWidth)

        when (image) {
            is FlatSamples.U8 -> {
                for (row in 0 until (image.samples.size / w)) {
                    val rowOffset = row * w
                    for (colByte in 0 until lineWidth) {
                        var b = 0
                        for (bit in 0 until 8) {
                            val pixelIdx = rowOffset + colByte * 8 + bit
                            if (pixelIdx < rowOffset + w) {
                                val pixelVal = image.samples[pixelIdx].toInt() and 0xFF
                                if (pixelVal == 0) {
                                    b = b or (1 shl (7 - bit))
                                }
                            }
                        }
                        lineBuf[colByte] = b.toByte()
                    }
                    writer.writeAll(lineBuf)
                }
            }
            is FlatSamples.U16 -> {
                for (row in 0 until (image.samples.size / w)) {
                    val rowOffset = row * w
                    for (colByte in 0 until lineWidth) {
                        var b = 0
                        for (bit in 0 until 8) {
                            val pixelIdx = rowOffset + colByte * 8 + bit
                            if (pixelIdx < rowOffset + w) {
                                val pixelVal = image.samples[pixelIdx].toInt() and 0xFFFF
                                if (pixelVal == 0) {
                                    b = b or (1 shl (7 - bit))
                                }
                            }
                        }
                        lineBuf[colByte] = b.toByte()
                    }
                    writer.writeAll(lineBuf)
                }
            }
        }
    }

    private fun writeBytes(
        writer: IoWrite,
        image: FlatSamples,
    ) {
        when (image) {
            is FlatSamples.U8 -> writer.writeAll(image.samples)
            is FlatSamples.U16 -> {
                val buf = ByteArray(image.samples.size * 2)
                for (i in image.samples.indices) {
                    val v = image.samples[i].toInt() and 0xFFFF
                    buf[i * 2] = ((v ushr 8) and 0xFF).toByte()
                    buf[i * 2 + 1] = (v and 0xFF).toByte()
                }
                writer.writeAll(buf)
            }
        }
    }

    private fun writeAscii(
        writer: IoWrite,
        image: FlatSamples,
    ) {
        val autoBreak = AutoBreak(writer, 70)
        autoBreak.use { ab ->
            when (image) {
                is FlatSamples.U8 -> {
                    for (sample in image.samples) {
                        val s = "${sample.toInt() and 0xFF} "
                        ab.writeAll(s.encodeToByteArray())
                    }
                }
                is FlatSamples.U16 -> {
                    for (sample in image.samples) {
                        val s = "${sample.toInt() and 0xFFFF} "
                        ab.writeAll(s.encodeToByteArray())
                    }
                }
            }
        }
    }

    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        val expectedBufferLen = colorType.bufferSize(width, height)
        require(expectedBufferLen == buf.size.toULong()) {
            "Invalid buffer length: expected $expectedBufferLen got ${buf.size} for ${width}x$height image"
        }
        encode(buf, width, height, colorType)
    }
}

internal class CheckedImageBuffer(
    val image: FlatSamples,
    val width: UInt,
    val height: UInt,
    val color: ExtendedColorType,
)

internal class UncheckedHeader(
    val header: PnmHeader,
)

internal class CheckedDimensions(
    val unchecked: UncheckedHeader,
    val width: UInt,
    val height: UInt,
)

internal class CheckedHeaderColor(
    val dimensions: CheckedDimensions,
    val color: ExtendedColorType,
)

internal class CheckedHeader(
    val color: CheckedHeaderColor,
    val image: CheckedImageBuffer,
)

internal class SampleWriter

