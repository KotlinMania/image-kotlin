// port-lint: source io/format.rs
package io.github.kotlinmania.image.io

/**
 * An enumeration of supported image formats.
 *
 * Not all formats support both encoding and decoding.
 */
public enum class ImageFormat {
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
    public fun toMimeType(): String =
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
    public fun canRead(): Boolean =
        when (this) {
            Png, Gif, Jpeg, WebP, Tiff, Tga, Bmp, Ico, Hdr, OpenExr, Pnm, Farbfeld, Avif, Qoi, Dds -> true
            Pcx -> false
        }

    /** Returns whether this `ImageFormat` can in principle be encoded by the library. */
    public fun canWrite(): Boolean =
        when (this) {
            Gif, Ico, Jpeg, Png, Bmp, Tiff, Tga, Pnm, Farbfeld, Avif, WebP, Hdr, OpenExr, Qoi -> true
            Dds, Pcx -> false
        }

    /**
     * Returns the list of applicable extensions for this format.
     */
    public fun extensionsStr(): List<String> =
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

    /** Returns whether reading is enabled for this `ImageFormat`. */
    public fun readingEnabled(): Boolean = canRead()

    /** Returns whether writing is enabled for this `ImageFormat`. */
    public fun writingEnabled(): Boolean = canWrite()

    public companion object {
        /**
         * Returns the image format specified by a file extension.
         */
        public fun fromExtension(ext: String): ImageFormat? =
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
         */
        public fun fromPath(path: String): ImageFormat? {
            val name = path.substringAfterLast('/').substringAfterLast('\\')
            val dot = name.lastIndexOf('.')
            if (dot < 0 || dot == name.length - 1) return null
            return fromExtension(name.substring(dot + 1))
        }

        /**
         * Returns the image format specified by a MIME type, or `null` if the MIME type is not
         * recognized.
         */
        public fun fromMimeType(mimeType: String): ImageFormat? =
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
        public fun all(): Sequence<ImageFormat> =
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
