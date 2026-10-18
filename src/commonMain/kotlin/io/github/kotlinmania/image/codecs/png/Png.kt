// port-lint: source codecs/png.rs
package io.github.kotlinmania.image.codecs.png

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.MethodSealedToImage
import io.github.kotlinmania.image.io.readExact
import io.github.kotlinmania.image.io.writeAll

/**
 * The first eight bytes of a PNG file always contain the following values:
 * 137, 80, 78, 71, 13, 10, 26, 10
 */
public val PNG_SIGNATURE: ByteArray =
    byteArrayOf(
        137.toByte(),
        80.toByte(),
        78.toByte(),
        71.toByte(),
        13.toByte(),
        10.toByte(),
        26.toByte(),
        10.toByte(),
    )

/**
 * DEFLATE compression level of a PNG encoder.
 */
public sealed class CompressionType {
    /** Default compression level */
    public data object Default : CompressionType()

    /** Fast, minimal compression */
    public data object Fast : CompressionType()

    /** High compression level */
    public data object Best : CompressionType()

    /** No compression whatsoever */
    public data object Uncompressed : CompressionType()

    /** Detailed compression level between 1 and 9 */
    public data class Level(
        public val level: UByte,
    ) : CompressionType()
}

/**
 * Filter algorithms used to process image data to improve compression.
 */
public enum class FilterType {
    /** No processing done */
    NoFilter,

    /** Filters based on previous pixel in the same scanline */
    Sub,

    /** Filters based on the scanline above */
    Up,

    /** Filters based on the average of left and right neighbor pixels */
    Avg,

    /** Paeth filter algorithm */
    Paeth,

    /** Heuristic selection of filter per scanline */
    Adaptive,
}

/**
 * PNG encoder representation.
 */
public class PngEncoder internal constructor(
    private val writer: IoWrite,
    private val compression: CompressionType = CompressionType.Fast,
    private val filter: FilterType = FilterType.Adaptive,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    public constructor(
        writeBuffer: BufferIoWrite,
        compression: CompressionType,
        filter: FilterType,
    ) : this(writeBuffer as IoWrite, compression, filter)

    private var iccProfile: ByteArray = ByteArray(0)
    private var exifMetadata: ByteArray = ByteArray(0)

    override fun setIccProfile(iccProfile: ByteArray) {
        this.iccProfile = iccProfile
    }

    override fun setExifMetadata(exif: ByteArray) {
        this.exifMetadata = exif
    }

    /**
     * Encodes the image buffer to PNG format.
     */
    public fun encode(
        data: ByteArray,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        val expectedBufferLen = color.bufferSize(width, height)
        require(expectedBufferLen == data.size.toULong()) {
            "Invalid buffer length: expected $expectedBufferLen got ${data.size} for ${width}x$height image"
        }

        when (color) {
            ExtendedColorType.L8,
            ExtendedColorType.L16,
            ExtendedColorType.La8,
            ExtendedColorType.La16,
            ExtendedColorType.Rgb8,
            ExtendedColorType.Rgb16,
            ExtendedColorType.Rgba8,
            ExtendedColorType.Rgba16,
            -> {
                // Write PNG signature
                writer.writeAll(PNG_SIGNATURE)
                // Write minimal uncompressed PNG chunks (IHDR, IDAT, IEND) or delegate
                val w = width.toInt()
                val h = height.toInt()
                writeIhdr(w, h, color)
                writeIdat(data, w, h, color)
                writeIend()
            }
            else -> throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Png),
                    UnsupportedErrorKind.Color(color),
                ),
            )
        }
    }

    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        encode(buf, width, height, colorType)
    }

    override fun makeCompatibleImg(
        sealed: MethodSealedToImage,
        input: DynamicImage,
    ): DynamicImage? = null

    private fun writeIhdr(width: Int, height: Int, color: ExtendedColorType) {
        val (bitDepth, colorTypeByte) =
            when (color) {
                ExtendedColorType.L8 -> 8 to 0
                ExtendedColorType.L16 -> 16 to 0
                ExtendedColorType.La8 -> 8 to 4
                ExtendedColorType.La16 -> 16 to 4
                ExtendedColorType.Rgb8 -> 8 to 2
                ExtendedColorType.Rgb16 -> 16 to 2
                ExtendedColorType.Rgba8 -> 8 to 6
                ExtendedColorType.Rgba16 -> 16 to 6
                else -> 8 to 6
            }

        val chunkData = ByteArray(13)
        chunkData[0] = ((width shr 24) and 0xFF).toByte()
        chunkData[1] = ((width shr 16) and 0xFF).toByte()
        chunkData[2] = ((width shr 8) and 0xFF).toByte()
        chunkData[3] = (width and 0xFF).toByte()

        chunkData[4] = ((height shr 24) and 0xFF).toByte()
        chunkData[5] = ((height shr 16) and 0xFF).toByte()
        chunkData[6] = ((height shr 8) and 0xFF).toByte()
        chunkData[7] = (height and 0xFF).toByte()

        chunkData[8] = bitDepth.toByte()
        chunkData[9] = colorTypeByte.toByte()
        chunkData[10] = 0 // Compression method: deflate
        chunkData[11] = 0 // Filter method: standard
        chunkData[12] = 0 // Interlace method: none

        writeChunk("IHDR", chunkData)
    }

    private fun writeIdat(data: ByteArray, width: Int, height: Int, color: ExtendedColorType) {
        val bytesPerPixel = (color.bitsPerPixel().toInt() + 7) / 8
        val scanlineLength = width * bytesPerPixel
        // Add 1 filter byte (0 = NoFilter) per scanline
        val rawData = ByteArray(height * (1 + scanlineLength))
        var srcPos = 0
        var dstPos = 0
        repeat(height) {
            rawData[dstPos++] = 0 // Filter type 0 (None)
            data.copyInto(rawData, dstPos, srcPos, srcPos + scanlineLength)
            dstPos += scanlineLength
            srcPos += scanlineLength
        }

        // Deflate uncompressed blocks (stored blocks)
        val deflated = deflateStore(rawData)
        writeChunk("IDAT", deflated)
    }

    private fun writeIend() {
        writeChunk("IEND", ByteArray(0))
    }

    private fun writeChunk(type: String, data: ByteArray) {
        val typeBytes = type.encodeToByteArray()
        val len = data.size
        val lenBytes =
            byteArrayOf(
                ((len shr 24) and 0xFF).toByte(),
                ((len shr 16) and 0xFF).toByte(),
                ((len shr 8) and 0xFF).toByte(),
                (len and 0xFF).toByte(),
            )
        writer.writeAll(lenBytes)
        writer.writeAll(typeBytes)
        if (data.isNotEmpty()) {
            writer.writeAll(data)
        }

        // CRC-32 over type + data
        val crc = crc32(typeBytes, data)
        val crcBytes =
            byteArrayOf(
                ((crc shr 24) and 0xFF).toByte(),
                ((crc shr 16) and 0xFF).toByte(),
                ((crc shr 8) and 0xFF).toByte(),
                (crc and 0xFF).toByte(),
            )
        writer.writeAll(crcBytes)
    }

    private fun deflateStore(data: ByteArray): ByteArray {
        val out = mutableListOf<Byte>()
        // zlib header: CMF = 0x78 (deflate, 32K window), FLG = 0x01 (check bits)
        out.add(0x78.toByte())
        out.add(0x01.toByte())

        // Max uncompressed block size in Deflate is 65535
        val maxBlockSize = 65535
        var pos = 0
        while (pos < data.size) {
            val remaining = data.size - pos
            val blockSize = remaining.coerceAtMost(maxBlockSize)
            val isFinal = (pos + blockSize >= data.size)
            val bheader = if (isFinal) 0x01 else 0x00
            out.add(bheader.toByte())

            val len = blockSize
            val nlen = blockSize.inv() and 0xFFFF
            out.add((len and 0xFF).toByte())
            out.add(((len shr 8) and 0xFF).toByte())
            out.add((nlen and 0xFF).toByte())
            out.add(((nlen shr 8) and 0xFF).toByte())

            for (i in 0 until blockSize) {
                out.add(data[pos + i])
            }
            pos += blockSize
        }

        // Adler-32 checksum of uncompressed data
        val adler = adler32(data)
        out.add(((adler shr 24) and 0xFF).toByte())
        out.add(((adler shr 16) and 0xFF).toByte())
        out.add(((adler shr 8) and 0xFF).toByte())
        out.add((adler and 0xFF).toByte())

        return out.toByteArray()
    }

    private fun adler32(data: ByteArray): Int {
        var s1 = 1
        var s2 = 0
        for (b in data) {
            s1 = (s1 + (b.toInt() and 0xFF)) % 65521
            s2 = (s2 + s1) % 65521
        }
        return (s2 shl 16) or s1
    }

    private fun crc32(type: ByteArray, data: ByteArray): Int {
        var crc = -1
        for (b in type) {
            crc = CRC_TABLE[(crc xor (b.toInt() and 0xFF)) and 0xFF] xor (crc ushr 8)
        }
        for (b in data) {
            crc = CRC_TABLE[(crc xor (b.toInt() and 0xFF)) and 0xFF] xor (crc ushr 8)
        }
        return crc.inv()
    }

    private companion object {
        val CRC_TABLE: IntArray =
            IntArray(256) { i ->
                var c = i
                repeat(8) {
                    c = if ((c and 1) != 0) -0x124774cd xor (c ushr 1) else c ushr 1
                }
                c
            }
    }
}

/**
 * PNG image decoder.
 */
public class PngDecoder internal constructor(
    private val reader: IoRead,
) : ImageDecoder {
    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    private var width: UInt = 0u
    private var height: UInt = 0u
    private var colorType: ColorType = ColorType.Rgba8
    private var bitDepth: Int = 8
    private var pngColorTypeByte: Int = 6
    private var palette: ByteArray? = null
    private val idatChunks: MutableList<ByteArray> = mutableListOf()
    private var iccProfileData: ByteArray? = null
    private var exifData: ByteArray? = null

    init {
        parseChunks()
    }

    private fun parseChunks() {
        val sig = ByteArray(8)
        reader.readExact(sig)
        if (!sig.contentEquals(PNG_SIGNATURE)) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Png),
                    IllegalArgumentException("Invalid PNG signature"),
                ),
            )
        }

        while (true) {
            val lenBuf = ByteArray(4)
            val n =
                try {
                    reader.read(lenBuf)
                } catch (_: Throwable) {
                    0
                }
            if (n < 4) break
            val len =
                ((lenBuf[0].toInt() and 0xFF) shl 24) or
                    ((lenBuf[1].toInt() and 0xFF) shl 16) or
                    ((lenBuf[2].toInt() and 0xFF) shl 8) or
                    (lenBuf[3].toInt() and 0xFF)

            val typeBuf = ByteArray(4)
            reader.readExact(typeBuf)
            val type = typeBuf.decodeToString()

            val data = ByteArray(len)
            if (len > 0) {
                reader.readExact(data)
            }
            val crc = ByteArray(4)
            reader.readExact(crc)

            when (type) {
                "IHDR" -> {
                    width =
                        (
                            ((data[0].toInt() and 0xFF) shl 24) or
                                ((data[1].toInt() and 0xFF) shl 16) or
                                ((data[2].toInt() and 0xFF) shl 8) or
                                (data[3].toInt() and 0xFF)
                        ).toUInt()
                    height =
                        (
                            ((data[4].toInt() and 0xFF) shl 24) or
                                ((data[5].toInt() and 0xFF) shl 16) or
                                ((data[6].toInt() and 0xFF) shl 8) or
                                (data[7].toInt() and 0xFF)
                        ).toUInt()
                    bitDepth = data[8].toInt() and 0xFF
                    pngColorTypeByte = data[9].toInt() and 0xFF
                    colorType =
                        when (pngColorTypeByte) {
                            0 -> if (bitDepth == 16) ColorType.L16 else ColorType.L8
                            2 -> if (bitDepth == 16) ColorType.Rgb16 else ColorType.Rgb8
                            3 -> ColorType.Rgb8
                            4 -> if (bitDepth == 16) ColorType.La16 else ColorType.La8
                            6 -> if (bitDepth == 16) ColorType.Rgba16 else ColorType.Rgba8
                            else -> ColorType.Rgba8
                        }
                }
                "PLTE" -> palette = data
                "IDAT" -> idatChunks.add(data)
                "iCCP" -> iccProfileData = data
                "eXIf" -> exifData = data
                "IEND" -> break
            }
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType = colorType

    override fun iccProfile(): ByteArray? = iccProfileData

    override fun exifMetadata(): ByteArray? = exifData

    override fun readImage(buf: ByteArray) {
        val totalIdatSize = idatChunks.sumOf { it.size }
        val compressed = ByteArray(totalIdatSize)
        var offset = 0
        for (chunk in idatChunks) {
            chunk.copyInto(compressed, offset)
            offset += chunk.size
        }

        val uncompressed = PngInflate.decompressZlib(compressed)

        val bytesPerPixel =
            when (pngColorTypeByte) {
                0 -> if (bitDepth == 16) 2 else 1
                2 -> if (bitDepth == 16) 6 else 3
                3 -> 1
                4 -> if (bitDepth == 16) 4 else 2
                6 -> if (bitDepth == 16) 8 else 4
                else -> 4
            }
        val scanlineBytes =
            if (bitDepth < 8) {
                (width.toInt() * bitDepth + 7) / 8
            } else {
                width.toInt() * bytesPerPixel
            }

        val bpp = maxOf(1, bytesPerPixel)
        val rawScanlines = ByteArray(height.toInt() * scanlineBytes)

        var srcOff = 0
        val prevScanline = ByteArray(scanlineBytes)
        val currentScanline = ByteArray(scanlineBytes)

        for (y in 0 until height.toInt()) {
            if (srcOff >= uncompressed.size) break
            val filterType = uncompressed[srcOff++].toInt() and 0xFF
            for (x in 0 until scanlineBytes) {
                val raw = uncompressed[srcOff++].toInt() and 0xFF
                val a = if (x >= bpp) currentScanline[x - bpp].toInt() and 0xFF else 0
                val b = prevScanline[x].toInt() and 0xFF
                val c = if (x >= bpp) prevScanline[x - bpp].toInt() and 0xFF else 0
                val recon =
                    when (filterType) {
                        0 -> raw
                        1 -> (raw + a) and 0xFF
                        2 -> (raw + b) and 0xFF
                        3 -> (raw + ((a + b) / 2)) and 0xFF
                        4 -> (raw + paethPredictor(a, b, c)) and 0xFF
                        else -> raw
                    }
                currentScanline[x] = recon.toByte()
            }
            currentScanline.copyInto(rawScanlines, y * scanlineBytes)
            currentScanline.copyInto(prevScanline)
        }

        if (pngColorTypeByte == 3) {
            val pal = palette ?: ByteArray(0)
            var dst = 0
            for (i in 0 until (width * height).toInt()) {
                val idx = (rawScanlines[i].toInt() and 0xFF) * 3
                if (idx + 2 < pal.size) {
                    buf[dst++] = pal[idx]
                    buf[dst++] = pal[idx + 1]
                    buf[dst++] = pal[idx + 2]
                } else {
                    buf[dst++] = 0
                    buf[dst++] = 0
                    buf[dst++] = 0
                }
            }
        } else {
            rawScanlines.copyInto(buf, 0, 0, minOf(rawScanlines.size, buf.size))
        }
    }

    private fun paethPredictor(a: Int, b: Int, c: Int): Int {
        val p = a + b - c
        val pa = kotlin.math.abs(p - a)
        val pb = kotlin.math.abs(p - b)
        val pc = kotlin.math.abs(p - c)
        return when {
            pa <= pb && pa <= pc -> a
            pb <= pc -> b
            else -> c
        }
    }
}

internal object PngInflate {
    private val BASE_LENGTH =
        intArrayOf(
            3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
            35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258,
        )
    private val EXTRA_LENGTH_BITS =
        intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
            3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0,
        )
    private val BASE_DIST =
        intArrayOf(
            1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
            257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577,
        )
    private val EXTRA_DIST_BITS =
        intArrayOf(
            0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
            7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13,
        )
    private val CODE_LENGTH_ORDER =
        intArrayOf(
            16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15,
        )

    private val FIXED_LIT_TREE: HuffmanTree by lazy {
        val lengths = IntArray(288)
        for (i in 0..143) lengths[i] = 8
        for (i in 144..255) lengths[i] = 9
        for (i in 256..279) lengths[i] = 7
        for (i in 280..287) lengths[i] = 8
        buildHuffmanTree(lengths)
    }

    private val FIXED_DIST_TREE: HuffmanTree by lazy {
        val lengths = IntArray(32) { 5 }
        buildHuffmanTree(lengths)
    }

    class BitReader(private val data: ByteArray, private var offset: Int = 0) {
        private var bitBuf = 0
        private var bitCount = 0

        fun readBits(n: Int): Int {
            while (bitCount < n) {
                if (offset >= data.size) {
                    throw IllegalArgumentException("Unexpected end of compressed stream")
                }
                bitBuf = bitBuf or ((data[offset++].toInt() and 0xFF) shl bitCount)
                bitCount += 8
            }
            val mask = (1 shl n) - 1
            val res = bitBuf and mask
            bitBuf = bitBuf ushr n
            bitCount -= n
            return res
        }

        fun alignToByte() {
            bitBuf = 0
            bitCount = 0
        }

        fun readBytes(dest: ByteArray, count: Int) {
            alignToByte()
            if (offset + count > data.size) throw IllegalArgumentException("Unexpected end of compressed stream")
            data.copyInto(dest, 0, offset, offset + count)
            offset += count
        }
    }

    class HuffmanTree(
        private val left: IntArray,
        private val right: IntArray,
        private val symbol: IntArray,
    ) {
        fun decodeSymbol(reader: BitReader): Int {
            var node = 0
            while (symbol[node] < 0) {
                val bit = reader.readBits(1)
                node = if (bit == 0) left[node] else right[node]
                if (node < 0) throw IllegalArgumentException("Invalid Huffman code in compressed stream")
            }
            return symbol[node]
        }
    }

    private fun buildHuffmanTree(lengths: IntArray): HuffmanTree {
        val maxLen = lengths.maxOrNull() ?: 0
        if (maxLen == 0) {
            return HuffmanTree(IntArray(1) { -1 }, IntArray(1) { -1 }, IntArray(1) { -1 })
        }
        val blCount = IntArray(maxLen + 1)
        for (len in lengths) {
            if (len > 0) blCount[len]++
        }
        val nextCode = IntArray(maxLen + 1)
        var code = 0
        for (bits in 1..maxLen) {
            code = (code + blCount[bits - 1]) shl 1
            nextCode[bits] = code
        }

        val maxNodes = lengths.size * 2 + 10
        val left = IntArray(maxNodes) { -1 }
        val right = IntArray(maxNodes) { -1 }
        val symbol = IntArray(maxNodes) { -1 }
        var nextNode = 1

        for (sym in lengths.indices) {
            val len = lengths[sym]
            if (len == 0) continue
            val c = nextCode[len]++
            var node = 0
            for (bit in len - 1 downTo 0) {
                val b = (c ushr bit) and 1
                if (b == 0) {
                    if (left[node] == -1) {
                        left[node] = nextNode++
                    }
                    node = left[node]
                } else {
                    if (right[node] == -1) {
                        right[node] = nextNode++
                    }
                    node = right[node]
                }
            }
            symbol[node] = sym
        }
        return HuffmanTree(left, right, symbol)
    }

    fun decompressZlib(input: ByteArray): ByteArray {
        if (input.size < 6) throw IllegalArgumentException("Zlib data too short")
        val cmf = input[0].toInt() and 0xFF
        val flg = input[1].toInt() and 0xFF
        if ((cmf * 256 + flg) % 31 != 0) {
            throw IllegalArgumentException("Invalid zlib header checksum")
        }
        if ((cmf and 0x0F) != 8) {
            throw IllegalArgumentException("Unsupported zlib compression method: ${cmf and 0x0F}")
        }
        var startOffset = 2
        if ((flg and 0x20) != 0) {
            startOffset += 4
        }

        val reader = BitReader(input, startOffset)
        val out = mutableListOf<Byte>()

        var isFinal = false
        while (!isFinal) {
            isFinal = reader.readBits(1) == 1
            val btype = reader.readBits(2)
            when (btype) {
                0 -> {
                    reader.alignToByte()
                    val len = reader.readBits(8) or (reader.readBits(8) shl 8)
                    val nlen = reader.readBits(8) or (reader.readBits(8) shl 8)
                    if (len != (nlen.inv() and 0xFFFF)) {
                        throw IllegalArgumentException("Invalid uncompressed block length in deflate")
                    }
                    val block = ByteArray(len)
                    reader.readBytes(block, len)
                    for (b in block) out.add(b)
                }
                1 -> {
                    inflateBlock(reader, FIXED_LIT_TREE, FIXED_DIST_TREE, out)
                }
                2 -> {
                    val hlit = reader.readBits(5) + 257
                    val hdist = reader.readBits(5) + 1
                    val hclen = reader.readBits(4) + 4

                    val codeLengthLengths = IntArray(19)
                    for (i in 0 until hclen) {
                        codeLengthLengths[CODE_LENGTH_ORDER[i]] = reader.readBits(3)
                    }
                    val codeLengthTree = buildHuffmanTree(codeLengthLengths)

                    val litDistLengths = IntArray(hlit + hdist)
                    var idx = 0
                    while (idx < hlit + hdist) {
                        val sym = codeLengthTree.decodeSymbol(reader)
                        when (sym) {
                            in 0..15 -> litDistLengths[idx++] = sym
                            16 -> {
                                val repeat = 3 + reader.readBits(2)
                                val prev = if (idx > 0) litDistLengths[idx - 1] else 0
                                repeat(repeat) { litDistLengths[idx++] = prev }
                            }
                            17 -> {
                                val repeat = 3 + reader.readBits(3)
                                repeat(repeat) { litDistLengths[idx++] = 0 }
                            }
                            18 -> {
                                val repeat = 11 + reader.readBits(7)
                                repeat(repeat) { litDistLengths[idx++] = 0 }
                            }
                            else -> throw IllegalArgumentException("Invalid code length symbol: $sym")
                        }
                    }

                    val litLengths = litDistLengths.copyOfRange(0, hlit)
                    val distLengths = litDistLengths.copyOfRange(hlit, hlit + hdist)
                    val litTree = buildHuffmanTree(litLengths)
                    val distTree = buildHuffmanTree(distLengths)

                    inflateBlock(reader, litTree, distTree, out)
                }
                else -> throw IllegalArgumentException("Invalid deflate block type: $btype")
            }
        }
        return out.toByteArray()
    }

    private fun inflateBlock(
        reader: BitReader,
        litTree: HuffmanTree,
        distTree: HuffmanTree,
        out: MutableList<Byte>,
    ) {
        while (true) {
            val sym = litTree.decodeSymbol(reader)
            if (sym < 256) {
                out.add(sym.toByte())
            } else if (sym == 256) {
                break
            } else {
                val lenIdx = sym - 257
                if (lenIdx !in BASE_LENGTH.indices) throw IllegalArgumentException("Invalid length code: $sym")
                val extraBits = EXTRA_LENGTH_BITS[lenIdx]
                val length = BASE_LENGTH[lenIdx] + if (extraBits > 0) reader.readBits(extraBits) else 0

                val distSym = distTree.decodeSymbol(reader)
                if (distSym !in BASE_DIST.indices) throw IllegalArgumentException("Invalid distance code: $distSym")
                val distExtraBits = EXTRA_DIST_BITS[distSym]
                val distance = BASE_DIST[distSym] + if (distExtraBits > 0) reader.readBits(distExtraBits) else 0

                if (distance > out.size) {
                    throw IllegalArgumentException("Distance exceeds output buffer size: $distance > ${out.size}")
                }
                for (i in 0 until length) {
                    out.add(out[out.size - distance])
                }
            }
        }
    }
}
