// port-lint: source src/metadata.rs
package io.github.kotlinmania.image.metadata

/*
 * Types describing image metadata.
 *
 * Translation ledger for `src/metadata.rs`. Per the workspace mod.rs rule,
 * an upstream file that mixes submodule declarations with concrete items may
 * be parceled into focused Kotlin files; this file is the tracking ledger and
 * each parceled file carries `// port-lint: source src/metadata.rs`.
 *
 * Upstream submodule declarations:
 *
 *   pub(crate) mod cicp;
 *
 *   pub use self::cicp::{
 *       Cicp, CicpColorPrimaries, CicpMatrixCoefficients, CicpTransferCharacteristics,
 *       CicpTransform, CicpVideoFullRangeFlag,
 *   };
 *
 * Per the workspace re-export rule, the Kotlin port does not bridge the
 * `pub use self::cicp::*` names through this file; callers reference the
 * `io.github.kotlinmania.image.metadata.cicp` package directly when the
 * `cicp.rs` parcel lands.
 *
 * Items parceled out of `src/metadata.rs`:
 *   - `Orientation` enum + `from_exif`/`to_exif`/`from_exif_chunk`/
 *     `remove_from_exif_chunk` -> Orientation.kt
 *   - `ExifEndian` helper enum and the private `from_exif_chunk_inner` /
 *     `locate_orientation_entry` helpers travel with Orientation.kt because
 *     they are private to the orientation parsing logic.
 *
 * Callers migrated:
 */

private const val MODULE_LEDGER = true
