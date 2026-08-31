// port-lint: source image/src/codecs/ico/mod.rs
package io.github.kotlinmania.image.codecs.ico

/**
 * Decoding and Encoding of ICO files.
 *
 * A decoder and encoder for ICO (Windows Icon) image container files.
 *
 * Translation ledger for `src/codecs/ico/mod.rs`. Upstream `mod.rs` re-exports:
 * - `IcoDecoder`
 * - `IcoEncoder`
 * - `IcoFrame`
 *
 * In the Kotlin port, callers reference `IcoDecoder`, `IcoEncoder`, and `IcoFrame`
 * directly from their defining files (`Decoder.kt` and `Encoder.kt`).
 */
public object IcoMod {
    public const val MODULE_NAME: String = "ico"
}
