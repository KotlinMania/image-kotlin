// port-lint: source src/io/format.rs
package io.github.kotlinmania.image.io

/**
 * An enumeration of supported image formats.
 *
 * Not all formats support both encoding and decoding.
 */
enum class ImageFormat {
    /** An image in PNG format. */
    Png,

    /** An image in JPEG format. */
    Jpeg,

    /** An image in GIF format. */
    Gif,

    /** An image in WebP format. */
    WebP,

    /** An image in general PNM format. */
    Pnm,

    /** An image in TIFF format. */
    Tiff,

    /** An image in TGA format. */
    Tga,

    /** An image in DDS format. */
    Dds,

    /** An image in BMP format. */
    Bmp,

    /** An image in ICO format. */
    Ico,

    /** An image in Radiance HDR format. */
    Hdr,

    /** An image in OpenEXR format. */
    OpenExr,

    /** An image in farbfeld format. */
    Farbfeld,

    /** An image in AVIF format. */
    Avif,

    /** An image in QOI format. */
    Qoi,

    /** An image in PCX format. */
    Pcx,

    ;

    /**
     * Returns the MIME type for this image format, or `"application/octet-stream"` if no MIME type
     * exists for the format.
     *
     * Notes on a few of the MIME types:
     *  - The portable anymap format has separate MIME types for the pixmap, graymap and bitmap
     *    formats, but this method returns the general `image/x-portable-anymap` MIME type.
     *  - The Targa format has two common MIME types, `image/x-targa` and `image/x-tga`; this
     *    method returns `image/x-targa` for that format.
     *  - The QOI MIME type is still a work in progress. This method returns `image/x-qoi` for
     *    that format.
     */
    fun toMimeType(): String =
        when (this) {
            Avif -> "image/avif"
            Jpeg -> "image/jpeg"
            Png -> "image/png"
            Gif -> "image/gif"
            WebP -> "image/webp"
            Tiff -> "image/tiff"
            Tga -> "image/x-targa"
            Dds -> "image/vnd-ms.dds"
            Bmp -> "image/bmp"
            Ico -> "image/x-icon"
            Hdr -> "image/vnd.radiance"
            OpenExr -> "image/x-exr"
            Pnm -> "image/x-portable-anymap"
            Qoi -> "image/x-qoi"
            Farbfeld -> "application/octet-stream"
            Pcx -> "image/vnd.zbrush.pcx"
        }

    /** Returns whether this `ImageFormat` can in principle be decoded by the library. */
    fun canRead(): Boolean =
        when (this) {
            Png, Gif, Jpeg, WebP, Tiff, Tga, Bmp, Ico, Hdr, OpenExr, Pnm, Farbfeld, Avif, Qoi -> true
            Dds, Pcx -> false
        }

    /** Returns whether this `ImageFormat` can in principle be encoded by the library. */
    fun canWrite(): Boolean =
        when (this) {
            Gif, Ico, Jpeg, Png, Bmp, Tiff, Tga, Pnm, Farbfeld, Avif, WebP, Hdr, OpenExr, Qoi -> true
            Dds, Pcx -> false
        }

    /**
     * Returns the list of applicable extensions for this format.
     *
     * All currently recognized image formats specify at least one extension, but for future
     * compatibility callers should not rely on this fact. The list may be empty if the format has
     * no recognized file representation, for example in case it is used as a purely transient
     * memory format.
     */
    fun extensionsStr(): List<String> =
        when (this) {
            Png -> listOf("png")
            Jpeg -> listOf("jpg", "jpeg")
            Gif -> listOf("gif")
            WebP -> listOf("webp")
            Pnm -> listOf("pbm", "pam", "ppm", "pgm", "pnm")
            Tiff -> listOf("tiff", "tif")
            Tga -> listOf("tga")
            Dds -> listOf("dds")
            Bmp -> listOf("bmp")
            Ico -> listOf("ico")
            Hdr -> listOf("hdr")
            OpenExr -> listOf("exr")
            Farbfeld -> listOf("ff")
            Avif -> listOf("avif")
            Qoi -> listOf("qoi")
            Pcx -> listOf("pcx")
        }

    // Upstream `reading_enabled` / `writing_enabled` are gated on Cargo `feature = "..."` flags.
    // The Kotlin port has no feature-gating mechanism, so each format reports whether the
    // library is willing to attempt decoding/encoding it in principle. `Dds` and `Pcx` remain
    // false (they were unconditionally false in upstream regardless of features).

    /** Returns whether reading is enabled for this `ImageFormat`. */
    fun readingEnabled(): Boolean = canRead()

    /** Returns whether writing is enabled for this `ImageFormat`. */
    fun writingEnabled(): Boolean = canWrite()

    companion object {
        /**
         * Returns the image format specified by a file extension.
         *
         * The extension is matched case-insensitively.
         *
         * Returns `null` if the extension is not recognized.
         */
        fun fromExtension(ext: String): ImageFormat? =
            when (ext.lowercase()) {
                "avif" -> Avif
                "jpg", "jpeg", "jfif" -> Jpeg
                "png", "apng" -> Png
                "gif" -> Gif
                "webp" -> WebP
                "tif", "tiff" -> Tiff
                "tga" -> Tga
                "dds" -> Dds
                "bmp" -> Bmp
                "ico" -> Ico
                "hdr" -> Hdr
                "exr" -> OpenExr
                "pbm", "pam", "ppm", "pgm", "pnm" -> Pnm
                "ff" -> Farbfeld
                "qoi" -> Qoi
                else -> null
            }

        /**
         * Returns the image format inferred from the file extension of the given path string.
         *
         * The extension is the substring after the last `.`. Returns `null` if no extension is
         * present or the extension is not recognized.
         *
         * Upstream `from_path` returns `ImageResult<ImageFormat>` (an error of variant
         * `ImageError::Unsupported` when the extension is missing or unrecognized). The Kotlin
         * counterpart will move to a sealed `ImageResult` once `error.rs` is ported; for now this
         * helper returns `null` for any failure, matching the shape of [fromExtension].
         */
        fun fromPath(path: String): ImageFormat? {
            val name = path.substringAfterLast('/').substringAfterLast('\\')
            val dot = name.lastIndexOf('.')
            if (dot < 0 || dot == name.length - 1) return null
            return fromExtension(name.substring(dot + 1))
        }

        /**
         * Returns the image format specified by a MIME type, or `null` if the MIME type is not
         * recognized.
         */
        fun fromMimeType(mimeType: String): ImageFormat? =
            when (mimeType) {
                "image/avif" -> Avif
                "image/jpeg" -> Jpeg
                "image/png" -> Png
                "image/gif" -> Gif
                "image/webp" -> WebP
                "image/tiff" -> Tiff
                "image/x-targa", "image/x-tga" -> Tga
                "image/vnd-ms.dds" -> Dds
                "image/bmp" -> Bmp
                "image/x-icon", "image/vnd.microsoft.icon" -> Ico
                "image/vnd.radiance" -> Hdr
                "image/x-exr" -> OpenExr
                "image/x-portable-bitmap",
                "image/x-portable-graymap",
                "image/x-portable-pixmap",
                "image/x-portable-anymap",
                -> Pnm
                "image/x-qoi" -> Qoi
                else -> null
            }

        /** Returns all `ImageFormat` variants. */
        fun all(): Sequence<ImageFormat> =
            sequenceOf(
                Gif,
                Ico,
                Jpeg,
                Png,
                Bmp,
                Tiff,
                Tga,
                Pnm,
                Farbfeld,
                Avif,
                WebP,
                OpenExr,
                Qoi,
                Dds,
                Hdr,
                Pcx,
            )
    }
}
