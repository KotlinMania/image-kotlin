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

        val err2 = UnsupportedError.fromFormatHint(ImageFormatHint.Unknown)
        assertEquals("The image format could not be determined", err2.toString())

        val err3 =
            UnsupportedError.fromFormatAndKind(
                ImageFormatHint.Unknown,
                UnsupportedErrorKind.GenericFeature("animation"),
            )
        assertEquals("The decoder does not support the format feature animation", err3.toString())
    }

    @Test
    fun testDecodingErrorFormatting() {
        val err = DecodingError(ImageFormatHint.Exact(ImageFormat.Jpeg), RuntimeException("corrupt header"))
        assertTrue(err.toString().contains("corrupt header"))
    }

    @Test
    fun testParameterErrorFormatting() {
        val err = ParameterError.fromKind(ParameterErrorKind.DimensionMismatch)
        assertEquals("The Image's dimensions are either too small or too large", err.toString())
    }

    @Test
    fun testLimitErrorFormatting() {
        val err = LimitError.fromKind(LimitErrorKind.InsufficientMemory)
        assertEquals("Memory limit exceeded", err.toString())
    }

    @Test
    fun testTryFromExtendedColorError() {
        val err = TryFromExtendedColorError(ExtendedColorType.Cmyk8)
        val imgErr = err.toImageError()
        assertTrue(imgErr is ImageError.Unsupported)
    }
}
