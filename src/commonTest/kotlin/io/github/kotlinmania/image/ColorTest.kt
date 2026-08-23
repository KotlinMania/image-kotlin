// port-lint: tests color.rs
package io.github.kotlinmania.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorTest {
    @Test
    fun testColorTypeProperties() {
        assertEquals(1u, ColorType.L8.bytesPerPixel())
        assertEquals(2u, ColorType.La8.bytesPerPixel())
        assertEquals(3u, ColorType.Rgb8.bytesPerPixel())
        assertEquals(4u, ColorType.Rgba8.bytesPerPixel())
        assertEquals(2u, ColorType.L16.bytesPerPixel())
        assertEquals(4u, ColorType.La16.bytesPerPixel())
        assertEquals(6u, ColorType.Rgb16.bytesPerPixel())
        assertEquals(8u, ColorType.Rgba16.bytesPerPixel())
        assertEquals(12u, ColorType.Rgb32F.bytesPerPixel())
        assertEquals(16u, ColorType.Rgba32F.bytesPerPixel())

        assertFalse(ColorType.L8.hasAlpha())
        assertTrue(ColorType.La8.hasAlpha())
        assertFalse(ColorType.Rgb8.hasAlpha())
        assertTrue(ColorType.Rgba8.hasAlpha())
        assertFalse(ColorType.L16.hasAlpha())
        assertTrue(ColorType.La16.hasAlpha())
        assertFalse(ColorType.Rgb16.hasAlpha())
        assertTrue(ColorType.Rgba16.hasAlpha())
        assertFalse(ColorType.Rgb32F.hasAlpha())
        assertTrue(ColorType.Rgba32F.hasAlpha())

        assertFalse(ColorType.L8.hasColor())
        assertFalse(ColorType.La8.hasColor())
        assertFalse(ColorType.L16.hasColor())
        assertFalse(ColorType.La16.hasColor())
        assertTrue(ColorType.Rgb8.hasColor())
        assertTrue(ColorType.Rgba8.hasColor())
        assertTrue(ColorType.Rgb16.hasColor())
        assertTrue(ColorType.Rgba16.hasColor())
        assertTrue(ColorType.Rgb32F.hasColor())
        assertTrue(ColorType.Rgba32F.hasColor())

        assertEquals(8u, ColorType.L8.bitsPerPixel())
        assertEquals(24u, ColorType.Rgb8.bitsPerPixel())
        assertEquals(32u, ColorType.Rgba8.bitsPerPixel())
        assertEquals(1u, ColorType.L8.channelCount())
        assertEquals(2u, ColorType.La8.channelCount())
        assertEquals(3u, ColorType.Rgb8.channelCount())
        assertEquals(4u, ColorType.Rgba8.channelCount())
    }

    @Test
    fun testExtendedColorTypeProperties() {
        assertEquals(1u, ExtendedColorType.L1.bitsPerPixel())
        assertEquals(2u, ExtendedColorType.L2.bitsPerPixel())
        assertEquals(4u, ExtendedColorType.L4.bitsPerPixel())
        assertEquals(8u, ExtendedColorType.L8.bitsPerPixel())
        assertEquals(16u, ExtendedColorType.L16.bitsPerPixel())
        assertEquals(24u, ExtendedColorType.Rgb8.bitsPerPixel())
        assertEquals(32u, ExtendedColorType.Rgba8.bitsPerPixel())
        assertEquals(24u, ExtendedColorType.Bgr8.bitsPerPixel())
        assertEquals(32u, ExtendedColorType.Bgra8.bitsPerPixel())
        assertEquals(32u, ExtendedColorType.Cmyk8.bitsPerPixel())
        assertEquals(64u, ExtendedColorType.Cmyk16.bitsPerPixel())
        assertEquals(15u, ExtendedColorType.Unknown(15u).bitsPerPixel())

        assertEquals(ColorType.Rgb8, ExtendedColorType.Rgb8.colorType())
        assertEquals(ColorType.Rgba8, ExtendedColorType.Rgba8.colorType())
        assertNull(ExtendedColorType.Bgr8.colorType())
        assertNull(ExtendedColorType.Cmyk8.colorType())

        assertEquals(300uL, ExtendedColorType.Rgb8.bufferSize(10u, 10u))
        assertEquals(400uL, ExtendedColorType.Rgba8.bufferSize(10u, 10u))
    }

    @Test
    fun testApplyWithAlphaRgba() {
        val rgba = Rgba(0, 0, 0, 0)
        rgba.applyWithAlpha({ s -> s }, { 0xFF })
        assertEquals(Rgba(0, 0, 0, 0xFF), rgba)
    }

    @Test
    fun testApplyWithAlphaRgb() {
        val rgb = Rgb(0, 0, 0)
        rgb.applyWithAlpha({ s -> s }, { error("bug") })
        assertEquals(Rgb(0, 0, 0), rgb)
    }

    @Test
    fun testMapWithAlphaRgba() {
        val rgba = Rgba(0, 0, 0, 0).mapWithAlpha({ s -> s }, { 0xFF })
        assertEquals(Rgba(0, 0, 0, 0xFF), rgba)
    }

    @Test
    fun testMapWithAlphaRgb() {
        val rgb = Rgb(0, 0, 0).mapWithAlpha({ s -> s }, { error("bug") })
        assertEquals(Rgb(0, 0, 0), rgb)
    }

    @Test
    fun testApplyWithoutAlphaRgba() {
        val rgba = Rgba(0, 0, 0, 0)
        rgba.applyWithoutAlpha { s -> s + 1 }
        assertEquals(Rgba(1, 1, 1, 0), rgba)
    }

    @Test
    fun testApplyWithoutAlphaRgb() {
        val rgb = Rgb(0, 0, 0)
        rgb.applyWithoutAlpha { s -> s + 1 }
        assertEquals(Rgb(1, 1, 1), rgb)
    }

    @Test
    fun testMapWithoutAlphaRgba() {
        val rgba = Rgba(0, 0, 0, 0).mapWithoutAlpha { s -> s + 1 }
        assertEquals(Rgba(1, 1, 1, 0), rgba)
    }

    @Test
    fun testMapWithoutAlphaRgb() {
        val rgb = Rgb(0, 0, 0).mapWithoutAlpha { s -> s + 1 }
        assertEquals(Rgb(1, 1, 1), rgb)
    }

    @Test
    fun testBlendLumaAlpha() {
        val a1 = LumaA(255u.toUByte(), 255u.toUByte())
        val b1 = LumaA(255u.toUByte(), 255u.toUByte())
        a1.blendUByte(b1)
        assertEquals(255u.toUByte(), a1.l)
        assertEquals(255u.toUByte(), a1.a)

        val a2 = LumaA(255u.toUByte(), 0u.toUByte())
        val b2 = LumaA(255u.toUByte(), 255u.toUByte())
        a2.blendUByte(b2)
        assertEquals(255u.toUByte(), a2.l)
        assertEquals(255u.toUByte(), a2.a)

        val a3 = LumaA(255u.toUByte(), 255u.toUByte())
        val b3 = LumaA(255u.toUByte(), 0u.toUByte())
        a3.blendUByte(b3)
        assertEquals(255u.toUByte(), a3.l)
        assertEquals(255u.toUByte(), a3.a)

        val a4 = LumaA(255u.toUByte(), 0u.toUByte())
        val b4 = LumaA(255u.toUByte(), 0u.toUByte())
        a4.blendUByte(b4)
        assertEquals(255u.toUByte(), a4.l)
        assertEquals(0u.toUByte(), a4.a)
    }

    @Test
    fun testBlendRgba() {
        val a1 = Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 255u.toUByte())
        val b1 = Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 255u.toUByte())
        a1.blendUByte(b1)
        assertEquals(Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 255u.toUByte()), a1)

        val a2 = Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 0u.toUByte())
        val b2 = Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 255u.toUByte())
        a2.blendUByte(b2)
        assertEquals(Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 255u.toUByte()), a2)

        val a3 = Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 255u.toUByte())
        val b3 = Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 0u.toUByte())
        a3.blendUByte(b3)
        assertEquals(Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 255u.toUByte()), a3)

        val a4 = Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 0u.toUByte())
        val b4 = Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 0u.toUByte())
        a4.blendUByte(b4)
        assertEquals(Rgba(255u.toUByte(), 255u.toUByte(), 255u.toUByte(), 0u.toUByte()), a4)
    }

    @Test
    fun accuracyConversion() {
        val pixel = Rgb(13u.toUByte(), 13u.toUByte(), 13u.toUByte())
        val luma = pixel.toLuma()
        assertEquals(13u.toUByte(), luma.l)
    }

    @Test
    fun testLosslessConversions() {
        val luma8 = Luma(63u.toUByte())
        val luma16 = Luma((63u * 65535u / 255u).toUShort())
        val luma8Back = Luma((luma16.l.toUInt() * 255u / 65535u).toUByte())
        assertEquals(luma8.channels(), luma8Back.channels())

        val lumaA8 = LumaA(63u.toUByte(), 63u.toUByte())
        val lumaA16 = LumaA((63u * 65535u / 255u).toUShort(), (63u * 65535u / 255u).toUShort())
        val lumaA8Back = LumaA((lumaA16.l.toUInt() * 255u / 65535u).toUByte(), (lumaA16.a.toUInt() * 255u / 65535u).toUByte())
        assertEquals(lumaA8.channels(), lumaA8Back.channels())

        val rgb8 = Rgb(63u.toUByte(), 63u.toUByte(), 63u.toUByte())
        val rgb16 = Rgb((63u * 65535u / 255u).toUShort(), (63u * 65535u / 255u).toUShort(), (63u * 65535u / 255u).toUShort())
        val rgb8Back = Rgb((rgb16.r.toUInt() * 255u / 65535u).toUByte(), (rgb16.g.toUInt() * 255u / 65535u).toUByte(), (rgb16.b.toUInt() * 255u / 65535u).toUByte())
        assertEquals(rgb8.channels(), rgb8Back.channels())

        val rgba8 = Rgba(63u.toUByte(), 63u.toUByte(), 63u.toUByte(), 63u.toUByte())
        val rgba16 = Rgba((63u * 65535u / 255u).toUShort(), (63u * 65535u / 255u).toUShort(), (63u * 65535u / 255u).toUShort(), (63u * 65535u / 255u).toUShort())
        val rgba8Back = Rgba((rgba16.r.toUInt() * 255u / 65535u).toUByte(), (rgba16.g.toUInt() * 255u / 65535u).toUByte(), (rgba16.b.toUInt() * 255u / 65535u).toUByte(), (rgba16.a.toUInt() * 255u / 65535u).toUByte())
        assertEquals(rgba8.channels(), rgba8Back.channels())
    }
}
