// port-lint: source codecs/ico/decoder.rs
package io.github.kotlinmania.image.codecs.ico

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.codecs.bmp.BmpDecoder
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead

/**
 * Errors that can occur during decoding and parsing an ICO image.
 */
public sealed class DecoderError(
    message: String,
) : Exception(message) {
    public object NoEntries : DecoderError("ICO directory contains no image")

    public object IcoEntryTooManyPlanesOrHotspot :
        DecoderError("ICO image entry has too many color planes or too large hotspot value")

    public object IcoEntryTooManyBitsPerPixelOrHotspot :
        DecoderError("ICO image entry has too many bits per pixel or too large hotspot value")

    public object PngShorterThanHeader :
        DecoderError("Entry specified a length that is shorter than PNG header!")

    public object PngNotRgba :
        DecoderError("The PNG is not in RGBA format!")

    public object InvalidDataSize :
        DecoderError("ICO image data size did not match expected size")

    public data class ImageEntryDimensionMismatch(
        val format: String,
        val entryWidth: UInt,
        val entryHeight: UInt,
        val imageWidth: UInt,
        val imageHeight: UInt,
    ) : DecoderError("Entry($entryWidth, $entryHeight) and $format($imageWidth, $imageHeight) dimensions do not match!")
}

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

internal data class DirEntry(
    val width: UByte,
    val height: UByte,
    val colorCount: UByte,
    val reserved: UByte,
    val numColorPlanes: Int,
    val bitsPerPixel: Int,
    val imageLength: UInt,
    val imageOffset: UInt,
) {
    fun realWidth(): UInt = if (width == 0.toUByte()) 256u else width.toUInt()

    fun realHeight(): UInt = if (height == 0.toUByte()) 256u else height.toUInt()

    fun matchesDimensions(w: UInt, h: UInt): Boolean =
        realWidth() == minOf(w, 256u) && realHeight() == minOf(h, 256u)
}

/**
 * An ICO decoder.
 */
public class IcoDecoder(
    private val reader: IoRead,
) : ImageDecoder {
    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    private val selectedEntry: DirEntry
    private val innerDecoder: ImageDecoder

    init {
        val allBytes = readAllBytes(reader)
        val entries = readEntries(allBytes)
        val entry = bestEntry(entries)
        selectedEntry = entry

        val offset = entry.imageOffset.toInt()
        val length = entry.imageLength.toInt()
        if (offset < 0 || offset > allBytes.size) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Ico),
                    DecoderError.InvalidDataSize.message ?: "",
                ),
            )
        }
        val end = minOf(allBytes.size, offset + length)
        val entryBytes = allBytes.copyOfRange(offset, end)

        val isPng = isPngData(entryBytes)
        if (isPng) {
            if (entryBytes.size < PNG_SIGNATURE.size) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Ico),
                        DecoderError.PngShorterThanHeader.message ?: "",
                    ),
                )
            }
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Ico),
                    "PNG inner format not supported in ICO decoder",
                ),
            )
        } else {
            // BMP entry in ICO format
            val hasBmHeader = entryBytes.size >= 2 && entryBytes[0] == 'B'.code.toByte() && entryBytes[1] == 'M'.code.toByte()
            innerDecoder =
                if (hasBmHeader) {
                    BmpDecoder(entryBytes, false)
                } else {
                    BmpDecoder(entryBytes, true)
                }
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = innerDecoder.dimensions()

    override fun colorType(): ColorType = innerDecoder.colorType()

    override fun readImage(buf: ByteArray) {
        val (w, h) = innerDecoder.dimensions()
        if (!selectedEntry.matchesDimensions(w, h)) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Ico),
                    DecoderError
                        .ImageEntryDimensionMismatch(
                            "BMP",
                            selectedEntry.realWidth(),
                            selectedEntry.realHeight(),
                            w,
                            h,
                        ).message ?: "",
                ),
            )
        }
        innerDecoder.readImage(buf)
    }

    private companion object {
        private fun readAllBytes(reader: IoRead): ByteArray {
            val byteList = ArrayList<Byte>()
            val temp = ByteArray(4096)
            var n: Int
            while (reader.read(temp).also { n = it } > 0) {
                for (i in 0 until n) {
                    byteList.add(temp[i])
                }
            }
            return byteList.toByteArray()
        }

        private fun readEntries(bytes: ByteArray): List<DirEntry> {
            if (bytes.size < 6) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Ico),
                        DecoderError.NoEntries.message ?: "",
                    ),
                )
            }
            val count = readU16Le(bytes, 4)

            if (count == 0) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Ico),
                        DecoderError.NoEntries.message ?: "",
                    ),
                )
            }

            val entries = ArrayList<DirEntry>(count)
            var pos = 6
            for (i in 0 until count) {
                if (pos + 16 > bytes.size) {
                    throw ImageError.Decoding(
                        DecodingError(
                            ImageFormatHint.Exact(ImageFormat.Ico),
                            DecoderError.InvalidDataSize.message ?: "",
                        ),
                    )
                }
                val width = (bytes[pos].toInt() and 0xFF).toUByte()
                val height = (bytes[pos + 1].toInt() and 0xFF).toUByte()
                val colorCount = (bytes[pos + 2].toInt() and 0xFF).toUByte()
                val res = (bytes[pos + 3].toInt() and 0xFF).toUByte()
                val planes = readU16Le(bytes, pos + 4)
                if (planes > 256) {
                    throw ImageError.Decoding(
                        DecodingError(
                            ImageFormatHint.Exact(ImageFormat.Ico),
                            DecoderError.IcoEntryTooManyPlanesOrHotspot.message ?: "",
                        ),
                    )
                }
                val bpp = readU16Le(bytes, pos + 6)
                if (bpp > 256) {
                    throw ImageError.Decoding(
                        DecodingError(
                            ImageFormatHint.Exact(ImageFormat.Ico),
                            DecoderError.IcoEntryTooManyBitsPerPixelOrHotspot.message ?: "",
                        ),
                    )
                }
                val imageLength = readU32Le(bytes, pos + 8).toUInt()
                val imageOffset = readU32Le(bytes, pos + 12).toUInt()
                pos += 16

                entries.add(
                    DirEntry(
                        width = width,
                        height = height,
                        colorCount = colorCount,
                        reserved = res,
                        numColorPlanes = planes,
                        bitsPerPixel = bpp,
                        imageLength = imageLength,
                        imageOffset = imageOffset,
                    ),
                )
            }
            return entries
        }

        private fun bestEntry(entries: List<DirEntry>): DirEntry {
            if (entries.isEmpty()) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Ico),
                        DecoderError.NoEntries.message ?: "",
                    ),
                )
            }
            var best = entries.first()
            var bestBpp = best.bitsPerPixel
            var bestArea = best.realWidth().toLong() * best.realHeight().toLong()

            for (i in 1 until entries.size) {
                val e = entries[i]
                val bpp = e.bitsPerPixel
                val area = e.realWidth().toLong() * e.realHeight().toLong()
                if (bpp > bestBpp || (bpp == bestBpp && area > bestArea)) {
                    best = e
                    bestBpp = bpp
                    bestArea = area
                }
            }
            return best
        }

        private fun isPngData(bytes: ByteArray): Boolean {
            if (bytes.size < PNG_SIGNATURE.size) return false
            for (i in PNG_SIGNATURE.indices) {
                if (bytes[i] != PNG_SIGNATURE[i]) return false
            }
            return true
        }

        private fun readU16Le(buf: ByteArray, offset: Int): Int {
            val b0 = buf[offset].toInt() and 0xFF
            val b1 = buf[offset + 1].toInt() and 0xFF
            return (b1 shl 8) or b0
        }

        private fun readU32Le(buf: ByteArray, offset: Int): Long {
            val b0 = buf[offset].toLong() and 0xFFL
            val b1 = buf[offset + 1].toLong() and 0xFFL
            val b2 = buf[offset + 2].toLong() and 0xFFL
            val b3 = buf[offset + 3].toLong() and 0xFFL
            return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        }
    }
}
