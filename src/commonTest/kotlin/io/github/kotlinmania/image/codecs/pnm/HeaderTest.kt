// port-lint: tests codecs/pnm/header.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.io.BufferIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeaderTest {
    @Test
    fun bitmapHeaderPropertiesAndWrite() {
        val bitmap = BitmapHeader(encoding = SampleEncoding.Ascii, height = 20u, width = 10u)
        val header = PnmHeader.from(bitmap)

        assertEquals(PnmSubtype.Bitmap(SampleEncoding.Ascii), header.subtype())
        assertEquals(10u, header.width())
        assertEquals(20u, header.height())
        assertEquals(1u, header.maximalSample())
        assertNotNull(header.asBitmap())
        assertNull(header.asGraymap())

        val writer = BufferIoWrite()
        header.write(writer)
        val written = writer.toByteArray().decodeToString()
        assertEquals("P1\n10 20\n", written)
    }

    @Test
    fun graymapHeaderPropertiesAndWrite() {
        val graymap = GraymapHeader(encoding = SampleEncoding.Binary, height = 30u, width = 40u, maxwhite = 255u)
        val header = PnmHeader.from(graymap)

        assertEquals(PnmSubtype.Graymap(SampleEncoding.Binary), header.subtype())
        assertEquals(40u, header.width())
        assertEquals(30u, header.height())
        assertEquals(255u, header.maximalSample())
        assertNotNull(header.asGraymap())

        val writer = BufferIoWrite()
        header.write(writer)
        val written = writer.toByteArray().decodeToString()
        assertEquals("P5\n40 30 255\n", written)
    }

    @Test
    fun pixmapHeaderPropertiesAndWrite() {
        val pixmap = PixmapHeader(encoding = SampleEncoding.Binary, height = 100u, width = 200u, maxval = 65535u)
        val header = PnmHeader.from(pixmap)

        assertEquals(PnmSubtype.Pixmap(SampleEncoding.Binary), header.subtype())
        assertEquals(200u, header.width())
        assertEquals(100u, header.height())
        assertEquals(65535u, header.maximalSample())
        assertNotNull(header.asPixmap())

        val writer = BufferIoWrite()
        header.write(writer)
        val written = writer.toByteArray().decodeToString()
        assertEquals("P6\n200 100 65535\n", written)
    }

    @Test
    fun arbitraryHeaderPropertiesAndWrite() {
        val pam =
            ArbitraryHeader(
                height = 50u,
                width = 60u,
                depth = 4u,
                maxval = 255u,
                tupltype = ArbitraryTuplType.RGBAlpha,
            )
        val header = PnmHeader.from(pam)

        assertEquals(PnmSubtype.ArbitraryMap, header.subtype())
        assertEquals(60u, header.width())
        assertEquals(50u, header.height())
        assertEquals(255u, header.maximalSample())
        assertNotNull(header.asArbitrary())

        val writer = BufferIoWrite()
        header.write(writer)
        val written = writer.toByteArray().decodeToString()
        assertEquals("P7\nWIDTH 60\nHEIGHT 50\nDEPTH 4\nMAXVAL 255\nTUPLTYPE RGB_ALPHA\nENDHDR\n", written)
    }
}
