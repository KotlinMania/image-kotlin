// port-lint: source codecs/dds.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.readExact
import io.github.kotlinmania.image.utils.checkDimensionOverflow

private fun IoRead.readU32Le(): UInt {
    val buf = ByteArray(4)
    readExact(buf)
    val b0 = buf[0].toLong() and 0xFFL
    val b1 = buf[1].toLong() and 0xFFL
    val b2 = buf[2].toLong() and 0xFFL
    val b3 = buf[3].toLong() and 0xFFL
    return ((b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0).toUInt()
}

/**
 * DDS pixel format.
 */
internal class DdsPixelFormat(
    val flags: UInt,
    val fourcc: ByteArray,
    val rgbBitCount: UInt,
    val rBitMask: UInt,
    val gBitMask: UInt,
    val bBitMask: UInt,
    val aBitMask: UInt,
) {
    companion object {
        fun fromReader(r: IoRead): DdsPixelFormat {
            val size = r.readU32Le()
            if (size != 32u) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Dds),
                        "Invalid DDS PixelFormat size: $size (expected 32)",
                    ),
                )
            }

            val flags = r.readU32Le()
            val fourcc = ByteArray(4)
            r.readExact(fourcc)
            val rgbBitCount = r.readU32Le()
            val rBitMask = r.readU32Le()
            val gBitMask = r.readU32Le()
            val bBitMask = r.readU32Le()
            val aBitMask = r.readU32Le()

            return DdsPixelFormat(
                flags = flags,
                fourcc = fourcc,
                rgbBitCount = rgbBitCount,
                rBitMask = rBitMask,
                gBitMask = gBitMask,
                bBitMask = bBitMask,
                aBitMask = aBitMask,
            )
        }
    }
}

/**
 * DDS file header.
 */
internal class DdsHeader(
    val flags: UInt,
    val height: UInt,
    val width: UInt,
    val pitchOrLinearSize: UInt,
    val depth: UInt,
    val mipmapCount: UInt,
    val pixelFormat: DdsPixelFormat,
    val caps: UInt,
    val caps2: UInt,
) {
    companion object {
        private const val REQUIRED_FLAGS: UInt = 0x1007u
        private const val VALID_FLAGS: UInt = 0x008A100Fu

        fun fromReader(r: IoRead): DdsHeader {
            val size = r.readU32Le()
            if (size != 124u) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Dds),
                        "Invalid DDS Header size: $size (expected 124)",
                    ),
                )
            }

            val flags = r.readU32Le()
            if ((flags and (REQUIRED_FLAGS or VALID_FLAGS.inv())) != REQUIRED_FLAGS) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Dds),
                        "Invalid DDS Header flags: 0x${flags.toString(16)}",
                    ),
                )
            }

            val height = r.readU32Le()
            val width = r.readU32Le()
            val pitchOrLinearSize = r.readU32Le()
            val depth = r.readU32Le()
            val mipmapCount = r.readU32Le()

            // Skip dwReserved1 (11 uint32s = 44 bytes)
            val reserved1 = ByteArray(44)
            r.readExact(reserved1)

            val pixelFormat = DdsPixelFormat.fromReader(r)
            val caps = r.readU32Le()
            val caps2 = r.readU32Le()

            // Skip dwCaps3, dwCaps4, dwReserved2 (3 uint32s = 12 bytes)
            val reserved2 = ByteArray(12)
            r.readExact(reserved2)

            return DdsHeader(
                flags = flags,
                height = height,
                width = width,
                pitchOrLinearSize = pitchOrLinearSize,
                depth = depth,
                mipmapCount = mipmapCount,
                pixelFormat = pixelFormat,
                caps = caps,
                caps2 = caps2,
            )
        }
    }
}

/**
 * DirectDraw Surface DX10 extension header.
 */
internal class DdsDx10Header(
    val dxgiFormat: UInt,
    val resourceDimension: UInt,
    val miscFlag: UInt,
    val arraySize: UInt,
    val miscFlags2: UInt,
) {
    fun validate() {
        if (dxgiFormat > 132u) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Dds),
                    "Invalid DXGI format: $dxgiFormat",
                ),
            )
        }

        if (resourceDimension < 2u || resourceDimension > 4u) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Dds),
                    "Invalid resource dimension: $resourceDimension",
                ),
            )
        }

        if (miscFlag != 0x0u && miscFlag != 0x4u) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Dds),
                    "Invalid DX10 misc flags: $miscFlag",
                ),
            )
        }

        if (resourceDimension == 4u && arraySize != 1u) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Dds),
                    "Invalid DX10 array size for 3D texture: $arraySize",
                ),
            )
        }

        if (miscFlags2 > 0x4u) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Dds),
                    "Invalid DX10 misc flags 2: $miscFlags2",
                ),
            )
        }
    }

    companion object {
        fun fromReader(r: IoRead): DdsDx10Header {
            val dxgiFormat = r.readU32Le()
            val resourceDimension = r.readU32Le()
            val miscFlag = r.readU32Le()
            val arraySize = r.readU32Le()
            val miscFlags2 = r.readU32Le()

            val header = DdsDx10Header(
                dxgiFormat = dxgiFormat,
                resourceDimension = resourceDimension,
                miscFlag = miscFlag,
                arraySize = arraySize,
                miscFlags2 = miscFlags2,
            )
            header.validate()
            return header
        }
    }
}

/**
 * The representation of a DDS decoder.
 */
public class DdsDecoder internal constructor(
    private val inner: DxtDecoder,
) : ImageDecoder {

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    public constructor(reader: IoRead) : this(createInner(reader))

    public companion object {
        private val DDS_MAGIC = byteArrayOf(0x44, 0x44, 0x53, 0x20) // "DDS "

        private fun createInner(reader: IoRead): DxtDecoder {
            val magic = ByteArray(4)
            reader.readExact(magic)
            if (!magic.contentEquals(DDS_MAGIC)) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Dds),
                        "DDS signature not found",
                    ),
                )
            }

            val header = DdsHeader.fromReader(reader)

            if ((header.pixelFormat.flags and 0x4u) != 0u) {
                val fourccStr = header.pixelFormat.fourcc.decodeToString()
                val variant =
                    when (fourccStr) {
                        "DXT1" -> DxtVariant.DXT1
                        "DXT3" -> DxtVariant.DXT3
                        "DXT5" -> DxtVariant.DXT5
                        "DX10" -> {
                            val dx10Header = DdsDx10Header.fromReader(reader)
                            when (dx10Header.dxgiFormat) {
                                in 70u..72u -> DxtVariant.DXT1
                                in 73u..75u -> DxtVariant.DXT3
                                in 76u..78u -> DxtVariant.DXT5
                                else -> throw ImageError.Unsupported(
                                    UnsupportedError(
                                        ImageFormatHint.Exact(ImageFormat.Dds),
                                        UnsupportedErrorKind.GenericFeature(
                                            "DDS DXGI Format ${dx10Header.dxgiFormat}",
                                        ),
                                    ),
                                )
                            }
                        }
                        else -> throw ImageError.Unsupported(
                            UnsupportedError(
                                ImageFormatHint.Exact(ImageFormat.Dds),
                                UnsupportedErrorKind.GenericFeature("DDS FourCC $fourccStr"),
                            ),
                        )
                    }

                val bytesPerPixel = variant.colorType().bytesPerPixel()
                if (checkDimensionOverflow(header.width, header.height, bytesPerPixel)) {
                    throw ImageError.Unsupported(
                        UnsupportedError(
                            ImageFormatHint.Exact(ImageFormat.Dds),
                            UnsupportedErrorKind.GenericFeature(
                                "Image dimensions (${header.width}x${header.height}) are too large",
                            ),
                        ),
                    )
                }

                return DxtDecoder.create(reader, header.width, header.height, variant)
            } else {
                throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Dds),
                        UnsupportedErrorKind.Format(ImageFormatHint.Name("DDS")),
                    ),
                )
            }
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = inner.dimensions()

    override fun colorType(): ColorType = inner.colorType()

    override fun readImage(buf: ByteArray) {
        inner.readImage(buf)
    }
}
