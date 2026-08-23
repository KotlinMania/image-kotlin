// port-lint: source metadata/cicp.rs
package io.github.kotlinmania.image.metadata

import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind

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
}
