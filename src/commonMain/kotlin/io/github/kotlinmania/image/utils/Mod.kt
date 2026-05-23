// port-lint: source src/utils/mod.rs
package io.github.kotlinmania.image.utils

/*
 * Utilities.
 *
 * Translation ledger for `src/utils/mod.rs`. Per the workspace mod.rs rule,
 * an upstream `mod.rs` with real implementation may be parceled into focused
 * Kotlin files; this file is the tracking ledger and each parceled file
 * carries `// port-lint: source src/utils/mod.rs`.
 *
 * Items parceled out of `src/utils/mod.rs`:
 *   - `expand_packed`               function -> ExpandPacked.kt
 *   - `expand_bits`                 function -> ExpandBits.kt
 *                 with `#[cfg(test)] mod test::gray_to_luma8_skip`
 *                                    -> ExpandBitsTests.kt (commonTest)
 *   - `check_dimension_overflow`    function -> CheckDimensionOverflow.kt
 *   - `clamp`                       function -> Clamp.kt
 *   - `vec_try_with_capacity`       function -> VecTryWithCapacity.kt
 *
 * Items still pending port:
 *   - `vec_copy_to_u8<T: bytemuck::Pod>`: this function relies on
 *     `bytemuck::cast_slice` to reinterpret an arbitrary `Pod` slice as
 *     bytes. Kotlin Multiplatform has no equivalent of the `bytemuck::Pod`
 *     trait and no portable mechanism for type-punning a generic slice into
 *     a byte slice. Porting this item requires a separate `bytemuck-kotlin`
 *     port to land first.
 */
