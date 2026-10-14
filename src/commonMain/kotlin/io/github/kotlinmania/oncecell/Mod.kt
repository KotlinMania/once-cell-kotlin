// port-lint: source lib.rs
package io.github.kotlinmania.oncecell

/**
 * Single-assignment cells and lazy values for Kotlin Multiplatform.
 *
 * # Overview
 *
 * This library provides two cell-like types, `unsync.OnceCell` and
 * `sync.OnceCell`. A `OnceCell` stores arbitrary values, can be assigned to
 * at most once, and provides direct access to the stored contents without
 * RAII guards.
 *
 * The `sync` package is thread-safe, while the `unsync` package is single-threaded.
 *
 * # Recipes
 *
 * `OnceCell` is useful for a variety of patterns:
 * - Safe initialization of global data
 * - Lazy initialized global data via `sync.Lazy` and `unsync.Lazy`
 * - General purpose lazy evaluation with local closures
 * - Lazily compiled regular expressions or expensive resources
 * - Runtime dynamic symbol tables and lazy loading
 * - Building-block atomic cells via the `race` package (`OnceNonZeroUsize`, `OnceBool`, `OnceRef`, `OnceBox`)
 */

// Tracking file for upstream lib.rs.
//
// Upstream module declarations (translated from upstream conditional compilation attributes):
// - Module `imp` is backed by standard library synchronization (`imp.std`), parking lot (`imp.pl`),
//   or critical section (`imp.cs`) depending on target configuration.
// - Module `unsync` provides single-threaded `OnceCell` and `Lazy`, parceled into
//   `unsync/OnceCell.kt` and `unsync/Lazy.kt`.
// - Module `sync` provides thread-safe `OnceCell` and `Lazy`, parceled into
//   `sync/OnceCell.kt` and `sync/Lazy.kt`.
// - Module `race` provides lock-free atomic cells where multiple threads may race to initialize,
//   implemented in `race/Race.kt`.
//
// Callers migrated:
// - none
//
// Projected callers (Rust). Cross-referenced from `RUST_CALLERS.md`:
// - `sync.Lazy` (28 sites across 11 caller crates): `codex-kotlin`, `arc-swap-kotlin`, `insta-kotlin`.
// - `sync.OnceCell` (10 sites across 8 caller crates): `dotenvy-kotlin`, `libwebrtc-kotlin`,
//   `reqwest-kotlin`, `runfiles-kotlin`, `serial-test-kotlin`, `starlark-kotlin`,
//   `syntect-kotlin`, `tempfile-kotlin`.
// - `unsync.OnceCell` (2 sites in 1 caller crate): `starlark-kotlin`.
// - `race.OnceBox` (1 site in 1 caller crate): `rustls-kotlin`.
