// port-lint: source codecs/hdr/mod.rs
package io.github.kotlinmania.image.codecs.hdr

/**
 * Decoding and Encoding of Radiance HDR Images.
 *
 * A decoder and encoder for Radiance HDR images.
 *
 * Translation ledger for `src/codecs/hdr/mod.rs`. Upstream `mod.rs` re-exports:
 * - `HdrDecoder`
 * - `HdrEncoder`
 * - `HdrMetadata`
 *
 * In the Kotlin port, callers reference these types directly
 * from their defining files (`Decoder.kt` and `Encoder.kt`).
 */
public object HdrMod {
    public const val MODULE_NAME: String = "hdr"
}
