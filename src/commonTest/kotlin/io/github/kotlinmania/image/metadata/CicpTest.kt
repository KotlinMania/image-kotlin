// port-lint: tests metadata/cicp.rs
package io.github.kotlinmania.image.metadata

import io.github.kotlinmania.image.LayoutWithColor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CicpTest {
    private fun noCoefficientFallback(): FloatArray = error("Fallback coefficients required")

    @Test
    fun testDerivedLuminance() {
        val luminance = Cicp.SRGB.intoRgb().derivedLuminance()
        assertNotNull(luminance)
        val kr = luminance[0]
        val kg = luminance[1]
        val kb = luminance[2]
        assertTrue(abs(kr - 0.2126f) < 1e-4f)
        assertTrue(abs(kg - 0.7152f) < 1e-4f)
        assertTrue(abs(kb - 0.0722f) < 1e-4f)
    }

    @Test
    fun testQualifyStability() {
        assertTrue(Cicp.SRGB.qualifyStability())
        assertTrue(Cicp.SRGB_LINEAR.qualifyStability())
        assertTrue(Cicp.DISPLAY_P3.qualifyStability())
    }

    @Test
    fun testTryIntoRgb() {
        val srgb = Cicp.SRGB
        val res = srgb.tryIntoRgb()
        assertTrue(res.isSuccess)
        assertEquals(srgb, res.getOrThrow().toCicp())
    }

    @Test
    fun testCanCreateTransforms() {
        assertNotNull(CicpTransform.new(Cicp.SRGB, Cicp.SRGB))
        assertNotNull(CicpTransform.new(Cicp.SRGB, Cicp.DISPLAY_P3))
        assertNotNull(CicpTransform.new(Cicp.DISPLAY_P3, Cicp.SRGB))
        assertNotNull(CicpTransform.new(Cicp.DISPLAY_P3, Cicp.DISPLAY_P3))
    }

    @Test
    fun testTransformPixelsSrgb() {
        val data = listOf(255u.toUByte(), 0u.toUByte(), 0u.toUByte(), 255u.toUByte())
        val color = Cicp.SRGB.intoRgb()
        val rgba =
            color.castPixels(
                data,
                LayoutWithColor.Rgba,
                LayoutWithColor.Rgb,
                ColorComponentForCicp.UBYTE,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(255u.toUByte(), 0u.toUByte(), 0u.toUByte()), rgba)

        val luma =
            color.castPixels(
                data,
                LayoutWithColor.Rgba,
                LayoutWithColor.Luma,
                ColorComponentForCicp.UBYTE,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(54u.toUByte()), luma)

        val lumaA =
            color.castPixels(
                data,
                LayoutWithColor.Rgba,
                LayoutWithColor.LumaAlpha,
                ColorComponentForCicp.UBYTE,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(54u.toUByte(), 255u.toUByte()), lumaA)
    }

    @Test
    fun testTransformPixelsSrgb16() {
        val data1 = listOf((UShort.MAX_VALUE / 2u).toUShort())
        val color = Cicp.SRGB.intoRgb()
        val rgb1 =
            color.castPixels(
                data1,
                LayoutWithColor.Luma,
                LayoutWithColor.Rgb,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(127u.toUByte(), 127u.toUByte(), 127u.toUByte()), rgb1)

        val luma1 =
            color.castPixels(
                data1,
                LayoutWithColor.Luma,
                LayoutWithColor.Luma,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(127u.toUByte()), luma1)

        val lumaA1 =
            color.castPixels(
                data1,
                LayoutWithColor.Luma,
                LayoutWithColor.LumaAlpha,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(127u.toUByte(), 255u.toUByte()), lumaA1)

        val data2 = listOf(((UShort.MAX_VALUE / 2u) + 1u).toUShort())
        val rgb2 =
            color.castPixels(
                data2,
                LayoutWithColor.Luma,
                LayoutWithColor.Rgb,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(128u.toUByte(), 128u.toUByte(), 128u.toUByte()), rgb2)

        val luma2 =
            color.castPixels(
                data2,
                LayoutWithColor.Luma,
                LayoutWithColor.Luma,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(128u.toUByte()), luma2)

        val lumaA2 =
            color.castPixels(
                data2,
                LayoutWithColor.Luma,
                LayoutWithColor.LumaAlpha,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(128u.toUByte(), 255u.toUByte()), lumaA2)
    }

    @Test
    fun testTransformPixelsSrgbLumaAlpha() {
        val data1 = listOf((UShort.MAX_VALUE / 2u).toUShort(), UShort.MAX_VALUE)
        val color = Cicp.SRGB.intoRgb()
        val rgb1 =
            color.castPixels(
                data1,
                LayoutWithColor.LumaAlpha,
                LayoutWithColor.Rgb,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(127u.toUByte(), 127u.toUByte(), 127u.toUByte()), rgb1)

        val luma1 =
            color.castPixels(
                data1,
                LayoutWithColor.LumaAlpha,
                LayoutWithColor.Luma,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(127u.toUByte()), luma1)

        val lumaA1 =
            color.castPixels(
                data1,
                LayoutWithColor.LumaAlpha,
                LayoutWithColor.LumaAlpha,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(127u.toUByte(), 255u.toUByte()), lumaA1)

        val data2 = listOf(((UShort.MAX_VALUE / 2u) + 1u).toUShort(), UShort.MAX_VALUE)
        val rgb2 =
            color.castPixels(
                data2,
                LayoutWithColor.LumaAlpha,
                LayoutWithColor.Rgb,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(128u.toUByte(), 128u.toUByte(), 128u.toUByte()), rgb2)

        val luma2 =
            color.castPixels(
                data2,
                LayoutWithColor.LumaAlpha,
                LayoutWithColor.Luma,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(128u.toUByte()), luma2)

        val lumaA2 =
            color.castPixels(
                data2,
                LayoutWithColor.LumaAlpha,
                LayoutWithColor.LumaAlpha,
                ColorComponentForCicp.USHORT,
                ColorComponentForCicp.UBYTE,
                ::noCoefficientFallback,
            )
        assertEquals(listOf(128u.toUByte(), 255u.toUByte()), lumaA2)
    }
}
