// port-lint: tests image/src/codecs/tga/header.rs
package io.github.kotlinmania.image.codecs.tga

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeaderTest {
    @Test
    fun testImageTypeProperties() {
        val rawColor = ImageType.fromValue(2u)
        assertEquals(ImageType.RawTrueColor, rawColor)
        assertTrue(rawColor.isColor())
        assertFalse(rawColor.isColorMapped())
        assertFalse(rawColor.isEncoded())

        val runColor = ImageType.fromValue(10u)
        assertEquals(ImageType.RunTrueColor, runColor)
        assertTrue(runColor.isColor())
        assertFalse(runColor.isColorMapped())
        assertTrue(runColor.isEncoded())

        val runMap = ImageType.fromValue(9u)
        assertEquals(ImageType.RunColorMap, runMap)
        assertTrue(runMap.isColor())
        assertTrue(runMap.isColorMapped())
        assertTrue(runMap.isEncoded())

        val rawGray = ImageType.fromValue(3u)
        assertEquals(ImageType.RawGrayScale, rawGray)
        assertFalse(rawGray.isColor())
    }

    @Test
    fun testFromPixelInfo() {
        val headerRgb = Header.fromPixelInfo(ExtendedColorType.Rgb8, 640u, 480u, false).getOrThrow()
        assertEquals(ImageType.RawTrueColor.value, headerRgb.imageType)
        assertEquals(640u.toUShort(), headerRgb.imageWidth)
        assertEquals(480u.toUShort(), headerRgb.imageHeight)
        assertEquals(24u.toUByte(), headerRgb.pixelDepth)

        val headerRgbaRle = Header.fromPixelInfo(ExtendedColorType.Rgba8, 320u, 240u, true).getOrThrow()
        assertEquals(ImageType.RunTrueColor.value, headerRgbaRle.imageType)
        assertEquals(32u.toUByte(), headerRgbaRle.pixelDepth)
    }

    @Test
    fun testHeaderRoundtrip() {
        val original =
            Header(
                idLength = 5u,
                mapType = 1u,
                imageType = 2u,
                mapOrigin = 0u,
                mapLength = 256u,
                mapEntrySize = 24u,
                xOrigin = 0u,
                yOrigin = 0u,
                imageWidth = 800u,
                imageHeight = 600u,
                pixelDepth = 24u,
                imageDesc = 32u,
            )

        val writeBuffer = BufferIoWrite()
        assertTrue(original.writeTo(writeBuffer).isSuccess)

        val bytes = writeBuffer.toByteArray()
        val readBuffer = BufferIoRead(bytes)
        val decoded = Header.fromReader(readBuffer).getOrThrow()

        assertEquals(original, decoded)
    }
}
