// port-lint: source image/src/codecs/jpeg/decoder.rs
package io.github.kotlinmania.image.codecs.jpeg

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.LimitError
import io.github.kotlinmania.image.LimitErrorKind
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.Limits
import io.github.kotlinmania.image.metadata.Orientation
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Zune color space representation for JPEG decoding parity.
 */
public enum class ZuneColorSpace {
    Rgb,
    Rgba,
    Luma,
    LumaA,
    Ycbcr,
    Unknown,
}

/**
 * Maps a Zune color space to a supported decoding color space.
 */
public fun toSupportedColorSpace(orig: ZuneColorSpace): ZuneColorSpace =
    when (orig) {
        ZuneColorSpace.Rgb, ZuneColorSpace.Rgba, ZuneColorSpace.Luma, ZuneColorSpace.LumaA -> orig
        else -> ZuneColorSpace.Rgb
    }

/**
 * Maps a Zune color space to an Image ColorType.
 */
public fun fromJpeg(colorspace: ZuneColorSpace): ColorType =
    when (toSupportedColorSpace(colorspace)) {
        ZuneColorSpace.Rgb -> ColorType.Rgb8
        ZuneColorSpace.Rgba -> ColorType.Rgba8
        ZuneColorSpace.Luma -> ColorType.L8
        ZuneColorSpace.LumaA -> ColorType.La8
        else -> ColorType.Rgb8
    }

/**
 * Constructs a new JPEG decoder configured with the specified limits.
 */
public fun newZuneDecoder(
    input: ByteArray,
    origColorSpace: ZuneColorSpace = ZuneColorSpace.Rgb,
    limits: Limits = Limits.noLimits(),
): JpegDecoder {
    val decoder = JpegDecoder(input)
    decoder.setLimits(limits)
    return decoder
}

/**
 * JPEG decoder supporting baseline JPEG decoding and metadata extraction.
 */
public class JpegDecoder(
    private val input: ByteArray,
) : ImageDecoder {
    private var width: UInt = 0u
    private var height: UInt = 0u
    private var origColorType: ColorType = ColorType.Rgb8
    private var limits: Limits = Limits.noLimits()
    private var cachedExif: ByteArray? = null
    private var cachedIcc: ByteArray? = null
    private var cachedXmp: ByteArray? = null
    private var cachedIptc: ByteArray? = null
    private var cachedOrientation: Orientation? = null

    // Quantization tables [0..3][64]
    private val qTables = Array(4) { IntArray(64) }
    private val hasQTable = BooleanArray(4)

    // Huffman tables: DC (0..3) and AC (0..3)
    private val dcHuffman = arrayOfNulls<HuffmanTable>(4)
    private val acHuffman = arrayOfNulls<HuffmanTable>(4)

    // Component specifications
    private class JpegComponent(
        val id: Int,
        val hSample: Int,
        val vSample: Int,
        val qTableId: Int,
        var dcTableId: Int = 0,
        var acTableId: Int = 0,
    )

    private val components = mutableListOf<JpegComponent>()
    private var scanDataStart: Int = -1

    init {
        parseHeaders()
    }

    public constructor(r: IoRead) : this(readAllBytes(r))

    private fun parseHeaders() {
        if (input.size < 4) {
            throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Jpeg), "Image data too short"))
        }
        if ((input[0].toInt() and 0xFF) != 0xFF || (input[1].toInt() and 0xFF) != 0xD8) {
            throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Jpeg), "Invalid JPEG SOI marker"))
        }

        var offset = 2
        var sofFound = false

        while (offset < input.size - 1) {
            if ((input[offset].toInt() and 0xFF) != 0xFF) {
                offset++
                continue
            }
            while (offset < input.size && (input[offset].toInt() and 0xFF) == 0xFF) {
                offset++
            }
            if (offset >= input.size) break
            val marker = input[offset].toInt() and 0xFF
            offset++

            // Standalone markers
            if (marker == 0xD8 || marker == 0xD9 || (marker in 0xD0..0xD7) || marker == 0x01) {
                continue
            }

            if (offset + 2 > input.size) break
            val length = ((input[offset].toInt() and 0xFF) shl 8) or (input[offset + 1].toInt() and 0xFF)
            if (length < 2 || offset + length > input.size) {
                throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Jpeg), "Invalid segment length"))
            }

            val payloadStart = offset + 2
            val payloadLen = length - 2

            when (marker) {
                // SOF0 (Baseline DCT), SOF1 (Extended Sequential), SOF2 (Progressive)
                0xC0, 0xC1, 0xC2 -> {
                    if (payloadLen >= 6) {
                        val precision = input[payloadStart].toInt() and 0xFF
                        if (precision != 8) {
                            throw ImageError.Unsupported(
                                UnsupportedError.fromFormatAndKind(
                                    ImageFormatHint.Exact(ImageFormat.Jpeg),
                                    UnsupportedErrorKind.GenericFeature("JPEG precision $precision"),
                                ),
                            )
                        }
                        val h = ((input[payloadStart + 1].toInt() and 0xFF) shl 8) or (input[payloadStart + 2].toInt() and 0xFF)
                        val w = ((input[payloadStart + 3].toInt() and 0xFF) shl 8) or (input[payloadStart + 4].toInt() and 0xFF)
                        val numComp = input[payloadStart + 5].toInt() and 0xFF

                        height = h.toUInt()
                        width = w.toUInt()
                        origColorType =
                            when (numComp) {
                                1 -> ColorType.L8
                                3 -> ColorType.Rgb8
                                4 -> ColorType.Rgba8
                                else -> ColorType.Rgb8
                            }

                        components.clear()
                        var compOffset = payloadStart + 6
                        for (i in 0 until numComp) {
                            if (compOffset + 3 <= payloadStart + payloadLen) {
                                val cId = input[compOffset].toInt() and 0xFF
                                val sampling = input[compOffset + 1].toInt() and 0xFF
                                val hSample = (sampling shr 4) and 0x0F
                                val vSample = sampling and 0x0F
                                val qTable = input[compOffset + 2].toInt() and 0xFF
                                components.add(JpegComponent(cId, hSample, vSample, qTable))
                                compOffset += 3
                            }
                        }
                        sofFound = true
                    }
                }
                // DQT
                0xDB -> {
                    var dqtOffset = payloadStart
                    while (dqtOffset < payloadStart + payloadLen) {
                        val info = input[dqtOffset].toInt() and 0xFF
                        val qTableId = info and 0x0F
                        val qPrecision = (info shr 4) and 0x0F
                        dqtOffset++
                        if (qTableId < 4) {
                            if (qPrecision == 0) {
                                if (dqtOffset + 64 <= payloadStart + payloadLen) {
                                    for (k in 0 until 64) {
                                        qTables[qTableId][ZIGZAG[k]] = input[dqtOffset + k].toInt() and 0xFF
                                    }
                                    hasQTable[qTableId] = true
                                    dqtOffset += 64
                                } else {
                                    break
                                }
                            } else {
                                if (dqtOffset + 128 <= payloadStart + payloadLen) {
                                    for (k in 0 until 64) {
                                        val v = ((input[dqtOffset + 2 * k].toInt() and 0xFF) shl 8) or (input[dqtOffset + 2 * k + 1].toInt() and 0xFF)
                                        qTables[qTableId][ZIGZAG[k]] = v
                                    }
                                    hasQTable[qTableId] = true
                                    dqtOffset += 128
                                } else {
                                    break
                                }
                            }
                        } else {
                            break
                        }
                    }
                }
                // DHT
                0xC4 -> {
                    var dhtOffset = payloadStart
                    while (dhtOffset < payloadStart + payloadLen) {
                        val info = input[dhtOffset].toInt() and 0xFF
                        val tableClass = (info shr 4) and 0x0F // 0 = DC, 1 = AC
                        val tableId = info and 0x0F
                        dhtOffset++
                        if (dhtOffset + 16 <= payloadStart + payloadLen && tableId < 4) {
                            val counts = IntArray(16)
                            var totalSymbols = 0
                            for (k in 0 until 16) {
                                counts[k] = input[dhtOffset + k].toInt() and 0xFF
                                totalSymbols += counts[k]
                            }
                            dhtOffset += 16
                            if (dhtOffset + totalSymbols <= payloadStart + payloadLen) {
                                val symbols = IntArray(totalSymbols)
                                for (k in 0 until totalSymbols) {
                                    symbols[k] = input[dhtOffset + k].toInt() and 0xFF
                                }
                                dhtOffset += totalSymbols
                                val table = HuffmanTable(counts, symbols)
                                if (tableClass == 0) {
                                    dcHuffman[tableId] = table
                                } else {
                                    acHuffman[tableId] = table
                                }
                            } else {
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
                // SOS (Start of Scan)
                0xDA -> {
                    if (payloadLen >= 2) {
                        val numScanComp = input[payloadStart].toInt() and 0xFF
                        var scanCompOffset = payloadStart + 1
                        for (i in 0 until numScanComp) {
                            if (scanCompOffset + 2 <= payloadStart + payloadLen) {
                                val cId = input[scanCompOffset].toInt() and 0xFF
                                val tableMapping = input[scanCompOffset + 1].toInt() and 0xFF
                                val dcTable = (tableMapping shr 4) and 0x0F
                                val acTable = tableMapping and 0x0F
                                components.find { it.id == cId }?.let {
                                    it.dcTableId = dcTable
                                    it.acTableId = acTable
                                }
                                scanCompOffset += 2
                            }
                        }
                    }
                    scanDataStart = offset + length
                    break
                }
                // APP1 (Exif or XMP)
                0xE1 -> {
                    if (payloadLen >= 6 &&
                        input[payloadStart] == 'E'.code.toByte() &&
                        input[payloadStart + 1] == 'x'.code.toByte() &&
                        input[payloadStart + 2] == 'i'.code.toByte() &&
                        input[payloadStart + 3] == 'f'.code.toByte() &&
                        input[payloadStart + 4] == 0.toByte() &&
                        input[payloadStart + 5] == 0.toByte()
                    ) {
                        val exifLen = payloadLen - 6
                        if (exifLen > 0) {
                            cachedExif = input.copyOfRange(payloadStart + 6, payloadStart + payloadLen)
                        }
                    } else if (payloadLen >= 29 && isXmpPrefix(input, payloadStart)) {
                        val xmpLen = payloadLen - 29
                        if (xmpLen > 0) {
                            cachedXmp = input.copyOfRange(payloadStart + 29, payloadStart + payloadLen)
                        }
                    }
                }
                // APP2 (ICC Profile)
                0xE2 -> {
                    if (payloadLen >= 14 && isIccPrefix(input, payloadStart)) {
                        val iccLen = payloadLen - 14
                        if (iccLen > 0) {
                            cachedIcc = input.copyOfRange(payloadStart + 14, payloadStart + payloadLen)
                        }
                    }
                }
                // APP13 (Photoshop / IPTC)
                0xED -> {
                    if (payloadLen >= 14 && isPhotoshopPrefix(input, payloadStart)) {
                        cachedIptc = input.copyOfRange(payloadStart, payloadStart + payloadLen)
                    }
                }
            }
            offset += length
        }

        if (!sofFound) {
            throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Jpeg), "No SOF marker found in JPEG stream"))
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType = origColorType

    override fun iccProfile(): ByteArray? = cachedIcc

    override fun exifMetadata(): ByteArray? = cachedExif

    override fun xmpMetadata(): ByteArray? = cachedXmp

    override fun iptcMetadata(): ByteArray? = cachedIptc

    override fun orientation(): Orientation {
        if (cachedOrientation == null) {
            cachedOrientation = cachedExif?.let { Orientation.fromExifChunk(it) } ?: Orientation.NoTransforms
        }
        return cachedOrientation ?: Orientation.NoTransforms
    }

    override fun setLimits(limits: Limits) {
        limits
            .checkSupport(
                io.github.kotlinmania.image.io
                    .LimitSupport(),
            ).getOrThrow()
        val (w, h) = dimensions()
        limits.checkDimensions(w, h).getOrThrow()
        this.limits = limits
    }

    override fun readImage(buf: ByteArray) {
        val expectedLen = totalBytes().toLong()
        if (buf.size.toLong() != expectedLen) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Jpeg),
                    "Length of the decoded data ${buf.size} doesn't match the advertised dimensions of the image that imply length $expectedLen",
                ),
            )
        }

        if (width == 0u || height == 0u) return

        limits.maxImageWidth?.let {
            if (width > it) {
                throw ImageError.Limits(LimitError.fromKind(LimitErrorKind.DimensionError))
            }
        }
        limits.maxImageHeight?.let {
            if (height > it) {
                throw ImageError.Limits(LimitError.fromKind(LimitErrorKind.DimensionError))
            }
        }

        decodeScanData(buf)
    }

    override fun readImageBoxed(buf: ByteArray) {
        readImage(buf)
    }

    private fun decodeScanData(outBuf: ByteArray) {
        if (scanDataStart < 0 || scanDataStart >= input.size) {
            return
        }

        val reader = BitReader(input, scanDataStart)
        val w = width.toInt()
        val h = height.toInt()

        var maxH = 1
        var maxV = 1
        for (c in components) {
            if (c.hSample > maxH) maxH = c.hSample
            if (c.vSample > maxV) maxV = c.vSample
        }

        val mcuWidth = maxH * 8
        val mcuHeight = maxV * 8
        val mcusX = (w + mcuWidth - 1) / mcuWidth
        val mcusY = (h + mcuHeight - 1) / mcuHeight

        val prevDC = IntArray(components.size)
        val block = IntArray(64)
        val idctBlock = IntArray(64)

        val mcuBuffers =
            Array(components.size) { cIdx ->
                val c = components[cIdx]
                Array(c.hSample * c.vSample) { IntArray(64) }
            }

        for (mcuY in 0 until mcusY) {
            for (mcuX in 0 until mcusX) {
                for (cIdx in 0 until components.size) {
                    val comp = components[cIdx]
                    val dcTable = dcHuffman[comp.dcTableId]
                    val acTable = acHuffman[comp.acTableId]
                    val qTable = qTables[comp.qTableId]

                    for (bIdx in 0 until comp.hSample * comp.vSample) {
                        block.fill(0)

                        val dcCode = dcTable?.decodeSymbol(reader) ?: 0
                        val dcDiff = if (dcCode > 0) reader.readSignedBits(dcCode) else 0
                        prevDC[cIdx] += dcDiff
                        block[0] = prevDC[cIdx] * qTable[0]

                        var k = 1
                        while (k < 64) {
                            val acCode = acTable?.decodeSymbol(reader) ?: 0
                            if (acCode == 0) {
                                break
                            }
                            val run = (acCode shr 4) and 0x0F
                            val size = acCode and 0x0F
                            k += run
                            if (k < 64) {
                                val acVal = if (size > 0) reader.readSignedBits(size) else 0
                                block[ZIGZAG[k]] = acVal * qTable[ZIGZAG[k]]
                                k++
                            }
                        }

                        fastIdct(block, idctBlock)
                        idctBlock.copyInto(mcuBuffers[cIdx][bIdx])
                    }
                }

                renderMcuToOutput(mcuX, mcuY, maxH, maxV, mcuBuffers, outBuf, w, h)
            }
        }
    }

    private fun renderMcuToOutput(
        mcuX: Int,
        mcuY: Int,
        maxH: Int,
        maxV: Int,
        mcuBuffers: Array<Array<IntArray>>,
        outBuf: ByteArray,
        width: Int,
        height: Int,
    ) {
        val basePixelX = mcuX * maxH * 8
        val basePixelY = mcuY * maxV * 8

        if (components.size == 1) {
            val lumaBlock = mcuBuffers[0][0]
            for (by in 0 until 8) {
                val py = basePixelY + by
                if (py >= height) continue
                for (bx in 0 until 8) {
                    val px = basePixelX + bx
                    if (px >= width) continue
                    val idx = py * width + px
                    val luma = (lumaBlock[by * 8 + bx] + 128).coerceIn(0, 255)
                    outBuf[idx] = luma.toByte()
                }
            }
        } else if (components.size >= 3) {
            val cbComp = components[1]
            val crComp = components[2]

            for (vy in 0 until maxV) {
                for (hx in 0 until maxH) {
                    val yBlockIdx = vy * maxH + hx
                    val yBlock = mcuBuffers[0][yBlockIdx]

                    for (by in 0 until 8) {
                        val py = basePixelY + vy * 8 + by
                        if (py >= height) continue

                        for (bx in 0 until 8) {
                            val px = basePixelX + hx * 8 + bx
                            if (px >= width) continue

                            val yVal = yBlock[by * 8 + bx].toDouble()
                            val cbVal = sampleSubsampled(mcuBuffers[1], cbComp, hx * 8 + bx, vy * 8 + by, maxH * 8, maxV * 8).toDouble()
                            val crVal = sampleSubsampled(mcuBuffers[2], crComp, hx * 8 + bx, vy * 8 + by, maxH * 8, maxV * 8).toDouble()

                            val r = (yVal + 1.402 * crVal + 128.0).toInt().coerceIn(0, 255)
                            val g = (yVal - 0.344136 * cbVal - 0.714136 * crVal + 128.0).toInt().coerceIn(0, 255)
                            val b = (yVal + 1.772 * cbVal + 128.0).toInt().coerceIn(0, 255)

                            val outIdx = (py * width + px) * 3
                            outBuf[outIdx] = r.toByte()
                            outBuf[outIdx + 1] = g.toByte()
                            outBuf[outIdx + 2] = b.toByte()
                        }
                    }
                }
            }
        }
    }

    private fun sampleSubsampled(
        blocks: Array<IntArray>,
        comp: JpegComponent,
        mcuPixelX: Int,
        mcuPixelY: Int,
        mcuPixelWidth: Int,
        mcuPixelHeight: Int,
    ): Int {
        val compPixelX = (mcuPixelX * (comp.hSample * 8)) / mcuPixelWidth
        val compPixelY = (mcuPixelY * (comp.vSample * 8)) / mcuPixelHeight

        val blockX = (compPixelX / 8).coerceIn(0, comp.hSample - 1)
        val blockY = (compPixelY / 8).coerceIn(0, comp.vSample - 1)
        val blockIdx = blockY * comp.hSample + blockX

        val inBlockX = compPixelX % 8
        val inBlockY = compPixelY % 8

        return blocks[blockIdx][inBlockY * 8 + inBlockX]
    }

    private class HuffmanTable(
        counts: IntArray,
        val symbols: IntArray,
    ) {
        private val minCode = IntArray(16)
        private val maxCode = IntArray(16) { -1 }
        private val valPtr = IntArray(16)

        init {
            var code = 0
            var ptr = 0
            for (i in 0 until 16) {
                if (counts[i] > 0) {
                    minCode[i] = code
                    maxCode[i] = code + counts[i] - 1
                    valPtr[i] = ptr
                    ptr += counts[i]
                    code += counts[i]
                } else {
                    minCode[i] = -1
                    maxCode[i] = -1
                    valPtr[i] = -1
                }
                code = code shl 1
            }
        }

        fun decodeSymbol(reader: BitReader): Int {
            var code = 0
            for (len in 0 until 16) {
                val bit = reader.readBit()
                code = (code shl 1) or bit
                if (maxCode[len] != -1 && code <= maxCode[len]) {
                    val index = valPtr[len] + (code - minCode[len])
                    return if (index in symbols.indices) symbols[index] else 0
                }
            }
            return 0
        }
    }

    private class BitReader(
        private val data: ByteArray,
        private var offset: Int,
    ) {
        private var bitBuffer: Int = 0
        private var bitsLeft: Int = 0

        fun readBit(): Int {
            if (bitsLeft == 0) {
                refill()
            }
            bitsLeft--
            return (bitBuffer shr bitsLeft) and 1
        }

        fun readSignedBits(n: Int): Int {
            if (n <= 0) return 0
            var v = 0
            for (i in 0 until n) {
                v = (v shl 1) or readBit()
            }
            val signBit = 1 shl (n - 1)
            return if ((v and signBit) == 0) {
                v - ((1 shl n) - 1)
            } else {
                v
            }
        }

        private fun refill() {
            if (offset >= data.size) {
                bitBuffer = 0
                bitsLeft = 8
                return
            }
            val b = data[offset].toInt() and 0xFF
            offset++
            if (b == 0xFF && offset < data.size) {
                val nextB = data[offset].toInt() and 0xFF
                if (nextB == 0x00) {
                    offset++
                }
            }
            bitBuffer = b
            bitsLeft = 8
        }
    }

    public companion object {
        /**
         * Creates a new decoder reading from [r].
         */
        public fun new(r: IoRead): JpegDecoder = JpegDecoder(r)

        /**
         * Creates a new decoder from [input].
         */
        public fun new(input: ByteArray): JpegDecoder = JpegDecoder(input)
        private val ZIGZAG =
            intArrayOf(
                0,
                1,
                8,
                16,
                9,
                2,
                3,
                10,
                17,
                24,
                32,
                25,
                18,
                11,
                4,
                5,
                12,
                19,
                26,
                33,
                40,
                48,
                41,
                34,
                27,
                20,
                13,
                6,
                7,
                14,
                21,
                28,
                35,
                42,
                49,
                56,
                57,
                50,
                43,
                36,
                29,
                22,
                15,
                23,
                30,
                37,
                44,
                51,
                58,
                59,
                52,
                45,
                38,
                31,
                39,
                46,
                53,
                60,
                61,
                54,
                47,
                55,
                62,
                63,
            )

        private fun readAllBytes(r: IoRead): ByteArray {
            val buf = ByteArray(4096)
            val result = mutableListOf<Byte>()
            while (true) {
                val read = r.read(buf)
                if (read <= 0) break
                for (i in 0 until read) {
                    result.add(buf[i])
                }
            }
            return result.toByteArray()
        }

        private fun isXmpPrefix(data: ByteArray, start: Int): Boolean {
            val prefix = "http://ns.adobe.com/xap/1.0/\u0000"
            if (start + prefix.length > data.size) return false
            for (i in prefix.indices) {
                if (data[start + i] != prefix[i].code.toByte()) return false
            }
            return true
        }

        private fun isIccPrefix(data: ByteArray, start: Int): Boolean {
            val prefix = "ICC_PROFILE\u0000"
            if (start + prefix.length > data.size) return false
            for (i in prefix.indices) {
                if (data[start + i] != prefix[i].code.toByte()) return false
            }
            return true
        }

        private fun isPhotoshopPrefix(data: ByteArray, start: Int): Boolean {
            val prefix = "Photoshop 3.0\u0000"
            if (start + prefix.length > data.size) return false
            for (i in prefix.indices) {
                if (data[start + i] != prefix[i].code.toByte()) return false
            }
            return true
        }

        private fun fastIdct(input: IntArray, output: IntArray) {
            val temp = DoubleArray(64)
            val pi = 3.14159265358979323846
            for (row in 0 until 8) {
                val rIdx = row * 8
                for (x in 0 until 8) {
                    var sum = 0.0
                    for (u in 0 until 8) {
                        val cu = if (u == 0) 1.0 / sqrt(2.0) else 1.0
                        sum += cu * input[rIdx + u] * cos((2 * x + 1) * u * pi / 16.0)
                    }
                    temp[rIdx + x] = sum * 0.5
                }
            }

            for (col in 0 until 8) {
                for (y in 0 until 8) {
                    var sum = 0.0
                    for (v in 0 until 8) {
                        val cv = if (v == 0) 1.0 / sqrt(2.0) else 1.0
                        sum += cv * temp[v * 8 + col] * cos((2 * y + 1) * v * pi / 16.0)
                    }
                    output[y * 8 + col] = (sum * 0.5).toInt()
                }
            }
        }
    }
}
