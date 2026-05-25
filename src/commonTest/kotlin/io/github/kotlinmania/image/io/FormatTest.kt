// port-lint: tests src/io/format.rs
package io.github.kotlinmania.image.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatTest {
    @Test
    fun testImageFormatFromPath() {
        assertEquals(ImageFormat.Jpeg, ImageFormat.fromPath("./a.jpg"))
        assertEquals(ImageFormat.Jpeg, ImageFormat.fromPath("./a.jpeg"))
        assertEquals(ImageFormat.Jpeg, ImageFormat.fromPath("./a.JPEG"))
        assertEquals(ImageFormat.Png, ImageFormat.fromPath("./a.pNg"))
        assertEquals(ImageFormat.Gif, ImageFormat.fromPath("./a.gif"))
        assertEquals(ImageFormat.WebP, ImageFormat.fromPath("./a.webp"))
        assertEquals(ImageFormat.Tiff, ImageFormat.fromPath("./a.tiFF"))
        assertEquals(ImageFormat.Tiff, ImageFormat.fromPath("./a.tif"))
        assertEquals(ImageFormat.Tga, ImageFormat.fromPath("./a.tga"))
        assertEquals(ImageFormat.Dds, ImageFormat.fromPath("./a.dds"))
        assertEquals(ImageFormat.Bmp, ImageFormat.fromPath("./a.bmp"))
        assertEquals(ImageFormat.Ico, ImageFormat.fromPath("./a.Ico"))
        assertEquals(ImageFormat.Hdr, ImageFormat.fromPath("./a.hdr"))
        assertEquals(ImageFormat.OpenExr, ImageFormat.fromPath("./a.exr"))
        assertEquals(ImageFormat.Pnm, ImageFormat.fromPath("./a.pbm"))
        assertEquals(ImageFormat.Pnm, ImageFormat.fromPath("./a.pAM"))
        assertEquals(ImageFormat.Pnm, ImageFormat.fromPath("./a.Ppm"))
        assertEquals(ImageFormat.Pnm, ImageFormat.fromPath("./a.pgm"))
        assertEquals(ImageFormat.Avif, ImageFormat.fromPath("./a.AViF"))
        assertNull(ImageFormat.fromPath("./a.txt"))
        assertNull(ImageFormat.fromPath("./a"))
    }

    @Test
    fun imageFormatsAreRecognized() {
        val allFormats = listOf(
            ImageFormat.Avif, ImageFormat.Png, ImageFormat.Jpeg, ImageFormat.Gif,
            ImageFormat.WebP, ImageFormat.Pnm, ImageFormat.Tiff, ImageFormat.Tga,
            ImageFormat.Dds, ImageFormat.Bmp, ImageFormat.Ico, ImageFormat.Hdr,
            ImageFormat.Farbfeld, ImageFormat.OpenExr,
        )
        for (format in allFormats) {
            for (ext in format.extensionsStr()) {
                val result = ImageFormat.fromPath("file.$ext")
                assertNotNull(result, "Path file.$ext not recognized as $format")
                assertEquals(format, result)
            }
        }
    }

    @Test
    fun all() {
        val allFormats = ImageFormat.all().toSet()
        assertTrue(allFormats.contains(ImageFormat.Avif))
        assertTrue(allFormats.contains(ImageFormat.Gif))
        assertTrue(allFormats.contains(ImageFormat.Bmp))
        assertTrue(allFormats.contains(ImageFormat.Farbfeld))
        assertTrue(allFormats.contains(ImageFormat.Jpeg))
    }

    @Test
    fun readingEnabled() {
        assertTrue(ImageFormat.Jpeg.readingEnabled())
        assertTrue(ImageFormat.Farbfeld.readingEnabled())
        assertTrue(!ImageFormat.Dds.readingEnabled())
    }

    @Test
    fun writingEnabled() {
        assertTrue(ImageFormat.Jpeg.writingEnabled())
        assertTrue(ImageFormat.Farbfeld.writingEnabled())
        assertTrue(!ImageFormat.Dds.writingEnabled())
    }

    @Test
    fun mimeRoundTrip() {
        val seen = mutableSetOf<String>()
        for (format in ImageFormat.all()) {
            val mime = format.toMimeType()
            if (mime == "application/octet-stream") continue
            seen += mime
        }
        assertEquals(ImageFormat.Png, ImageFormat.fromMimeType("image/png"))
        assertEquals(ImageFormat.Jpeg, ImageFormat.fromMimeType("image/jpeg"))
        assertEquals(ImageFormat.Tga, ImageFormat.fromMimeType("image/x-tga"))
        assertEquals(ImageFormat.Pnm, ImageFormat.fromMimeType("image/x-portable-pixmap"))
        assertNull(ImageFormat.fromMimeType("application/unknown"))
        assertTrue(seen.contains("image/avif"))
    }
}
