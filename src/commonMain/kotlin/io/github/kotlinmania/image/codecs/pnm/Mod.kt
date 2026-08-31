// port-lint: source image/src/codecs/pnm/mod.rs
package io.github.kotlinmania.image.codecs.pnm

/*
 * Decoding and encoding of PNM (PBM/PGM/PPM/PAM) images.
 *
 * Translation ledger for `src/codecs/pnm/mod.rs`. Per the workspace
 * mod.rs rule, an upstream file that mixes submodule declarations with
 * concrete items may be parceled into focused Kotlin files; this file is
 * the tracking ledger and each parceled file carries its own
 * `// port-lint: source <upstream>` header.
 *
 * Upstream submodule declarations:
 *
 *   mod autobreak;
 *   mod decoder;
 *   mod encoder;
 *   mod header;
 *
 * Items parceled out of `src/codecs/pnm/`:
 *   - autobreak::AutoBreak (line-wrapping writer adapter)
 *       -> AutoBreak.kt (+ AutoBreakTest.kt under commonTest)
 *   - header::* (PNM headers, subtypes, sample encodings, tuple types)
 *       -> Header.kt (+ HeaderTest.kt under commonTest)
 *   - decoder::* (PNM decoder for PBM, PGM, PPM, PAM)
 *       -> Decoder.kt (+ DecoderTests.kt under commonTest)
 *   - encoder::* (PNM encoder for PBM, PGM, PPM, PAM)
 *       -> Encoder.kt (+ EncoderTests.kt under commonTest)
 *
 * Callers migrated:
 */

private const val MODULE_LEDGER = true
