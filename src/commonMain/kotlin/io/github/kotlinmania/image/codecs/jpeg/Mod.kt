// port-lint: source src/codecs/jpeg/mod.rs
package io.github.kotlinmania.image.codecs.jpeg

/*
 * Decoding and Encoding of JPEG Images
 *
 * JPEG (Joint Photographic Experts Group) is an image format that supports lossy compression.
 * This module implements the Baseline JPEG standard.
 *
 * Related links:
 *   - http://www.w3.org/Graphics/JPEG/itu-t81.pdf — the JPEG specification
 *
 * Translation ledger for `src/codecs/jpeg/mod.rs`. Upstream `mod.rs` re-exports
 *   pub use self::decoder::JpegDecoder;
 *   pub use self::encoder::{JpegEncoder, PixelDensity, PixelDensityUnit};
 * which, per the workspace re-export rule, are not bridged through this file
 * with Kotlin `typealias` declarations; callers reference the per-submodule
 * packages directly.
 *
 * Items parceled out of `src/codecs/jpeg/mod.rs`:
 *   - submodule `entropy` -> Entropy.kt
 *   - submodule `transform` -> Transform.kt
 *
 * Callers migrated:
 */

private const val MODULE_LEDGER = true
