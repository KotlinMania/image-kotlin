// port-lint: source math/mod.rs
package io.github.kotlinmania.image.math

/**
 * Mathematical helper functions and types.
 *
 * Re-exports from upstream `math/mod.rs`:
 *
 * ```
 * pub use self::rect::Rect;
 * pub(crate) use utils::multiply_accumulate;
 * pub(super) use utils::resize_dimensions;
 * ```
 *
 * In the Kotlin port these symbols are referenced directly from their defining files
 * (`Rect.kt`, `Utils.kt`) — no aliasing is performed in this file.
 */

// Callers migrated:
