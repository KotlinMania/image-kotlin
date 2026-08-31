// port-lint: source codecs/tga/mod.rs
package io.github.kotlinmania.image.codecs.tga

/**
 * Decoding and Encoding of TGA Images.
 *
 * A decoder and encoder for TGA images.
 *
 * Translation ledger for `src/codecs/tga/mod.rs`. Upstream `mod.rs` re-exports:
 * - `TgaDecoder`
 * - `TgaEncoder`
 *
 * In the Kotlin port, callers reference `TgaDecoder` and `TgaEncoder` directly
 * from their defining files (`Decoder.kt` and `Encoder.kt`).
 */
public object TgaMod {
    public const val MODULE_NAME: String = "tga"
}
