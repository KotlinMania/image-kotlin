// port-lint: source src/utils/mod.rs
package io.github.kotlinmania.image.utils

/**
 * Allocate a [MutableList] with the requested initial [capacity]. The Rust
 * counterpart returns `Result<Vec<T>, TryReserveError>` because Rust exposes
 * a fallible reservation API (`try_reserve_exact`). Kotlin's `ArrayList`
 * constructor pre-allocates the backing storage and throws
 * `OutOfMemoryError` on allocation failure rather than returning a typed
 * error, so the equivalent Kotlin surface is a plain function that returns
 * the list directly.
 */
internal fun <T> vecTryWithCapacity(capacity: Int): MutableList<T> = ArrayList(capacity)
