// port-lint: tests images/dynimage.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.imageops.FilterType
import io.github.kotlinmania.image.metadata.Cicp
import io.github.kotlinmania.image.metadata.CicpColorPrimaries
import io.github.kotlinmania.image.metadata.CicpTransform
import io.github.kotlinmania.image.metadata.Orientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class DynimageTests {
    @Test
    fun testEmptyFile() {
        assertFailsWith<ImageError> {
            loadFromMemory(byteArrayOf())
        }
    }

    private fun testGrayscale(img: DynamicImage, alphaDiscarded: Boolean) {
        img.putPixel(0u, 0u, Rgba(255u, 0u, 0u, 100u))
        val expectedAlpha: UByte = if (alphaDiscarded) 255u else 100u
        val pixel = img.grayscale().getPixel(0u, 0u)
        assertEquals(54u, pixel.r)
        assertEquals(54u, pixel.g)
        assertEquals(54u, pixel.b)
        assertEquals(expectedAlpha, pixel.a)
    }

    private fun testGrayscaleAlphaDiscarded(img: DynamicImage) {
        testGrayscale(img, true)
    }

    private fun testGrayscaleAlphaPreserved(img: DynamicImage) {
        testGrayscale(img, false)
    }

    @Test
    fun testGrayscaleLuma8() {
        testGrayscaleAlphaDiscarded(DynamicImage.newLuma8(1u, 1u))
        testGrayscaleAlphaDiscarded(DynamicImage.new(1u, 1u, ColorType.L8))
    }

    @Test
    fun testGrayscaleLumaA8() {
        testGrayscaleAlphaPreserved(DynamicImage.newLumaA8(1u, 1u))
        testGrayscaleAlphaPreserved(DynamicImage.new(1u, 1u, ColorType.La8))
    }

    @Test
    fun testGrayscaleRgb8() {
        testGrayscaleAlphaDiscarded(DynamicImage.newRgb8(1u, 1u))
        testGrayscaleAlphaDiscarded(DynamicImage.new(1u, 1u, ColorType.Rgb8))
    }

    @Test
    fun testGrayscaleRgba8() {
        testGrayscaleAlphaPreserved(DynamicImage.newRgba8(1u, 1u))
        testGrayscaleAlphaPreserved(DynamicImage.new(1u, 1u, ColorType.Rgba8))
    }

    @Test
    fun testGrayscaleLuma16() {
        testGrayscaleAlphaDiscarded(DynamicImage.newLuma16(1u, 1u))
        testGrayscaleAlphaDiscarded(DynamicImage.new(1u, 1u, ColorType.L16))
    }

    @Test
    fun testGrayscaleLumaA16() {
        testGrayscaleAlphaPreserved(DynamicImage.newLumaA16(1u, 1u))
        testGrayscaleAlphaPreserved(DynamicImage.new(1u, 1u, ColorType.La16))
    }

    @Test
    fun testGrayscaleRgb16() {
        testGrayscaleAlphaDiscarded(DynamicImage.newRgb16(1u, 1u))
        testGrayscaleAlphaDiscarded(DynamicImage.new(1u, 1u, ColorType.Rgb16))
    }

    @Test
    fun testGrayscaleRgba16() {
        testGrayscaleAlphaPreserved(DynamicImage.newRgba16(1u, 1u))
        testGrayscaleAlphaPreserved(DynamicImage.new(1u, 1u, ColorType.Rgba16))
    }

    @Test
    fun testGrayscaleRgb32F() {
        testGrayscaleAlphaDiscarded(DynamicImage.newRgb32F(1u, 1u))
        testGrayscaleAlphaDiscarded(DynamicImage.new(1u, 1u, ColorType.Rgb32F))
    }

    @Test
    fun testGrayscaleRgba32F() {
        testGrayscaleAlphaPreserved(DynamicImage.newRgba32F(1u, 1u))
        testGrayscaleAlphaPreserved(DynamicImage.new(1u, 1u, ColorType.Rgba32F))
    }

    @Test
    fun testToVecU8() {
        val b1 = DynamicImage.newLuma8(1u, 1u).intoBytes()
        assertEquals(1, b1.size)
        val b2 = DynamicImage.newLuma16(1u, 1u).intoBytes()
        assertEquals(2, b2.size)
    }

    @Test
    fun issue1705CanTurn16bitImageIntoBytes() {
        val bytesIn = ByteArray(64 * 64 * 2) { (-1).toByte() }
        val img = DynamicImage.ImageLuma16(createGray16(64u, 64u, bytesIn))
        assertNotNull(img.asLuma16())

        val bytes = img.intoBytes()
        assertEquals(64 * 64 * 2, bytes.size)
        for (b in bytes) {
            assertEquals((-1).toByte(), b)
        }
    }

    @Test
    fun testConvertTo() {
        val imageLuma8 = DynamicImage.newLuma8(1u, 1u)
        val imageLuma16 = DynamicImage.newLuma16(1u, 1u)
        assertEquals(imageLuma8.toLuma16().asRaw().size, imageLuma16.toLuma16().asRaw().size)
    }

    @Test
    fun testInvert() {
        val img = DynamicImage.newRgba8(2u, 2u)
        img.putPixel(0u, 0u, Rgba(10u, 20u, 30u, 255u))
        img.invert()
        val p = img.getPixel(0u, 0u)
        assertEquals(245u, p.r)
        assertEquals(235u, p.g)
        assertEquals(225u, p.b)
        assertEquals(255u, p.a)
    }

    @Test
    fun testRotateAndFlip() {
        val img = DynamicImage.newRgb8(3u, 2u)
        img.putPixel(0u, 0u, Rgba(255u, 0u, 0u, 255u))
        val r90 = img.rotate90()
        assertEquals(2u, r90.width())
        assertEquals(3u, r90.height())

        val r180 = img.rotate180()
        assertEquals(3u, r180.width())
        assertEquals(2u, r180.height())

        val r270 = img.rotate270()
        assertEquals(2u, r270.width())
        assertEquals(3u, r270.height())

        val fliph = img.fliph()
        assertEquals(3u, fliph.width())
        assertEquals(2u, fliph.height())

        val flipv = img.flipv()
        assertEquals(3u, flipv.width())
        assertEquals(2u, flipv.height())
    }

    @Test
    fun testApplyOrientation() {
        val img = DynamicImage.newRgb8(4u, 2u)
        val noTransform = img.applyOrientation(Orientation.NoTransforms)
        assertEquals(4u, noTransform.width())
        assertEquals(2u, noTransform.height())

        val rot90 = img.applyOrientation(Orientation.Rotate90)
        assertEquals(2u, rot90.width())
        assertEquals(4u, rot90.height())
    }

    @Test
    fun testResizeAndThumbnail() {
        val img = DynamicImage.newRgb8(10u, 10u)
        val resized = img.resize(5u, 5u, FilterType.Nearest)
        assertEquals(5u, resized.width())
        assertEquals(5u, resized.height())

        val thumb = img.thumbnail(4u, 4u)
        assertEquals(4u, thumb.width())
        assertEquals(4u, thumb.height())

        val filled = img.resizeToFill(8u, 4u, FilterType.Nearest)
        assertEquals(8u, filled.width())
        assertEquals(4u, filled.height())
    }

    @Test
    fun testBlurAndFilter() {
        val img = DynamicImage.newRgb8(5u, 5u)
        val blurred = img.blur(1.0f)
        assertEquals(5u, blurred.width())
        assertEquals(5u, blurred.height())

        val fastBlurred = img.fastBlur(1.0f)
        assertEquals(5u, fastBlurred.width())
        assertEquals(5u, fastBlurred.height())

        val unsharp = img.unsharpen(1.0f, 2)
        assertEquals(5u, unsharp.width())
        assertEquals(5u, unsharp.height())

        val kernel =
            floatArrayOf(
                0.0f,
                -1.0f,
                0.0f,
                -1.0f,
                5.0f,
                -1.0f,
                0.0f,
                -1.0f,
                0.0f,
            )
        val filtered = img.filter3x3(kernel)
        assertEquals(5u, filtered.width())
        assertEquals(5u, filtered.height())
    }

    @Test
    fun testAdjustments() {
        val img = DynamicImage.newRgb8(2u, 2u)
        val contrasted = img.adjustContrast(0.5f)
        assertEquals(2u, contrasted.width())

        val brightened = img.brighten(10)
        assertEquals(2u, brightened.width())

        val hueRotated = img.huerotate(90)
        assertEquals(2u, hueRotated.width())
    }

    @Test
    fun testCrop() {
        val img = DynamicImage.newRgb8(10u, 10u)
        val cropped = img.crop(2u, 3u, 4u, 5u)
        assertEquals(4u, cropped.width())
        assertEquals(5u, cropped.height())
    }

    @Test
    fun testAsMutAccessors() {
        val rgb8 = DynamicImage.newRgb8(2u, 2u)
        assertNotNull(rgb8.asMutRgb8())
        val rgba8 = DynamicImage.newRgba8(2u, 2u)
        assertNotNull(rgba8.asMutRgba8())
        val luma8 = DynamicImage.newLuma8(2u, 2u)
        assertNotNull(luma8.asMutLuma8())
        val lumaA8 = DynamicImage.newLumaA8(2u, 2u)
        assertNotNull(lumaA8.asMutLumaAlpha8())
        val rgb16 = DynamicImage.newRgb16(2u, 2u)
        assertNotNull(rgb16.asMutRgb16())
        val rgba16 = DynamicImage.newRgba16(2u, 2u)
        assertNotNull(rgba16.asMutRgba16())
        val luma16 = DynamicImage.newLuma16(2u, 2u)
        assertNotNull(luma16.asMutLuma16())
        val lumaA16 = DynamicImage.newLumaA16(2u, 2u)
        assertNotNull(lumaA16.asMutLumaAlpha16())
        val rgb32F = DynamicImage.newRgb32F(2u, 2u)
        assertNotNull(rgb32F.asMutRgb32F())
        val rgba32F = DynamicImage.newRgba32F(2u, 2u)
        assertNotNull(rgba32F.asMutRgba32F())
    }

    @Test
    fun testFlatSamplesAccessors() {
        val rgb8 = DynamicImage.newRgb8(2u, 2u)
        assertNotNull(rgb8.asFlatSamplesU8())
        val rgb16 = DynamicImage.newRgb16(2u, 2u)
        assertNotNull(rgb16.asFlatSamplesU16())
        val rgb32F = DynamicImage.newRgb32F(2u, 2u)
        assertNotNull(rgb32F.asFlatSamplesF32())
    }

    @Test
    fun testToLuma32F() {
        val img = DynamicImage.newRgb8(2u, 2u)
        img.putPixel(0u, 0u, Rgba(255u, 255u, 255u, 255u))
        val luma32 = img.toLuma32F()
        assertEquals(2u, luma32.width())
        assertEquals(2u, luma32.height())
        val p = luma32.getPixel(0u, 0u)
        assertEquals(1.0f, p.l, 0.01f)

        val lumaA32 = img.toLumaAlpha32F()
        assertEquals(2u, lumaA32.width())
        assertEquals(2u, lumaA32.height())
        val pa = lumaA32.getPixel(0u, 0u)
        assertEquals(1.0f, pa.l, 0.01f)
        assertEquals(1.0f, pa.a, 0.01f)
    }

    @Test
    fun testInPlaceAffineOperations() {
        val img = DynamicImage.newRgb8(2u, 2u)
        img.putPixel(0u, 0u, Rgba(255u, 0u, 0u, 255u))
        img.fliphInPlace()
        val pFliph = img.getPixel(1u, 0u)
        assertEquals(255u, pFliph.r)

        img.flipvInPlace()
        val pFlipv = img.getPixel(1u, 1u)
        assertEquals(255u, pFlipv.r)

        img.rotate180InPlace()
        val pRot = img.getPixel(0u, 0u)
        assertEquals(255u, pRot.r)
    }

    @Test
    fun testDynamicImageDefaultImplementation() {
        val defaultImg = DynamicImage.newLuma8(0u, 0u)
        assertEquals(0u, defaultImg.width())
        assertEquals(0u, defaultImg.height())
    }

    @Test
    fun colorConversionSrgbP3() {
        val source =
            DynamicImage.ImageRgb8(
                ImageBuffer.createRgb(128u, 128u) { _, _ -> Rgb(255u.toUByte(), 0u.toUByte(), 0u.toUByte()) },
            )
        val target =
            DynamicImage.ImageRgba8(
                ImageBuffer.createRgba(128u, 128u) { _, _ -> Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()) },
            )

        source.setRgbPrimaries(Cicp.SRGB.primaries)
        source.setTransferFunction(Cicp.SRGB.transfer)
        target.setRgbPrimaries(Cicp.DISPLAY_P3.primaries)
        target.setTransferFunction(Cicp.DISPLAY_P3.transfer)

        target.copyFromColorSpace(source, ConvertColorOptions())

        val targetBuf = target.asRgba8()
        assertNotNull(targetBuf)
        assertEquals(Rgba(234u.toUByte(), 51u.toUByte(), 35u.toUByte(), 255u.toUByte()), targetBuf.getPixel(0u, 0u))
    }

    @Test
    fun colorConversionPreservesSample() {
        val sourceBuf = createRgb16(128u, 128u)
        for (y in 0u until 128u) {
            for (x in 0u until 128u) {
                sourceBuf.putPixel(x, y, Rgb(UShort.MAX_VALUE, 0u.toUShort(), 0u.toUShort()))
            }
        }
        val source = DynamicImage.ImageRgb16(sourceBuf)

        val target =
            DynamicImage.ImageRgba8(
                ImageBuffer.createRgba(128u, 128u) { _, _ -> Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()) },
            )

        source.setRgbPrimaries(Cicp.SRGB.primaries)
        source.setTransferFunction(Cicp.SRGB.transfer)
        target.setRgbPrimaries(Cicp.DISPLAY_P3.primaries)
        target.setTransferFunction(Cicp.DISPLAY_P3.transfer)

        target.copyFromColorSpace(source, ConvertColorOptions())

        val targetBuf = target.asRgba8()
        assertNotNull(targetBuf)
        assertEquals(Rgba(234u.toUByte(), 51u.toUByte(), 35u.toUByte(), 255u.toUByte()), targetBuf.getPixel(0u, 0u))
    }

    @Test
    fun colorConversionPreservesSampleInFastpath() {
        val sourceBuf = createRgb16(128u, 128u)
        for (y in 0u until 128u) {
            for (x in 0u until 128u) {
                sourceBuf.putPixel(x, y, Rgb(UShort.MAX_VALUE, 0u.toUShort(), 0u.toUShort()))
            }
        }
        val source = DynamicImage.ImageRgb16(sourceBuf)

        val target =
            DynamicImage.ImageRgba8(
                ImageBuffer.createRgba(128u, 128u) { _, _ -> Rgba(0u.toUByte(), 0u.toUByte(), 0u.toUByte(), 0u.toUByte()) },
            )

        target.copyFromColorSpace(source, ConvertColorOptions())

        val targetBuf = target.asRgba8()
        assertNotNull(targetBuf)
        assertEquals(Rgba(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte()), targetBuf.getPixel(0u, 0u))
    }

    @Test
    fun colorConversionRgbToLuma() {
        val sourceBuf = createRgb16(128u, 128u)
        for (y in 0u until 128u) {
            for (x in 0u until 128u) {
                sourceBuf.putPixel(x, y, Rgb(0u.toUShort(), UShort.MAX_VALUE, 0u.toUShort()))
            }
        }
        val source = DynamicImage.ImageRgb16(sourceBuf)

        val target =
            DynamicImage.ImageLuma8(
                ImageBuffer.createGray(128u, 128u) { _, _ -> Luma(0u.toUByte()) },
            )

        target.copyFromColorSpace(source, ConvertColorOptions())
        assertNotNull(target.asLuma8())
    }

    @Test
    fun copyColorSpaceCoverage() {
        val types =
            listOf(
                ColorType.L8,
                ColorType.La8,
                ColorType.Rgb8,
                ColorType.Rgba8,
                ColorType.L16,
                ColorType.La16,
                ColorType.Rgb16,
                ColorType.Rgba16,
                ColorType.Rgb32F,
                ColorType.Rgba32F,
            )

        val transform = CicpTransform.new(Cicp.SRGB, Cicp.DISPLAY_P3)
        assertNotNull(transform)

        for (from in types) {
            for (to in types) {
                val source = DynamicImage.new(16u, 16u, from)
                val target = DynamicImage.new(16u, 16u, to)

                source.setRgbPrimaries(Cicp.SRGB.primaries)
                source.setTransferFunction(Cicp.SRGB.transfer)

                target.setRgbPrimaries(Cicp.DISPLAY_P3.primaries)
                target.setTransferFunction(Cicp.DISPLAY_P3.transfer)

                target.copyFromColorSpace(
                    source,
                    ConvertColorOptions(transform = transform),
                )
            }
        }
    }

    @Test
    fun applyColorSpace() {
        val buffer =
            DynamicImage.ImageRgb8(
                ImageBuffer.createRgb(128u, 128u) { _, _ -> Rgb(UByte.MAX_VALUE, 0u.toUByte(), 0u.toUByte()) },
            )

        buffer.setRgbPrimaries(Cicp.SRGB.primaries)
        buffer.setTransferFunction(Cicp.SRGB.transfer)

        buffer.applyColorSpace(Cicp.DISPLAY_P3, ConvertColorOptions())

        val target = buffer.asRgb8()
        assertNotNull(target)
        assertEquals(Rgb(234u.toUByte(), 51u.toUByte(), 35u.toUByte()), target.getPixel(0u, 0u))
    }

    @Test
    fun applyColorSpaceCoverage() {
        val types =
            listOf(
                ColorType.L8,
                ColorType.La8,
                ColorType.Rgb8,
                ColorType.Rgba8,
                ColorType.L16,
                ColorType.La16,
                ColorType.Rgb16,
                ColorType.Rgba16,
                ColorType.Rgb32F,
                ColorType.Rgba32F,
            )

        val transform = CicpTransform.new(Cicp.SRGB, Cicp.DISPLAY_P3)
        assertNotNull(transform)

        for (colorType in types) {
            val buffer = DynamicImage.new(16u, 16u, colorType)

            buffer.setRgbPrimaries(Cicp.SRGB.primaries)
            buffer.setTransferFunction(Cicp.SRGB.transfer)

            buffer.applyColorSpace(
                Cicp.DISPLAY_P3,
                ConvertColorOptions(transform = transform),
            )
        }
    }

    @Test
    fun convertColorSpace() {
        val sourceBuf = createRgb16(128u, 128u)
        for (y in 0u until 128u) {
            for (x in 0u until 128u) {
                sourceBuf.putPixel(x, y, Rgb(UShort.MAX_VALUE, 0u.toUShort(), 0u.toUShort()))
            }
        }
        val buffer = DynamicImage.ImageRgb16(sourceBuf)

        buffer.setRgbPrimaries(Cicp.SRGB.primaries)
        buffer.setTransferFunction(Cicp.SRGB.transfer)

        val converted = buffer.convertColorSpace(Cicp.DISPLAY_P3, ColorType.Rgb8, ConvertColorOptions())

        val target = converted.asRgb8()
        assertNotNull(target)
        assertEquals(Rgb(234u.toUByte(), 51u.toUByte(), 35u.toUByte()), target.getPixel(0u, 0u))
    }

    @Test
    fun intoLumaIsColorSpaceAware() {
        val sourceBuf = createRgb16(128u, 128u)
        for (y in 0u until 128u) {
            for (x in 0u until 128u) {
                sourceBuf.putPixel(x, y, Rgb(UShort.MAX_VALUE, 0u.toUShort(), 0u.toUShort()))
            }
        }
        val buffer = DynamicImage.ImageRgb16(sourceBuf)

        buffer.setRgbPrimaries(Cicp.DISPLAY_P3.primaries)
        buffer.setTransferFunction(Cicp.DISPLAY_P3.transfer)

        val luma8 = buffer.clone().intoLuma8()
        assertEquals(Luma(58u.toUByte()), luma8.getPixel(0u, 0u))
        assertEquals(Cicp.DISPLAY_P3, luma8.colorSpace())

        buffer.setRgbPrimaries(Cicp.SRGB.primaries)

        val luma8Srgb = buffer.clone().intoLuma8()
        assertEquals(Luma(54u.toUByte()), luma8Srgb.getPixel(0u, 0u))
        assertNotEquals(Cicp.DISPLAY_P3, luma8Srgb.colorSpace())
    }

    @Test
    fun fromLumaIsColorSpaceAware() {
        val sourceBuf = createGray16(128u, 128u)
        for (y in 0u until 128u) {
            for (x in 0u until 128u) {
                sourceBuf.putPixel(x, y, Luma(UShort.MAX_VALUE))
            }
        }
        val buffer = DynamicImage.ImageLuma16(sourceBuf)

        buffer.setRgbPrimaries(Cicp.DISPLAY_P3.primaries)
        buffer.setTransferFunction(Cicp.DISPLAY_P3.transfer)

        val rgb8 = buffer.clone().intoRgb8()
        assertEquals(Rgb(UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE), rgb8.getPixel(0u, 0u))
        assertEquals(Cicp.DISPLAY_P3, rgb8.colorSpace())

        buffer.setRgbPrimaries(Cicp.SRGB.primaries)

        val rgb8Srgb = buffer.clone().intoRgb8()
        assertEquals(Rgb(UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE), rgb8Srgb.getPixel(0u, 0u))
        assertNotEquals(Cicp.DISPLAY_P3, rgb8Srgb.colorSpace())
    }

    @Test
    fun fromLumaForAllChromaticities() {
        val chroma =
            listOf(
                CicpColorPrimaries.SRgb,
                CicpColorPrimaries.RgbM,
                CicpColorPrimaries.RgbB,
                CicpColorPrimaries.Bt601,
                CicpColorPrimaries.Rgb240m,
                CicpColorPrimaries.GenericFilm,
                CicpColorPrimaries.Rgb2020,
                CicpColorPrimaries.Xyz,
                CicpColorPrimaries.SmpteRp431,
                CicpColorPrimaries.SmpteRp432,
                CicpColorPrimaries.Industry22,
                CicpColorPrimaries.Unspecified,
            )

        val sourceBuf = createGray16(128u, 128u)
        for (y in 0u until 128u) {
            for (x in 0u until 128u) {
                sourceBuf.putPixel(x, y, Luma(UShort.MAX_VALUE))
            }
        }
        val buffer = DynamicImage.ImageLuma16(sourceBuf)

        for (c in chroma) {
            buffer.setRgbPrimaries(c)
            val rgb = buffer.toRgb8()
            assertEquals(
                Rgb(UByte.MAX_VALUE, UByte.MAX_VALUE, UByte.MAX_VALUE),
                rgb.getPixel(0u, 0u),
                "Failed for chroma: $c",
            )
        }
    }

    private data class ChromaCase(
        val chroma: CicpColorPrimaries,
        val r: UByte,
        val g: UByte,
        val b: UByte,
        val expectedLuma: UByte,
    )

    @Test
    fun fromRgbForAllChromaticities() {
        val cases =
            listOf(
                ChromaCase(CicpColorPrimaries.SRgb, 54u, 182u, 18u, 143u),
                ChromaCase(CicpColorPrimaries.RgbM, 76u, 150u, 29u, 114u),
                ChromaCase(CicpColorPrimaries.RgbB, 57u, 180u, 18u, 141u),
                ChromaCase(CicpColorPrimaries.Bt601, 54u, 179u, 22u, 139u),
                ChromaCase(CicpColorPrimaries.Rgb240m, 54u, 179u, 22u, 139u),
                ChromaCase(CicpColorPrimaries.GenericFilm, 65u, 173u, 17u, 135u),
                ChromaCase(CicpColorPrimaries.Rgb2020, 67u, 173u, 15u, 136u),
                ChromaCase(CicpColorPrimaries.Xyz, 0u, 255u, 0u, 255u),
                ChromaCase(CicpColorPrimaries.SmpteRp431, 53u, 184u, 18u, 145u),
                ChromaCase(CicpColorPrimaries.SmpteRp432, 58u, 176u, 20u, 137u),
                ChromaCase(CicpColorPrimaries.Industry22, 59u, 171u, 24u, 131u),
                ChromaCase(CicpColorPrimaries.Unspecified, 54u, 182u, 18u, 143u),
            )

        for (case in cases) {
            val sourceBuf = ImageBuffer.createRgb(128u, 128u) { _, _ -> Rgb(case.r, case.g, case.b) }
            val buffer = DynamicImage.ImageRgb8(sourceBuf)
            buffer.setRgbPrimaries(case.chroma)

            val buf = buffer.toLuma8()
            assertEquals(Luma(case.expectedLuma), buf.getPixel(0u, 0u), "Failed for chroma: ${case.chroma}")
        }
    }

    @Test
    fun convertColorSpaceCoverage() {
        val types =
            listOf(
                ColorType.L8,
                ColorType.La8,
                ColorType.Rgb8,
                ColorType.Rgba8,
                ColorType.L16,
                ColorType.La16,
                ColorType.Rgb16,
                ColorType.Rgba16,
                ColorType.Rgb32F,
                ColorType.Rgba32F,
            )

        val transform = CicpTransform.new(Cicp.SRGB, Cicp.DISPLAY_P3)
        assertNotNull(transform)

        for (from in types) {
            for (to in types) {
                val buffer = DynamicImage.new(16u, 16u, from)

                buffer.setRgbPrimaries(Cicp.SRGB.primaries)
                buffer.setTransferFunction(Cicp.SRGB.transfer)

                val converted =
                    buffer.convertColorSpace(
                        Cicp.DISPLAY_P3,
                        to,
                        ConvertColorOptions(transform = transform),
                    )
                assertEquals(to, converted.color())
            }
        }
    }
}
