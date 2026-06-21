// port-lint: source src/metadata.rs
package io.github.kotlinmania.image.metadata

/**
 * Describes the transformations to be applied to the image.
 * Compatible with [Exif orientation](https://web.archive.org/web/20200412005226/https://www.impulseadventure.com/photo/exif-orientation.html).
 *
 * Orientation is specified in the file's metadata, and is often written by cameras.
 *
 * You can apply it to an image via `DynamicImage.applyOrientation`.
 */
enum class Orientation {
    /** Do not perform any transformations. */
    NoTransforms,

    /** Rotate by 90 degrees clockwise. */
    Rotate90,

    /** Rotate by 180 degrees. Can be performed in-place. */
    Rotate180,

    /** Rotate by 270 degrees clockwise. Equivalent to rotating by 90 degrees counter-clockwise. */
    Rotate270,

    /** Flip horizontally. Can be performed in-place. */
    FlipHorizontal,

    /** Flip vertically. Can be performed in-place. */
    FlipVertical,

    /** Rotate by 90 degrees clockwise and flip horizontally. */
    Rotate90FlipH,

    /** Rotate by 270 degrees clockwise and flip horizontally. */
    Rotate270FlipH,

    ;

    /**
     * Converts into [Exif orientation](https://web.archive.org/web/20200412005226/https://www.impulseadventure.com/photo/exif-orientation.html).
     */
    fun toExif(): Int =
        when (this) {
            NoTransforms -> 1
            FlipHorizontal -> 2
            Rotate180 -> 3
            FlipVertical -> 4
            Rotate90FlipH -> 5
            Rotate90 -> 6
            Rotate270FlipH -> 7
            Rotate270 -> 8
        }

    companion object {
        /**
         * Converts from [Exif orientation](https://web.archive.org/web/20200412005226/https://www.impulseadventure.com/photo/exif-orientation.html).
         */
        fun fromExif(exifOrientation: Int): Orientation? =
            when (exifOrientation) {
                1 -> NoTransforms
                2 -> FlipHorizontal
                3 -> Rotate180
                4 -> FlipVertical
                5 -> Rotate90FlipH
                6 -> Rotate90
                7 -> Rotate270FlipH
                8 -> Rotate270
                else -> null
            }

        /**
         * Extracts the image orientation from a raw Exif chunk.
         *
         * You can obtain the Exif chunk using `ImageDecoder.exifMetadata`.
         *
         * It is more convenient to use `ImageDecoder.orientation` than to invoke this function.
         * Only use this function if you extract and process the Exif chunk separately.
         */
        fun fromExifChunk(chunk: ByteArray): Orientation? =
            fromExifChunkInner(chunk)?.first

        /**
         * Extracts the image orientation from a raw Exif chunk and sets the orientation
         * in the Exif chunk to [NoTransforms].
         *
         * This is useful if you want to apply the orientation yourself, and then encode the
         * image with the rest of the Exif chunk intact.
         *
         * If the orientation data is not cleared from the Exif chunk after you apply the
         * orientation data yourself, the image will end up being rotated once again by any
         * software that correctly handles Exif, leading to an incorrect result.
         *
         * If the Exif value is present but invalid, `null` is returned and the Exif chunk
         * is not modified.
         */
        fun removeFromExifChunk(chunk: ByteArray): Orientation? {
            val located = fromExifChunkInner(chunk) ?: return null
            val (orientation, offset, endian) = located
            val noOrientation = NoTransforms.toExif()
            when (endian) {
                ExifEndian.Big -> writeU16Be(chunk, offset, noOrientation)
                ExifEndian.Little -> writeU16Le(chunk, offset, noOrientation)
            }
            return orientation
        }

        /**
         * Returns the orientation, the offset in the Exif chunk where it was found, and the
         * Exif chunk endianness.
         */
        private fun fromExifChunkInner(chunk: ByteArray): Triple<Orientation, Int, ExifEndian>? {
            if (chunk.size < 4) return null
            val magic0 = chunk[0].toInt() and 0xff
            val magic1 = chunk[1].toInt() and 0xff
            val magic2 = chunk[2].toInt() and 0xff
            val magic3 = chunk[3].toInt() and 0xff
            return when {
                magic0 == 0x49 && magic1 == 0x49 && magic2 == 42 && magic3 == 0 ->
                    locateOrientationEntry(chunk, ExifEndian.Little)?.let { (o, off) ->
                        Triple(o, off, ExifEndian.Little)
                    }
                magic0 == 0x4d && magic1 == 0x4d && magic2 == 0 && magic3 == 42 ->
                    locateOrientationEntry(chunk, ExifEndian.Big)?.let { (o, off) ->
                        Triple(o, off, ExifEndian.Big)
                    }
                else -> null
            }
        }

        /**
         * Extracted into a helper function to be generic over endianness.
         */
        private fun locateOrientationEntry(
            chunk: ByteArray,
            endian: ExifEndian,
        ): Pair<Orientation, Int>? {
            var pos = 4
            val ifdOffset = readU32(chunk, pos, endian) ?: return null
            pos = ifdOffset.toInt()
            val entries = readU16(chunk, pos, endian) ?: return null
            pos += 2
            for (i in 0 until entries) {
                val tag = readU16(chunk, pos, endian) ?: return null
                pos += 2
                val format = readU16(chunk, pos, endian) ?: return null
                pos += 2
                val count = readU32(chunk, pos, endian) ?: return null
                pos += 4
                val value = readU16(chunk, pos, endian) ?: return null
                pos += 2
                val padding = readU16(chunk, pos, endian) ?: return null
                pos += 2
                if (tag == 0x112 && format == 3 && count == 1u) {
                    // we've read 4 bytes (2 * u16) past the start of the value
                    val offset = pos - 4
                    val orientation = fromExif(minOf(value, 255))
                    return orientation?.let { it to offset }
                }
            }
            // If we reached this point without returning early, there was no orientation
            return null
        }
    }
}

internal enum class ExifEndian { Big, Little }

private fun readU16(chunk: ByteArray, offset: Int, endian: ExifEndian): Int? {
    if (offset < 0 || offset + 2 > chunk.size) return null
    return when (endian) {
        ExifEndian.Little ->
            (chunk[offset].toInt() and 0xff) or
                ((chunk[offset + 1].toInt() and 0xff) shl 8)
        ExifEndian.Big ->
            ((chunk[offset].toInt() and 0xff) shl 8) or
                (chunk[offset + 1].toInt() and 0xff)
    }
}

private fun readU32(chunk: ByteArray, offset: Int, endian: ExifEndian): UInt? {
    if (offset < 0 || offset + 4 > chunk.size) return null
    val b0 = (chunk[offset].toInt() and 0xff).toUInt()
    val b1 = (chunk[offset + 1].toInt() and 0xff).toUInt()
    val b2 = (chunk[offset + 2].toInt() and 0xff).toUInt()
    val b3 = (chunk[offset + 3].toInt() and 0xff).toUInt()
    return when (endian) {
        ExifEndian.Little -> b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        ExifEndian.Big -> (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }
}

private fun writeU16Le(chunk: ByteArray, offset: Int, value: Int) {
    chunk[offset] = (value and 0xff).toByte()
    chunk[offset + 1] = ((value shr 8) and 0xff).toByte()
}

private fun writeU16Be(chunk: ByteArray, offset: Int, value: Int) {
    chunk[offset] = ((value shr 8) and 0xff).toByte()
    chunk[offset + 1] = (value and 0xff).toByte()
}
