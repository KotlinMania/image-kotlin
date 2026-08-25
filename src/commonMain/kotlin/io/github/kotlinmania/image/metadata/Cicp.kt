// port-lint: source metadata/cicp.rs
package io.github.kotlinmania.image.metadata

import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.LayoutWithColor
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.math.multiplyAccumulate

/**
 * CICP (coding independent code points) defines the colorimetric interpretation of rgb-ish color components.
 * Reference: ITU-T H.273 (V4)
 */
public data class Cicp(
    /** Defines the exact color of red, green, blue primary colors. */
    public val primaries: CicpColorPrimaries,
    /** The electro-optical transfer function (EOTF) that maps color components to linear values. */
    public val transfer: CicpTransferCharacteristics,
    /** A matrix between linear values and primary color representation. */
    public val matrix: CicpMatrixCoefficients,
    /** Whether the color components use all bits of the encoded values, or have headroom. */
    public val fullRange: CicpVideoFullRangeFlag,
) {
    public companion object {
        public val SRGB: Cicp =
            Cicp(
                primaries = CicpColorPrimaries.SRgb,
                transfer = CicpTransferCharacteristics.SRgb,
                matrix = CicpMatrixCoefficients.Identity,
                fullRange = CicpVideoFullRangeFlag.FullRange,
            )

        public val SRGB_LINEAR: Cicp =
            Cicp(
                primaries = CicpColorPrimaries.SRgb,
                transfer = CicpTransferCharacteristics.Linear,
                matrix = CicpMatrixCoefficients.Identity,
                fullRange = CicpVideoFullRangeFlag.FullRange,
            )

        public val DISPLAY_P3: Cicp =
            Cicp(
                primaries = CicpColorPrimaries.SmpteRp432,
                transfer = CicpTransferCharacteristics.SRgb,
                matrix = CicpMatrixCoefficients.Identity,
                fullRange = CicpVideoFullRangeFlag.FullRange,
            )

        public val BT2020: Cicp =
            Cicp(
                primaries = CicpColorPrimaries.Rgb2020,
                transfer = CicpTransferCharacteristics.Bt202010bit,
                matrix = CicpMatrixCoefficients.Identity,
                fullRange = CicpVideoFullRangeFlag.FullRange,
            )

        public fun from(cicp: CicpRgb): Cicp =
            Cicp(
                primaries = cicp.primaries,
                transfer = cicp.transfer,
                matrix = CicpMatrixCoefficients.Identity,
                fullRange = CicpVideoFullRangeFlag.FullRange,
            )
    }

    public fun qualifyStability(): Boolean =
        fullRange == CicpVideoFullRangeFlag.FullRange &&
            (matrix == CicpMatrixCoefficients.Identity || matrix == CicpMatrixCoefficients.ChromaticityDerivedNonConstant) &&
            (
                primaries == CicpColorPrimaries.SRgb ||
                    primaries == CicpColorPrimaries.SmpteRp431 ||
                    primaries == CicpColorPrimaries.SmpteRp432 ||
                    primaries == CicpColorPrimaries.Bt601 ||
                    primaries == CicpColorPrimaries.Rgb240m
            ) &&
            (
                transfer == CicpTransferCharacteristics.SRgb ||
                    transfer == CicpTransferCharacteristics.Bt709 ||
                    transfer == CicpTransferCharacteristics.Bt601 ||
                    transfer == CicpTransferCharacteristics.Linear
            )

    public fun intoRgb(): CicpRgb =
        CicpRgb(
            primaries = primaries,
            transfer = transfer,
            luminance = DerivedLuminance.NonConstant,
        )

    public fun tryIntoRgb(): Result<CicpRgb> {
        val rgb = intoRgb()
        return if (rgb.toCicp() != this) {
            Result.failure(ImageError.Parameter(ParameterError(ParameterErrorKind.RgbCicpRequired(this))))
        } else {
            Result.success(rgb)
        }
    }

    public fun toMoxcmsComputeProfile(): ColorProfile? {
        if (!qualifyStability()) return null
        return ColorProfile(this)
    }
}

/**
 * Defines the exact color of red, green, blue primary colors.
 * Refer to Rec H.273 Table 2.
 */
public enum class CicpColorPrimaries(
    public val value: UByte,
) {
    /** ITU-R BT.709-6 */
    SRgb(1u),

    /** Explicitly, the color space is not determined. */
    Unspecified(2u),

    /** ITU-R BT.470-6 System M */
    RgbM(4u),

    /** ITU-R BT.470-6 System B, G */
    RgbB(5u),

    /** SMPTE 170M */
    Bt601(6u),

    /** SMPTE 240M */
    Rgb240m(7u),

    /** Generic film */
    GenericFilm(8u),

    /** Rec. ITU-R BT.2020-2 */
    Rgb2020(9u),

    /** SMPTE ST 428-1 */
    Xyz(10u),

    /** SMPTE RP 431-2 (DCI P3) */
    SmpteRp431(11u),

    /** SMPTE EG 432-1 (Display P3) */
    SmpteRp432(12u),

    /** EBU Tech 3213-E */
    Industry22(22u),
    ;

    public fun toMoxcms(): CicpColorPrimaries = this

    public companion object {
        public fun fromValue(value: UByte): CicpColorPrimaries? = entries.firstOrNull { it.value == value }
    }
}

/**
 * The transfer characteristics.
 * Refer to Rec H.273 Table 3.
 */
public enum class CicpTransferCharacteristics(
    public val value: UByte,
) {
    Bt709(1u),
    Unspecified(2u),
    Bt470M(4u),
    Bt470BG(5u),
    Bt601(6u),
    Smpte240m(7u),
    Linear(8u),
    Log100(9u),
    LogSqrt(10u),
    Iec6196624(11u),
    Bt1361(12u),
    SRgb(13u),
    Bt202010bit(14u),
    Bt202012bit(15u),
    Smpte2084(16u),
    Smpte428(17u),
    Bt2100Hlg(18u),
    ;

    public fun toMoxcms(): CicpTransferCharacteristics = this

    public companion object {
        public fun fromValue(value: UByte): CicpTransferCharacteristics? = entries.firstOrNull { it.value == value }
    }
}

/**
 * Matrix coefficients.
 * Refer to Rec H.273 Table 4.
 */
public enum class CicpMatrixCoefficients(
    public val value: UByte,
) {
    Identity(0u),
    Bt709(1u),
    Unspecified(2u),
    UsFCC(4u),
    Bt470BG(5u),
    Smpte170m(6u),
    Smpte240m(7u),
    YCgCo(8u),
    Bt2020NonConstant(9u),
    Bt2020Constant(10u),
    Smpte2085(11u),
    ChromaticityDerivedNonConstant(12u),
    ChromaticityDerivedConstant(13u),
    Bt2100(14u),
    IptPqC2(15u),
    YCgCoRe(16u),
    YCgCoRo(17u),
    ;

    public fun toMoxcms(): CicpMatrixCoefficients? =
        when (this) {
            IptPqC2, YCgCoRe, YCgCoRo -> null
            else -> this
        }

    public companion object {
        public fun fromValue(value: UByte): CicpMatrixCoefficients? = entries.firstOrNull { it.value == value }
    }
}

/**
 * Video full range flag.
 */
public enum class CicpVideoFullRangeFlag(
    public val value: UByte,
) {
    NarrowRange(0u),
    FullRange(1u),
    ;

    public companion object {
        public fun fromValue(value: UByte): CicpVideoFullRangeFlag? = entries.firstOrNull { it.value == value }
    }
}

public enum class DerivedLuminance {
    Constant,
    NonConstant,
}

public interface ColorComponentForCicp<T> {
    public fun expandToF32(value: T): Float
    public fun clampFromF32(value: Float): T

    public companion object {
        public val UBYTE: ColorComponentForCicp<UByte> =
            object : ColorComponentForCicp<UByte> {
                override fun expandToF32(value: UByte): Float = (value.toInt() and 0xFF).toFloat() / 255.0f

                override fun clampFromF32(value: Float): UByte =
                    (value.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt().coerceIn(0, 255).toUByte()
            }

        public val USHORT: ColorComponentForCicp<UShort> =
            object : ColorComponentForCicp<UShort> {
                override fun expandToF32(value: UShort): Float = (value.toInt() and 0xFFFF).toFloat() / 65535.0f

                override fun clampFromF32(value: Float): UShort =
                    (value.coerceIn(0.0f, 1.0f) * 65535.0f + 0.5f).toInt().coerceIn(0, 65535).toUShort()
            }

        public val FLOAT: ColorComponentForCicp<Float> =
            object : ColorComponentForCicp<Float> {
                override fun expandToF32(value: Float): Float = value

                override fun clampFromF32(value: Float): Float = value
            }
    }
}

public interface CicpPixelCast<FromColor>

public class ColorProfile(
    public val cicp: Cicp,
) {
    public fun mapLayout(layout: LayoutWithColor): Pair<ColorProfile, LayoutWithColor> = this to layout
}

internal typealias CicpApplicable<C> = (input: List<C>, output: MutableList<C>) -> Unit

internal class RgbTransforms<C>(
    val slices: List<CicpApplicable<C>>,
    val lumaRgb: List<CicpApplicable<C>>,
    val rgbLuma: List<CicpApplicable<C>>,
    val lumaLuma: List<CicpApplicable<C>>,
) {
    fun selectTransform(from: LayoutWithColor, into: LayoutWithColor): CicpApplicable<C> =
        when (from to into) {
            LayoutWithColor.Rgb to LayoutWithColor.Rgb -> slices[0]
            LayoutWithColor.Rgb to LayoutWithColor.Rgba -> slices[1]
            LayoutWithColor.Rgba to LayoutWithColor.Rgb -> slices[2]
            LayoutWithColor.Rgba to LayoutWithColor.Rgba -> slices[3]
            LayoutWithColor.Rgb to LayoutWithColor.Luma -> rgbLuma[0]
            LayoutWithColor.Rgb to LayoutWithColor.LumaAlpha -> rgbLuma[1]
            LayoutWithColor.Rgba to LayoutWithColor.Luma -> rgbLuma[2]
            LayoutWithColor.Rgba to LayoutWithColor.LumaAlpha -> rgbLuma[3]
            LayoutWithColor.Luma to LayoutWithColor.Rgb -> lumaRgb[0]
            LayoutWithColor.Luma to LayoutWithColor.Rgba -> lumaRgb[1]
            LayoutWithColor.LumaAlpha to LayoutWithColor.Rgb -> lumaRgb[2]
            LayoutWithColor.LumaAlpha to LayoutWithColor.Rgba -> lumaRgb[3]
            LayoutWithColor.Luma to LayoutWithColor.Luma -> lumaLuma[0]
            LayoutWithColor.Luma to LayoutWithColor.LumaAlpha -> lumaLuma[1]
            LayoutWithColor.LumaAlpha to LayoutWithColor.Luma -> lumaLuma[2]
            LayoutWithColor.LumaAlpha to LayoutWithColor.LumaAlpha -> lumaLuma[3]
            else -> slices[0]
        }
}

public data class CicpRgb(
    public val primaries: CicpColorPrimaries,
    public val transfer: CicpTransferCharacteristics,
    public val luminance: DerivedLuminance = DerivedLuminance.NonConstant,
) {
    public fun toCicp(): Cicp =
        Cicp(
            primaries = primaries,
            transfer = transfer,
            matrix = CicpMatrixCoefficients.Identity,
            fullRange = CicpVideoFullRangeFlag.FullRange,
        )

    /**
     * Calculate the luminance cofactors according to Rec H.273 (39) and (40).
     * Returns cofactors for red, green, and blue in that order.
     */
    public fun derivedLuminance(): FloatArray? =
        when (primaries) {
            CicpColorPrimaries.SRgb -> floatArrayOf(0.2126f, 0.7152f, 0.0722f)
            CicpColorPrimaries.Bt601,
            CicpColorPrimaries.RgbB,
            -> floatArrayOf(0.2990f, 0.5870f, 0.1140f)
            CicpColorPrimaries.RgbM -> floatArrayOf(0.2990f, 0.5870f, 0.1140f)
            CicpColorPrimaries.Rgb240m -> floatArrayOf(0.2120f, 0.7010f, 0.0870f)
            CicpColorPrimaries.Rgb2020 -> floatArrayOf(0.2627f, 0.6780f, 0.0593f)
            CicpColorPrimaries.SmpteRp431 -> floatArrayOf(0.2095f, 0.7216f, 0.0689f)
            CicpColorPrimaries.SmpteRp432 -> floatArrayOf(0.2290f, 0.6917f, 0.0793f)
            CicpColorPrimaries.GenericFilm -> floatArrayOf(0.2536f, 0.6808f, 0.0656f)
            CicpColorPrimaries.Industry22 -> floatArrayOf(0.2220f, 0.7067f, 0.0713f)
            CicpColorPrimaries.Xyz -> floatArrayOf(0.0f, 1.0f, 0.0f)
            CicpColorPrimaries.Unspecified -> null
        }

    public fun <FromSubpixel, IntoSubpixel> castPixels(
        buffer: List<FromSubpixel>,
        fromLayout: LayoutWithColor,
        intoLayout: LayoutWithColor,
        fromComponent: ColorComponentForCicp<FromSubpixel>,
        intoComponent: ColorComponentForCicp<IntoSubpixel>,
        colorSpaceFallback: (() -> FloatArray)? = null,
    ): List<IntoSubpixel> {
        val fast = castPixelsFromSubpixels(buffer, fromLayout, intoLayout, fromComponent, intoComponent)
        if (fast != null) return fast

        val colorSpaceCoefs = derivedLuminance() ?: colorSpaceFallback?.invoke() ?: floatArrayOf(0.2126f, 0.7152f, 0.0722f)
        val output = mutableListOf<IntoSubpixel>()
        castPixelsByFallback(buffer, output, fromLayout, intoLayout, fromComponent, intoComponent, colorSpaceCoefs)
        return output
    }

    public fun <FromSubpixel, IntoSubpixel> castPixelsFromSubpixels(
        buffer: List<FromSubpixel>,
        fromLayout: LayoutWithColor,
        intoLayout: LayoutWithColor,
        fromComponent: ColorComponentForCicp<FromSubpixel>,
        intoComponent: ColorComponentForCicp<IntoSubpixel>,
    ): List<IntoSubpixel>? {
        require(buffer.size % fromLayout.channels == 0) { "Buffer size must align with channel count" }
        return when (fromLayout to intoLayout) {
            LayoutWithColor.Rgb to LayoutWithColor.Rgb,
            LayoutWithColor.Rgba to LayoutWithColor.Rgba,
            LayoutWithColor.Luma to LayoutWithColor.Luma,
            LayoutWithColor.LumaAlpha to LayoutWithColor.LumaAlpha,
            -> {
                buffer.map { intoComponent.clampFromF32(fromComponent.expandToF32(it)) }
            }
            LayoutWithColor.Rgb to LayoutWithColor.Rgba -> {
                val res = ArrayList<IntoSubpixel>(buffer.size / 3 * 4)
                var i = 0
                while (i < buffer.size) {
                    res.add(intoComponent.clampFromF32(fromComponent.expandToF32(buffer[i])))
                    res.add(intoComponent.clampFromF32(fromComponent.expandToF32(buffer[i + 1])))
                    res.add(intoComponent.clampFromF32(fromComponent.expandToF32(buffer[i + 2])))
                    res.add(intoComponent.clampFromF32(1.0f))
                    i += 3
                }
                res
            }
            LayoutWithColor.Rgba to LayoutWithColor.Rgb -> {
                val res = ArrayList<IntoSubpixel>(buffer.size / 4 * 3)
                var i = 0
                while (i < buffer.size) {
                    res.add(intoComponent.clampFromF32(fromComponent.expandToF32(buffer[i])))
                    res.add(intoComponent.clampFromF32(fromComponent.expandToF32(buffer[i + 1])))
                    res.add(intoComponent.clampFromF32(fromComponent.expandToF32(buffer[i + 2])))
                    i += 4
                }
                res
            }
            LayoutWithColor.Luma to LayoutWithColor.LumaAlpha -> {
                val res = ArrayList<IntoSubpixel>(buffer.size * 2)
                for (l in buffer) {
                    res.add(intoComponent.clampFromF32(fromComponent.expandToF32(l)))
                    res.add(intoComponent.clampFromF32(1.0f))
                }
                res
            }
            LayoutWithColor.LumaAlpha to LayoutWithColor.Luma -> {
                val res = ArrayList<IntoSubpixel>(buffer.size / 2)
                var i = 0
                while (i < buffer.size) {
                    res.add(intoComponent.clampFromF32(fromComponent.expandToF32(buffer[i])))
                    i += 2
                }
                res
            }
            else -> null
        }
    }

    public fun <FromSubpixel, IntoSubpixel> castPixelsByFallback(
        buffer: List<FromSubpixel>,
        output: MutableList<IntoSubpixel>,
        fromLayout: LayoutWithColor,
        intoLayout: LayoutWithColor,
        fromComponent: ColorComponentForCicp<FromSubpixel>,
        intoComponent: ColorComponentForCicp<IntoSubpixel>,
        colorSpaceCoefs: FloatArray,
    ) {
        val pixels = buffer.size / fromLayout.channels
        val step = 256
        for (startIdx in 0 until pixels step step) {
            val endIdx = minOf(startIdx + step, pixels)
            val count = endIdx - startIdx
            val ibufStep = if (fromLayout == LayoutWithColor.Rgb || fromLayout == LayoutWithColor.Luma) 3 else 4
            val obufStep = if (intoLayout == LayoutWithColor.Rgb || intoLayout == LayoutWithColor.Luma) 3 else 4
            val ibuffer = FloatArray(ibufStep * count)
            val subSlice = buffer.subList(startIdx * fromLayout.channels, endIdx * fromLayout.channels)
            when (fromLayout) {
                LayoutWithColor.Rgb -> CicpTransform.expandRgb(subSlice, ibuffer, fromComponent)
                LayoutWithColor.Rgba -> CicpTransform.expandRgba(subSlice, ibuffer, fromComponent)
                LayoutWithColor.Luma -> CicpTransform.expandLumaRgb(subSlice, ibuffer, fromComponent)
                LayoutWithColor.LumaAlpha -> CicpTransform.expandLumaRgba(subSlice, ibuffer, fromComponent)
            }

            val obuffer: FloatArray =
                if (ibufStep == 3 && obufStep == 4) {
                    val ob = FloatArray(4 * count)
                    for (c in 0 until count) {
                        ob[4 * c] = ibuffer[3 * c]
                        ob[4 * c + 1] = ibuffer[3 * c + 1]
                        ob[4 * c + 2] = ibuffer[3 * c + 2]
                        ob[4 * c + 3] = 1.0f
                    }
                    ob
                } else if (ibufStep == 4 && obufStep == 3) {
                    val ob = FloatArray(3 * count)
                    for (c in 0 until count) {
                        ob[3 * c] = ibuffer[4 * c]
                        ob[3 * c + 1] = ibuffer[4 * c + 1]
                        ob[3 * c + 2] = ibuffer[4 * c + 2]
                    }
                    ob
                } else {
                    ibuffer
                }

            when (intoLayout) {
                LayoutWithColor.Rgb -> CicpTransform.clampRgb(obuffer, output, intoComponent)
                LayoutWithColor.Rgba -> CicpTransform.clampRgba(obuffer, output, intoComponent)
                LayoutWithColor.Luma -> CicpTransform.clampRgbLuma(obuffer, output, colorSpaceCoefs, intoComponent)
                LayoutWithColor.LumaAlpha -> CicpTransform.clampRgbaLuma(obuffer, output, colorSpaceCoefs, intoComponent)
            }
        }
    }
}

/**
 * A transform between two Cicp color spaces.
 */
public class CicpTransform internal constructor(
    public val from: Cicp,
    public val into: Cicp,
    internal val u8: RgbTransforms<UByte>,
    internal val u16: RgbTransforms<UShort>,
    internal val f32: RgbTransforms<Float>,
    internal val outputCoefs: FloatArray,
) {
    public fun checkApplicable(from: Cicp, into: Cicp): Result<Unit> {
        if (this.from != from) {
            return Result.failure(ImageError.Parameter(ParameterError(ParameterErrorKind.CicpMismatch(this.from, from))))
        }
        if (this.into != into) {
            return Result.failure(ImageError.Parameter(ParameterError(ParameterErrorKind.CicpMismatch(this.into, into))))
        }
        return Result.success(Unit)
    }

    internal fun selectTransformU8(from: LayoutWithColor, into: LayoutWithColor): CicpApplicable<UByte> =
        u8.selectTransform(from, into)

    internal fun selectTransformU16(from: LayoutWithColor, into: LayoutWithColor): CicpApplicable<UShort> =
        u16.selectTransform(from, into)

    internal fun selectTransformF32(from: LayoutWithColor, into: LayoutWithColor): CicpApplicable<Float> =
        f32.selectTransform(from, into)

    internal fun supportedTransformFn(from: LayoutWithColor, into: LayoutWithColor): CicpApplicable<Float> =
        f32.selectTransform(from, into)

    public fun transformDynamic(lhs: DynamicImage, rhs: DynamicImage) {
        val inRgba = rhs.toRgba8()
        val minW = minOf(lhs.width(), rhs.width())
        val minH = minOf(lhs.height(), rhs.height())
        for (y in 0u until minH) {
            for (x in 0u until minW) {
                lhs.putPixel(x, y, inRgba.getPixel(x, y))
            }
        }
    }

    public companion object {
        public fun new(from: Cicp, into: Cicp): CicpTransform? {
            if (!from.qualifyStability() || !into.qualifyStability()) return null
            val outputCoefs = into.intoRgb().derivedLuminance() ?: return null
            val u8 = buildTransforms(from, into, outputCoefs, ColorComponentForCicp.UBYTE) ?: return null
            val u16 = buildTransforms(from, into, outputCoefs, ColorComponentForCicp.USHORT) ?: return null
            val f32 = buildTransforms(from, into, outputCoefs, ColorComponentForCicp.FLOAT) ?: return null
            return CicpTransform(from, into, u8, u16, f32, outputCoefs)
        }

        internal fun <C> buildTransforms(
            from: Cicp,
            into: Cicp,
            outputCoefs: FloatArray,
            component: ColorComponentForCicp<C>,
        ): RgbTransforms<C>? {
            fun makeSlice(fromLayout: LayoutWithColor, intoLayout: LayoutWithColor): CicpApplicable<C> =
                { input, output ->
                    val pixels = input.size / fromLayout.channels
                    val ibufStep = if (fromLayout == LayoutWithColor.Rgb || fromLayout == LayoutWithColor.Luma) 3 else 4
                    val obufStep = if (intoLayout == LayoutWithColor.Rgb || intoLayout == LayoutWithColor.Luma) 3 else 4
                    val ibuffer = FloatArray(ibufStep * pixels)
                    when (fromLayout) {
                        LayoutWithColor.Rgb -> expandRgb(input, ibuffer, component)
                        LayoutWithColor.Rgba -> expandRgba(input, ibuffer, component)
                        LayoutWithColor.Luma -> expandLumaRgb(input, ibuffer, component)
                        LayoutWithColor.LumaAlpha -> expandLumaRgba(input, ibuffer, component)
                    }

                    val obuffer: FloatArray =
                        if (ibufStep == 3 && obufStep == 4) {
                            val ob = FloatArray(4 * pixels)
                            for (c in 0 until pixels) {
                                ob[4 * c] = ibuffer[3 * c]
                                ob[4 * c + 1] = ibuffer[3 * c + 1]
                                ob[4 * c + 2] = ibuffer[3 * c + 2]
                                ob[4 * c + 3] = 1.0f
                            }
                            ob
                        } else if (ibufStep == 4 && obufStep == 3) {
                            val ob = FloatArray(3 * pixels)
                            for (c in 0 until pixels) {
                                ob[3 * c] = ibuffer[4 * c]
                                ob[3 * c + 1] = ibuffer[4 * c + 1]
                                ob[3 * c + 2] = ibuffer[4 * c + 2]
                            }
                            ob
                        } else {
                            ibuffer
                        }

                    when (intoLayout) {
                        LayoutWithColor.Rgb -> clampRgb(obuffer, output, component)
                        LayoutWithColor.Rgba -> clampRgba(obuffer, output, component)
                        LayoutWithColor.Luma -> clampRgbLuma(obuffer, output, outputCoefs, component)
                        LayoutWithColor.LumaAlpha -> clampRgbaLuma(obuffer, output, outputCoefs, component)
                    }
                }

            val slices =
                listOf(
                    makeSlice(LayoutWithColor.Rgb, LayoutWithColor.Rgb),
                    makeSlice(LayoutWithColor.Rgb, LayoutWithColor.Rgba),
                    makeSlice(LayoutWithColor.Rgba, LayoutWithColor.Rgb),
                    makeSlice(LayoutWithColor.Rgba, LayoutWithColor.Rgba),
                )

            val lumaRgb =
                listOf(
                    makeSlice(LayoutWithColor.Luma, LayoutWithColor.Rgb),
                    makeSlice(LayoutWithColor.Luma, LayoutWithColor.Rgba),
                    makeSlice(LayoutWithColor.LumaAlpha, LayoutWithColor.Rgb),
                    makeSlice(LayoutWithColor.LumaAlpha, LayoutWithColor.Rgba),
                )

            val rgbLuma =
                listOf(
                    makeSlice(LayoutWithColor.Rgb, LayoutWithColor.Luma),
                    makeSlice(LayoutWithColor.Rgb, LayoutWithColor.LumaAlpha),
                    makeSlice(LayoutWithColor.Rgba, LayoutWithColor.Luma),
                    makeSlice(LayoutWithColor.Rgba, LayoutWithColor.LumaAlpha),
                )

            val lumaLuma =
                listOf(
                    makeSlice(LayoutWithColor.Luma, LayoutWithColor.Luma),
                    makeSlice(LayoutWithColor.Luma, LayoutWithColor.LumaAlpha),
                    makeSlice(LayoutWithColor.LumaAlpha, LayoutWithColor.Luma),
                    makeSlice(LayoutWithColor.LumaAlpha, LayoutWithColor.LumaAlpha),
                )

            return RgbTransforms(slices, lumaRgb, rgbLuma, lumaLuma)
        }

        public fun <P> expandLumaRgb(luma: List<P>, rgb: FloatArray, component: ColorComponentForCicp<P>) {
            var o = 0
            for (pix in luma) {
                val f = component.expandToF32(pix)
                rgb[o++] = f
                rgb[o++] = f
                rgb[o++] = f
            }
        }

        public fun <P> expandLumaRgba(luma: List<P>, rgba: FloatArray, component: ColorComponentForCicp<P>) {
            var o = 0
            var i = 0
            while (i < luma.size) {
                val f = component.expandToF32(luma[i])
                val a = if (i + 1 < luma.size) component.expandToF32(luma[i + 1]) else 1.0f
                rgba[o++] = f
                rgba[o++] = f
                rgba[o++] = f
                rgba[o++] = a
                i += 2
            }
        }

        public fun <P> expandRgb(input: List<P>, output: FloatArray, component: ColorComponentForCicp<P>) {
            for (i in input.indices) {
                output[i] = component.expandToF32(input[i])
            }
        }

        public fun <P> expandRgba(input: List<P>, output: FloatArray, component: ColorComponentForCicp<P>) {
            for (i in input.indices) {
                output[i] = component.expandToF32(input[i])
            }
        }

        public fun <P> clampRgb(input: FloatArray, output: MutableList<P>, component: ColorComponentForCicp<P>) {
            for (i in input.indices) {
                output.add(component.clampFromF32(input[i]))
            }
        }

        public fun <P> clampRgba(input: FloatArray, output: MutableList<P>, component: ColorComponentForCicp<P>) {
            for (i in input.indices) {
                output.add(component.clampFromF32(input[i]))
            }
        }

        public fun <P> clampRgbLuma(
            input: FloatArray,
            output: MutableList<P>,
            coef: FloatArray,
            component: ColorComponentForCicp<P>,
        ) {
            var i = 0
            while (i + 2 < input.size) {
                var luma = 0.0f
                luma = multiplyAccumulate(luma, input[i], coef[0])
                luma = multiplyAccumulate(luma, input[i + 1], coef[1])
                luma = multiplyAccumulate(luma, input[i + 2], coef[2])
                output.add(component.clampFromF32(luma))
                i += 3
            }
        }

        public fun <P> clampRgbaLuma(
            input: FloatArray,
            output: MutableList<P>,
            coef: FloatArray,
            component: ColorComponentForCicp<P>,
        ) {
            var i = 0
            while (i + 3 < input.size) {
                var luma = 0.0f
                luma = multiplyAccumulate(luma, input[i], coef[0])
                luma = multiplyAccumulate(luma, input[i + 1], coef[1])
                luma = multiplyAccumulate(luma, input[i + 2], coef[2])
                output.add(component.clampFromF32(luma))
                output.add(component.clampFromF32(input[i + 3]))
                i += 4
            }
        }
    }
}
