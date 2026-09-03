// port-lint: tests error.rs
package io.github.kotlinmania.image

import io.github.kotlinmania.image.io.ImageFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorTest {
    @Test
    fun testUnsupportedErrorFormatting() {
        val err1 =
            UnsupportedError.fromFormatAndKind(
                ImageFormatHint.Exact(ImageFormat.Png),
                UnsupportedErrorKind.Color(ExtendedColorType.Cmyk8),
            )
        assertTrue(err1.toString().contains("Cmyk8"))
        assertEquals(UnsupportedErrorKind.Color(ExtendedColorType.Cmyk8), err1.kind())

        val err2 = UnsupportedError.fromFormatHint(ImageFormatHint.Unknown)
        assertEquals("The image format could not be determined", err2.toString())
        assertEquals(ImageFormatHint.Unknown, err2.formatHint())
        assertTrue(err2.kind() is UnsupportedErrorKind.Format)

        val err3 =
            UnsupportedError.fromFormatAndKind(
                ImageFormatHint.Unknown,
                UnsupportedErrorKind.GenericFeature("animation"),
            )
        assertEquals("The decoder does not support the format feature animation", err3.toString())
        assertEquals(UnsupportedErrorKind.GenericFeature("animation"), err3.kind())
    }

    @Test
    fun testDecodingErrorFormatting() {
        val err = DecodingError(ImageFormatHint.Exact(ImageFormat.Jpeg), RuntimeException("corrupt header"))
        assertTrue(err.toString().contains("corrupt header"))
        assertEquals(ImageFormatHint.Exact(ImageFormat.Jpeg), err.format)
        assertEquals(ImageFormatHint.Exact(ImageFormat.Jpeg), err.formatHint())
    }

    @Test
    fun testEncodingErrorFormatting() {
        val err = EncodingError(ImageFormatHint.Exact(ImageFormat.Png), RuntimeException("encode failure"))
        assertTrue(err.toString().contains("encode failure"))
        assertEquals(ImageFormatHint.Exact(ImageFormat.Png), err.format)
        assertEquals(ImageFormatHint.Exact(ImageFormat.Png), err.formatHint())
    }

    @Test
    fun testParameterErrorFormatting() {
        val err = ParameterError.fromKind(ParameterErrorKind.DimensionMismatch)
        assertEquals("The Image's dimensions are either too small or too large", err.toString())
        assertEquals(ParameterErrorKind.DimensionMismatch, err.kind())
    }

    @Test
    fun testLimitErrorFormatting() {
        val err = LimitError.fromKind(LimitErrorKind.InsufficientMemory)
        assertEquals("Memory limit exceeded", err.toString())
        assertEquals(LimitErrorKind.InsufficientMemory, err.kind())
    }

    @Test
    fun testTryFromExtendedColorError() {
        val err = TryFromExtendedColorError(ExtendedColorType.Cmyk8)
        val imgErr = err.toImageError()
        assertTrue(imgErr is ImageError.Unsupported)
    }

    private inline fun <reified T : Any> checkSendSync() {
        assertTrue(T::class.isInstance(ImageError.Parameter(ParameterError.fromKind(ParameterErrorKind.DimensionMismatch))))
    }

    @Test
    fun assertSendSync() {
        checkSendSync<ImageError>()
    }

    @Test
    fun testSendSyncStability() {
        checkSendSync<ImageError>()
    }
}
