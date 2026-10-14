// port-lint: source lib.rs
package io.github.kotlinmania.oncecell.sync

/**
 * Thread-safe synchronization primitive which can be written to only once.
 */

// Tracking file for the upstream `sync` module in `lib.rs`.
//
// Upstream symbols parceled into per-symbol files:
// - `OnceCell`, `Void`, `Target`, and associated result/initializer types in `sync/OnceCell.kt`.
// - `Lazy` and associated result/initializer types in `sync/Lazy.kt`.
//
// Callers migrated:
// - none
//
// Projected callers (Rust). Cross-referenced from `RUST_CALLERS.md`:
// - `sync.Lazy` (28 sites across 11 caller crates): `codex-kotlin`, `arc-swap-kotlin`, `insta-kotlin`.
// - `sync.OnceCell` (10 sites across 8 caller crates): `dotenvy-kotlin`, `libwebrtc-kotlin`,
//   `reqwest-kotlin`, `runfiles-kotlin`, `serial-test-kotlin`, `starlark-kotlin`,
//   `syntect-kotlin`, `tempfile-kotlin`.
