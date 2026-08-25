// port-lint: source codecs/hdr/decoder.rs
package io.github.kotlinmania.image.codecs.hdr

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.readExact
import io.github.kotlinmania.image.utils.checkDimensionOverflow
import kotlin.math.pow

/**
 * Errors that can occur during decoding and parsing of a HDR image.
 */
public sealed class DecoderError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** HDR's "#?RADIANCE" signature wrong or missing */
    public object RadianceHdrSignatureInvalid : DecoderError("Radiance HDR signature not found")

    /** EOF before end of header */
    public object TruncatedHeader : DecoderError("EOF in header")

    /** EOF instead of image dimensions */
    public object TruncatedDimensions : DecoderError("EOF in dimensions line")

    /** A value couldn't be parsed */
    public data class UnparsableF32(
        val line: LineType,
        val pe: Throwable,
    ) : DecoderError("Cannot parse $line value as f32: ${pe.message ?: pe.toString()}", pe)

    /** A value couldn't be parsed */
    public data class UnparsableU32(
        val line: LineType,
        val pe: Throwable,
    ) : DecoderError("Cannot parse $line value as u32: ${pe.message ?: pe.toString()}", pe)

    /** Not enough numbers in line */
    public data class LineTooShort(
        val line: LineType,
    ) : DecoderError("Not enough numbers in $line")

    /** COLORCORR contains too many numbers in strict mode */
    public object ExtraneousColorcorrNumbers : DecoderError("Extra numbers in COLORCORR")

    /** Dimensions line had too few elements */
    public data class DimensionsLineTooShort(
        val elements: Int,
        val expected: Int,
    ) : DecoderError("Dimensions line too short: have $elements elements, expected $expected")

    /** Dimensions line had too many elements */
    public data class DimensionsLineTooLong(
        val expected: Int,
    ) : DecoderError("Dimensions line too long, expected $expected elements")

    /** The length of a scanline wasn't a match for the specified length */
    public data class WrongScanlineLength(
        val len: Int,
        val expected: Int,
    ) : DecoderError("Wrong length of decoded scanline: got $len, expected $expected")

    /** First pixel of a scanline is a run length marker */
    public object FirstPixelRlMarker : DecoderError("First pixel of a scanline shouldn't be run length marker")
}

/**
 * Lines which contain parsable data that can fail.
 */
public enum class LineType {
    Exposure,
    Pixaspect,
    Colorcorr,
    DimensionsHeight,
    DimensionsWidth,
    ;

    override fun toString(): String =
        when (this) {
            Exposure -> "EXPOSURE"
            Pixaspect -> "PIXASPECT"
            Colorcorr -> "COLORCORR"
            DimensionsHeight -> "height dimension"
            DimensionsWidth -> "width dimension"
        }
}

/** Radiance HDR file signature */
public val SIGNATURE: ByteArray =
    byteArrayOf(
        '#'.code.toByte(),
        '?'.code.toByte(),
        'R'.code.toByte(),
        'A'.code.toByte(),
        'D'.code.toByte(),
        'I'.code.toByte(),
        'A'.code.toByte(),
        'N'.code.toByte(),
        'C'.code.toByte(),
        'E'.code.toByte(),
    )
private const val SIGNATURE_LENGTH: Int = 10

/**
 * Refer to Radiance HDR RGBE image format specification.
 */
public data class Rgbe8Pixel(
    public var r: UByte = 0u,
    public var g: UByte = 0u,
    public var b: UByte = 0u,
    public var e: UByte = 0u,
) {
    /**
     * Converts `Rgbe8Pixel` into `Rgb<Float>` linearly.
     */
    public fun toHdr(): Rgb<Float> =
        if (e == 0u.toUByte()) {
            Rgb(0.0f, 0.0f, 0.0f)
        } else {
            val exp = 2.0.pow(e.toDouble() - (128.0 + 8.0)).toFloat()
            Rgb(exp * r.toFloat(), exp * g.toFloat(), exp * b.toFloat())
        }
}

/**
 * Creates `Rgbe8Pixel` from components.
 */
public fun rgbe8(r: UByte, g: UByte, b: UByte, e: UByte): Rgbe8Pixel = Rgbe8Pixel(r, g, b, e)

/**
 * Creates `Rgbe8Pixel` from Int components.
 */
public fun rgbe8(r: Int, g: Int, b: Int, e: Int): Rgbe8Pixel =
    Rgbe8Pixel(r.toUByte(), g.toUByte(), b.toUByte(), e.toUByte())

/**
 * Orientation matrix for Radiance HDR metadata.
 */
public data class HdrOrientation(
    public val c1x: Byte = 1,
    public val c1y: Byte = 0,
    public val c2x: Byte = 0,
    public val c2y: Byte = 1,
)

/**
 * Color correction factors for Radiance HDR metadata.
 */
public data class HdrColorCorrection(
    public val r: Float = 1.0f,
    public val g: Float = 1.0f,
    public val b: Float = 1.0f,
)

/**
 * Custom header attribute key-value pair for Radiance HDR metadata.
 */
public data class HdrCustomAttribute(
    public val key: String,
    public val value: String,
)

/**
 * Metadata for Radiance HDR image.
 */
public class HdrMetadata(
    public var width: UInt = 0u,
    public var height: UInt = 0u,
    public var orientation: HdrOrientation = HdrOrientation(),
    public var exposure: Float? = null,
    public var colorCorrection: HdrColorCorrection? = null,
    public var pixelAspectRatio: Float? = null,
    customAttributes: List<HdrCustomAttribute> = emptyList(),
) {
    private val _customAttributes: MutableList<HdrCustomAttribute> = customAttributes.toMutableList()
    public val customAttributes: List<HdrCustomAttribute> get() = _customAttributes

    public fun copy(
        width: UInt = this.width,
        height: UInt = this.height,
        orientation: HdrOrientation = this.orientation,
        exposure: Float? = this.exposure,
        colorCorrection: HdrColorCorrection? = this.colorCorrection,
        pixelAspectRatio: Float? = this.pixelAspectRatio,
        customAttributes: List<HdrCustomAttribute> = this._customAttributes,
    ): HdrMetadata =
        HdrMetadata(
            width = width,
            height = height,
            orientation = orientation,
            exposure = exposure,
            colorCorrection = colorCorrection,
            pixelAspectRatio = pixelAspectRatio,
            customAttributes = customAttributes,
        )

    internal fun updateHeaderInfo(line: String, strict: Boolean) {
        val maybeKeyValue = splitAtFirst(line, "=")?.let { (key, value) -> Pair(key.trim(), value) }
        if (maybeKeyValue != null) {
            _customAttributes.add(HdrCustomAttribute(maybeKeyValue.first, maybeKeyValue.second))
        } else {
            _customAttributes.add(HdrCustomAttribute("", line))
        }

        if (maybeKeyValue != null) {
            val (key, valStr) = maybeKeyValue
            when (key) {
                "FORMAT" -> {
                    if (valStr.trim() != "32-bit_rle_rgbe") {
                        throw ImageError.Unsupported(
                            UnsupportedError(
                                ImageFormatHint.Exact(ImageFormat.Hdr),
                                UnsupportedErrorKind.Format(
                                    ImageFormatHint.Name(limitStringLen(valStr, 20)),
                                ),
                            ),
                        )
                    }
                }
                "EXPOSURE" -> {
                    try {
                        val v = valStr.trim().toFloat()
                        exposure = (exposure ?: 1.0f) * v
                    } catch (pe: Exception) {
                        if (strict) {
                            throw ImageError.Decoding(
                                DecodingError(
                                    ImageFormatHint.Exact(ImageFormat.Hdr),
                                    DecoderError.UnparsableF32(LineType.Exposure, pe),
                                ),
                            )
                        }
                    }
                }
                "PIXASPECT" -> {
                    try {
                        val v = valStr.trim().toFloat()
                        pixelAspectRatio = (pixelAspectRatio ?: 1.0f) * v
                    } catch (pe: Exception) {
                        if (strict) {
                            throw ImageError.Decoding(
                                DecodingError(
                                    ImageFormatHint.Exact(ImageFormat.Hdr),
                                    DecoderError.UnparsableF32(LineType.Pixaspect, pe),
                                ),
                            )
                        }
                    }
                }
                "COLORCORR" -> {
                    val rgbcorr = FloatArray(3) { 1.0f }
                    try {
                        val extraNumbers = parseSpaceSeparatedF32(valStr, rgbcorr, LineType.Colorcorr)
                        if (strict && extraNumbers) {
                            throw ImageError.Decoding(
                                DecodingError(
                                    ImageFormatHint.Exact(ImageFormat.Hdr),
                                    DecoderError.ExtraneousColorcorrNumbers,
                                ),
                            )
                        }
                        val curr = colorCorrection ?: HdrColorCorrection(1.0f, 1.0f, 1.0f)
                        colorCorrection = HdrColorCorrection(curr.r * rgbcorr[0], curr.g * rgbcorr[1], curr.b * rgbcorr[2])
                    } catch (err: Exception) {
                        if (strict) {
                            if (err is ImageError) throw err
                            throw ImageError.Decoding(
                                DecodingError(
                                    ImageFormatHint.Exact(ImageFormat.Hdr),
                                    err,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    public companion object {
        public fun new(): HdrMetadata = HdrMetadata()
    }
}

/**
 * A Radiance HDR decoder.
 */
public class HdrDecoder internal constructor(
    private val reader: IoRead,
    private val width: UInt,
    private val height: UInt,
    private val meta: HdrMetadata,
) : ImageDecoder {
    internal constructor(bufferIoRead: BufferIoRead) : this(
        reader = bufferIoRead,
        width = 0u,
        height = 0u,
        meta = HdrMetadata.new(),
    )

    public fun metadata(): HdrMetadata = meta.copy(customAttributes = meta.customAttributes.toList())

    internal fun readImageTransform(
        f: (Rgbe8Pixel) -> Rgb<Float>,
        outputSlice: Array<Rgb<Float>>,
    ) {
        require(outputSlice.size == width.toInt() * height.toInt()) {
            "outputSlice length ${outputSlice.size} does not match dimensions ${width}x$height"
        }

        if (width == 0u || height == 0u) {
            return
        }

        val w = width.toInt()
        val buf = Array(w) { Rgbe8Pixel() }
        for (y in 0 until height.toInt()) {
            readScanline(reader, buf)
            val rowOffset = y * w
            for (x in 0 until w) {
                outputSlice[rowOffset + x] = f(buf[x])
            }
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(meta.width, meta.height)

    override fun colorType(): ColorType = ColorType.Rgb32F

    override fun readImage(buf: ByteArray) {
        val total = totalBytes().toLong()
        require(buf.size.toLong() == total) {
            "buf size ${buf.size} does not match total bytes $total"
        }

        val img = Array(width.toInt() * height.toInt()) { Rgb(0.0f, 0.0f, 0.0f) }
        readImageTransform({ it.toHdr() }, img)

        for (i in img.indices) {
            val rgb = img[i]
            val base = i * 12

            val rBits = rgb.r.toBits()
            buf[base] = (rBits and 0xFF).toByte()
            buf[base + 1] = ((rBits ushr 8) and 0xFF).toByte()
            buf[base + 2] = ((rBits ushr 16) and 0xFF).toByte()
            buf[base + 3] = ((rBits ushr 24) and 0xFF).toByte()

            val gBits = rgb.g.toBits()
            buf[base + 4] = (gBits and 0xFF).toByte()
            buf[base + 5] = ((gBits ushr 8) and 0xFF).toByte()
            buf[base + 6] = ((gBits ushr 16) and 0xFF).toByte()
            buf[base + 7] = ((gBits ushr 24) and 0xFF).toByte()

            val bBits = rgb.b.toBits()
            buf[base + 8] = (bBits and 0xFF).toByte()
            buf[base + 9] = ((bBits ushr 8) and 0xFF).toByte()
            buf[base + 10] = ((bBits ushr 16) and 0xFF).toByte()
            buf[base + 11] = ((bBits ushr 24) and 0xFF).toByte()
        }
    }

    public companion object {
        /**
         * Reads Radiance HDR image header from stream [reader].
         * If the header is valid, creates `HdrDecoder` with strict mode enabled.
         */
        public fun new(reader: IoRead): HdrDecoder = withStrictness(reader, true)

        /**
         * Allows reading old Radiance HDR images.
         */
        public fun newNonstrict(reader: IoRead): HdrDecoder = withStrictness(reader, false)

        /**
         * Reads Radiance HDR image header from stream [reader],
         * if the header is valid, creates `HdrDecoder`.
         */
        public fun withStrictness(reader: IoRead, strict: Boolean): HdrDecoder {
            val attributes = HdrMetadata.new()

            if (strict) {
                val signature = ByteArray(SIGNATURE_LENGTH)
                try {
                    reader.readExact(signature)
                } catch (e: Exception) {
                    throw ImageError.Decoding(
                        DecodingError(
                            ImageFormatHint.Exact(ImageFormat.Hdr),
                            DecoderError.RadianceHdrSignatureInvalid,
                        ),
                    )
                }
                if (!signature.contentEquals(SIGNATURE)) {
                    throw ImageError.Decoding(
                        DecodingError(
                            ImageFormatHint.Exact(ImageFormat.Hdr),
                            DecoderError.RadianceHdrSignatureInvalid,
                        ),
                    )
                }
                readLineU8(reader)
            }

            while (true) {
                val line =
                    readLineU8(reader) ?: throw ImageError.Decoding(
                        DecodingError(
                            ImageFormatHint.Exact(ImageFormat.Hdr),
                            DecoderError.TruncatedHeader,
                        ),
                    )
                if (line.isEmpty()) {
                    break
                } else if (line[0] == '#'.code.toByte()) {
                    continue
                }
                val lineStr = line.decodeToString()
                attributes.updateHeaderInfo(lineStr, strict)
            }

            val dimensionsBytes =
                readLineU8(reader) ?: throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Hdr),
                        DecoderError.TruncatedDimensions,
                    ),
                )
            val dimensionsStr = dimensionsBytes.decodeToString()
            val (width, height) = parseDimensionsLine(dimensionsStr, strict)

            if (checkDimensionOverflow(width, height, ColorType.Rgb8.bytesPerPixel())) {
                throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Hdr),
                        UnsupportedErrorKind.GenericFeature("Image dimensions (${width}x$height) are too large"),
                    ),
                )
            }

            attributes.width = width
            attributes.height = height

            return HdrDecoder(
                reader = reader,
                width = width,
                height = height,
                meta = attributes,
            )
        }
    }
}

private fun readScanline(r: IoRead, buf: Array<Rgbe8Pixel>) {
    require(buf.isNotEmpty())
    val width = buf.size
    val fb = readRgbe(r)
    if (fb.r == 2u.toUByte() && fb.g == 2u.toUByte() && fb.b < 128u.toUByte()) {
        decodeComponent(r, width) { offset, value -> buf[offset].r = value }
        decodeComponent(r, width) { offset, value -> buf[offset].g = value }
        decodeComponent(r, width) { offset, value -> buf[offset].b = value }
        decodeComponent(r, width) { offset, value -> buf[offset].e = value }
    } else {
        decodeOldRle(r, fb, buf)
    }
}

private fun readByte(r: IoRead): UByte {
    val buf = ByteArray(1)
    r.readExact(buf)
    return buf[0].toUByte()
}

private fun decodeComponent(r: IoRead, width: Int, setComponent: (Int, UByte) -> Unit) {
    val buf = ByteArray(128)
    var pos = 0
    while (pos < width) {
        val rl = readByte(r).toInt()
        if (rl <= 128) {
            if (pos + rl > width) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Hdr),
                        DecoderError.WrongScanlineLength(pos + rl, width),
                    ),
                )
            }
            r.readExact(buf, 0, rl)
            for (offset in 0 until rl) {
                setComponent(pos + offset, buf[offset].toUByte())
            }
            pos += rl
        } else {
            val runLen = rl - 128
            if (pos + runLen > width) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Hdr),
                        DecoderError.WrongScanlineLength(pos + runLen, width),
                    ),
                )
            }
            val value = readByte(r)
            for (offset in 0 until runLen) {
                setComponent(pos + offset, value)
            }
            pos += runLen
        }
    }
    if (pos != width) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.WrongScanlineLength(pos, width),
            ),
        )
    }
}

private fun decodeOldRle(r: IoRead, fb: Rgbe8Pixel, buf: Array<Rgbe8Pixel>) {
    require(buf.isNotEmpty())
    val width = buf.size

    fun rlMarker(pix: Rgbe8Pixel): Int? =
        if (pix.r == 1u.toUByte() && pix.g == 1u.toUByte() && pix.b == 1u.toUByte()) {
            pix.e.toInt()
        } else {
            null
        }

    if (rlMarker(fb) != null) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.FirstPixelRlMarker,
            ),
        )
    }

    buf[0] = fb
    var xOff = 1
    var rlMult = 1
    var prevPixel = fb

    while (xOff < width) {
        val pix = readRgbe(r)
        val marker = rlMarker(pix)
        if (marker != null) {
            val rl = marker * rlMult
            rlMult *= 256
            if (xOff + rl <= width) {
                for (b in xOff until xOff + rl) {
                    buf[b] = prevPixel.copy()
                }
            } else {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Hdr),
                        DecoderError.WrongScanlineLength(xOff + rl, width),
                    ),
                )
            }
            xOff += rl
        } else {
            rlMult = 1
            prevPixel = pix
            buf[xOff] = pix
            xOff += 1
        }
    }
    if (xOff != width) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.WrongScanlineLength(xOff, width),
            ),
        )
    }
}

internal fun readRgbe(r: IoRead): Rgbe8Pixel {
    val buf = ByteArray(4)
    r.readExact(buf)
    return Rgbe8Pixel(
        r = buf[0].toUByte(),
        g = buf[1].toUByte(),
        b = buf[2].toUByte(),
        e = buf[3].toUByte(),
    )
}

internal fun parseSpaceSeparatedF32(line: String, vals: FloatArray, lineTp: LineType): Boolean {
    val nums = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (nums.size < vals.size) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.LineTooShort(lineTp),
            ),
        )
    }
    for (i in vals.indices) {
        try {
            vals[i] = nums[i].toFloat()
        } catch (pe: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Hdr),
                    DecoderError.UnparsableF32(lineTp, pe),
                ),
            )
        }
    }
    return nums.size > vals.size
}

internal fun parseDimensionsLine(line: String, strict: Boolean): Pair<UInt, UInt> {
    val dimensionsCount = 4
    val dimParts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (dimParts.size < 1) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.DimensionsLineTooShort(0, dimensionsCount),
            ),
        )
    }
    if (dimParts.size < 2) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.DimensionsLineTooShort(1, dimensionsCount),
            ),
        )
    }
    if (dimParts.size < 3) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.DimensionsLineTooShort(2, dimensionsCount),
            ),
        )
    }
    if (dimParts.size < 4) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.DimensionsLineTooShort(3, dimensionsCount),
            ),
        )
    }
    if (strict && dimParts.size > 4) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                DecoderError.DimensionsLineTooLong(dimensionsCount),
            ),
        )
    }

    val c1Tag = dimParts[0]
    val c1Str = dimParts[1]
    val c2Tag = dimParts[2]
    val c2Str = dimParts[3]

    if (c1Tag == "-Y" && c2Tag == "+X") {
        val height: UInt
        try {
            height = c1Str.toUInt()
        } catch (pe: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Hdr),
                    DecoderError.UnparsableU32(LineType.DimensionsHeight, pe),
                ),
            )
        }
        val width: UInt
        try {
            width = c2Str.toUInt()
        } catch (pe: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Hdr),
                    DecoderError.UnparsableU32(LineType.DimensionsWidth, pe),
                ),
            )
        }
        return Pair(width, height)
    } else {
        throw ImageError.Unsupported(
            UnsupportedError(
                ImageFormatHint.Exact(ImageFormat.Hdr),
                UnsupportedErrorKind.GenericFeature(
                    "Orientation ${limitStringLen(c1Tag, 4)} ${limitStringLen(c2Tag, 4)}",
                ),
            ),
        )
    }
}

internal fun limitStringLen(s: String, len: Int): String {
    val charCount = s.length
    return if (charCount > len) {
        s.take(len) + "..."
    } else {
        s
    }
}

internal fun splitAtFirst(s: String, separator: String): Pair<String, String>? {
    val p = s.indexOf(separator)
    return if (p <= 0 || p >= s.length - separator.length) {
        null
    } else {
        Pair(s.substring(0, p), s.substring(p + separator.length))
    }
}

internal fun readLineU8(r: IoRead): ByteArray? {
    val ret = ArrayList<Byte>(16)
    val byte = ByteArray(1)
    while (true) {
        val n = r.read(byte, 0, 1)
        if (n == 0 || byte[0] == '\n'.code.toByte()) {
            if (ret.isEmpty() && (n == 0 || byte[0] != '\n'.code.toByte())) {
                return null
            }
            val result = ByteArray(ret.size)
            for (i in ret.indices) {
                result[i] = ret[i]
            }
            return result
        }
        ret.add(byte[0])
    }
}
