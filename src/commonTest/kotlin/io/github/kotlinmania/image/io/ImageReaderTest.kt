// port-lint: tests image/src/io/image_reader_type.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.codecs.FarbfeldEncoder
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageReaderTest {
    @Test
    fun testGuessFormatAndDecode() {
        val width = 2u
        val height = 2u
        val originalData = ByteArray(32) { (it * 3).toByte() }

        val sink = BufferIoWrite()
        val encoder = FarbfeldEncoder(sink)
        encoder.encode(originalData, width, height)

        val farbfeldBytes = sink.toByteArray()

        val reader = ImageReader.fromBytes(farbfeldBytes).withGuessedFormat()
        assertEquals(ImageFormat.Farbfeld, reader.format())
        assertEquals(Pair(width, height), reader.dimensions())

        val decoder = reader.intoDecoder()
        assertEquals(ColorType.Rgba16, decoder.colorType())
        val decoded = ByteArray(32)
        decoder.readImage(decoded)
        assertEquals(originalData.toList(), decoded.toList())
    }
}
