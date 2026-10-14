// port-lint: source lib.rs
package io.github.kotlinmania.oncecell.unsync

/**
 * Single-threaded version of `OnceCell` and `Lazy`.
 */

// Tracking file for the upstream `unsync` module in `lib.rs`.
//
// Upstream symbols parceled into per-symbol files:
// - `OnceCell` and associated result/initializer types in `unsync/OnceCell.kt`.
// - `Lazy` and associated result/initializer types in `unsync/Lazy.kt`.
//
// Callers migrated:
// - none
//
// Projected callers (Rust). Cross-referenced from `RUST_CALLERS.md`:
// - `unsync.OnceCell`: `starlark-kotlin` (2 sites).
