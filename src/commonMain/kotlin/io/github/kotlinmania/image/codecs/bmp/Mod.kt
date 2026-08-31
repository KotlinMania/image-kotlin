// port-lint: source image/src/codecs/bmp/mod.rs
package io.github.kotlinmania.image.codecs.bmp

/**
 * Decoding and Encoding of BMP Images.
 *
 * A decoder and encoder for BMP (Windows Bitmap) images.
 *
 * Translation ledger for `src/codecs/bmp/mod.rs`. Upstream `mod.rs` re-exports:
 * - `BmpDecoder`
 * - `BmpEncoder`
 *
 * In the Kotlin port, callers reference `BmpDecoder` and `BmpEncoder` directly
 * from their defining files (`Decoder.kt` and `Encoder.kt`).
 */
public object BmpMod {
    public const val MODULE_NAME: String = "bmp"
}
