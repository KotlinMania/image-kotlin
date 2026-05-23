// port-lint: source src/metadata.rs
package io.github.kotlinmania.image.metadata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrientationTest {

    @Test
    fun exifRoundTripCoversAllVariants() {
        for (variant in Orientation.entries) {
            val exif = variant.toExif()
            assertEquals(variant, Orientation.fromExif(exif), "round-trip for $variant")
        }
    }

    @Test
    fun fromExifRejectsOutOfRange() {
        assertNull(Orientation.fromExif(0))
        assertNull(Orientation.fromExif(9))
        assertNull(Orientation.fromExif(255))
    }

    @Test
    fun fromExifChunkParsesLittleEndian() {
        val chunk = exifChunkLittleEndian(orientation = 2)
        assertEquals(Orientation.FlipHorizontal, Orientation.fromExifChunk(chunk))
    }

    @Test
    fun fromExifChunkParsesBigEndian() {
        val chunk = exifChunkBigEndian(orientation = 6)
        assertEquals(Orientation.Rotate90, Orientation.fromExifChunk(chunk))
    }

    @Test
    fun fromExifChunkReturnsNullForUnknownMagic() {
        val chunk = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        assertNull(Orientation.fromExifChunk(chunk))
    }

    @Test
    fun fromExifChunkReturnsNullForTruncatedChunk() {
        assertNull(Orientation.fromExifChunk(byteArrayOf(0x49, 0x49, 0x2a)))
    }

    @Test
    fun removeFromExifChunkExtractsAndClears() {
        val chunk = exifChunkLittleEndian(orientation = 2)
        val extracted = Orientation.removeFromExifChunk(chunk)
        assertEquals(Orientation.FlipHorizontal, extracted)
        // Subsequent extraction should now report NoTransforms because the value was cleared.
        assertEquals(Orientation.NoTransforms, Orientation.fromExifChunk(chunk))
    }

    @Test
    fun removeFromExifChunkClearsBigEndianValue() {
        val chunk = exifChunkBigEndian(orientation = 6)
        assertEquals(Orientation.Rotate90, Orientation.removeFromExifChunk(chunk))
        assertEquals(Orientation.NoTransforms, Orientation.fromExifChunk(chunk))
    }

    @Test
    fun fromExifChunkReturnsNullWhenOrientationEntryMissing() {
        // Build an Exif chunk whose single IFD entry has a tag != 0x112.
        val chunk = exifChunkLittleEndianWithCustomTag(tag = 0x100, orientation = 6)
        assertNull(Orientation.fromExifChunk(chunk))
    }

    private fun exifChunkLittleEndian(orientation: Int): ByteArray =
        exifChunkLittleEndianWithCustomTag(tag = 0x112, orientation = orientation)

    private fun exifChunkLittleEndianWithCustomTag(tag: Int, orientation: Int): ByteArray {
        // Header: II, magic 42, then u32 IFD offset (=8) in little-endian.
        val header = byteArrayOf(0x49, 0x49, 0x2a, 0x00, 0x08, 0x00, 0x00, 0x00)
        // IFD entry count = 1 (u16 little-endian).
        val count = byteArrayOf(0x01, 0x00)
        val entry = byteArrayOf(
            (tag and 0xff).toByte(), ((tag shr 8) and 0xff).toByte(),
            0x03, 0x00,                          // format = 3 (u16) little-endian
            0x01, 0x00, 0x00, 0x00,              // count = 1 (u32) little-endian
            (orientation and 0xff).toByte(),
            ((orientation shr 8) and 0xff).toByte(),
            0x00, 0x00,                          // padding (u16) little-endian
        )
        return header + count + entry
    }

    private fun exifChunkBigEndian(orientation: Int): ByteArray {
        val header = byteArrayOf(0x4d, 0x4d, 0x00, 0x2a, 0x00, 0x00, 0x00, 0x08)
        val count = byteArrayOf(0x00, 0x01)
        val entry = byteArrayOf(
            0x01, 0x12,                          // tag = 0x112 (u16) big-endian
            0x00, 0x03,                          // format = 3 (u16) big-endian
            0x00, 0x00, 0x00, 0x01,              // count = 1 (u32) big-endian
            ((orientation shr 8) and 0xff).toByte(),
            (orientation and 0xff).toByte(),
            0x00, 0x00,                          // padding
        )
        return header + count + entry
    }
}
