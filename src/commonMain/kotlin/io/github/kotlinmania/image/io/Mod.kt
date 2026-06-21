// port-lint: source src/io.rs
package io.github.kotlinmania.image.io

/*
 * Input and output of images.
 *
 * Translation ledger for `src/io.rs`. Per the workspace mod.rs rule, an
 * upstream file that mixes submodule declarations with concrete items may
 * be parceled into focused Kotlin files; this file is the tracking ledger
 * and each parceled file carries its own `// port-lint: source <upstream>`
 * header.
 *
 * Upstream submodule declarations:
 *
 *   pub(crate) mod decoder;
 *   pub(crate) mod encoder;
 *   pub(crate) mod format;
 *   pub(crate) mod free_functions;
 *   pub(crate) mod image_reader_type;
 *   pub(crate) mod limits;
 *
 * Per the workspace re-export rule, the deprecated re-exports
 *
 *   pub type Reader<R> = ImageReader<R>;
 *   pub type Limits = limits::Limits;
 *   pub type LimitSupport = limits::LimitSupport;
 *
 * and
 *
 *   pub(crate) use self::image_reader_type::ImageReader;
 *
 * are not bridged here via `typealias`. Callers reference the per-submodule
 * packages directly. Both are marked `#[deprecated]` upstream and are not
 * part of the long-term Kotlin surface.
 *
 * The crate-internal `ReadExt` trait and `seek_relative` helper depend on
 * `std::io::{Read, Seek}` and arrive when the IO layer lands.
 *
 * Items parceled out of `src/io.rs` (and the directory):
 *   - `ImageFormat` enum + `from_extension` / `from_path` / `from_mime_type` /
 *     `to_mime_type` / `can_read` / `can_write` / `extensions_str` /
 *     `reading_enabled` / `writing_enabled` / `all` -> Format.kt
 *
 * Callers migrated:
 */

private const val MODULE_LEDGER = true
