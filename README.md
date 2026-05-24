# once-cell-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fonce--cell--kotlin-blue.svg)](https://github.com/KotlinMania/once-cell-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/once-cell-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/once-cell-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/once-cell-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/once-cell-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`matklad/once_cell`](https://github.com/matklad/once_cell).

**Original Project:** This port is based on [`matklad/once_cell`](https://github.com/matklad/once_cell). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `matklad/once_cell`

> The text below is reproduced and lightly edited from [`https://github.com/matklad/once_cell`](https://github.com/matklad/once_cell). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

<p align="center"><img src="design/logo.png" alt="once_cell"></p>


[![Build Status](https://github.com/matklad/once_cell/actions/workflows/ci.yaml/badge.svg)](https://github.com/matklad/once_cell/actions)
[![Crates.io](https://img.shields.io/crates/v/once_cell.svg)](https://crates.io/crates/once_cell)
[![API reference](https://docs.rs/once_cell/badge.svg)](https://docs.rs/once_cell/)

## Overview

`once_cell` provides two new cell-like types, `unsync::OnceCell` and `sync::OnceCell`. `OnceCell`
might store arbitrary non-`Copy` types, can be assigned to at most once and provide direct access
to the stored contents. In a nutshell, API looks *roughly* like this:

```rust
impl OnceCell<T> {
    fn new() -> OnceCell<T> { ... }
    fn set(&self, value: T) -> Result<(), T> { ... }
    fn get(&self) -> Option<&T> { ... }
}
```

Note that, like with `RefCell` and `Mutex`, the `set` method requires only a shared reference.
Because of the single assignment restriction `get` can return an `&T` instead of `Ref<T>`
or `MutexGuard<T>`.

`once_cell` also has a `Lazy<T>` type, build on top of `OnceCell` which provides the same API as
the `lazy_static!` macro, but without using any macros:

```rust
use std::{sync::Mutex, collections::HashMap};
use once_cell::sync::Lazy;

static GLOBAL_DATA: Lazy<Mutex<HashMap<i32, String>>> = Lazy::new(|| {
    let mut m = HashMap::new();
    m.insert(13, "Spica".to_string());
    m.insert(74, "Hoyten".to_string());
    Mutex::new(m)
});

fn main() {
    println!("{:?}", GLOBAL_DATA.lock().unwrap());
}
```

More patterns and use-cases are in the [docs](https://docs.rs/once_cell/)!

# Related crates

* [double-checked-cell](https://github.com/niklasf/double-checked-cell)
* [lazy-init](https://crates.io/crates/lazy-init)
* [lazycell](https://crates.io/crates/lazycell)
* [mitochondria](https://crates.io/crates/mitochondria)
* [lazy_static](https://crates.io/crates/lazy_static)
* [async_once_cell](https://crates.io/crates/async_once_cell)
* [generic_once_cell](https://crates.io/crates/generic_once_cell) (bring your own mutex)

Parts of `once_cell` API are included into `std` [as of Rust 1.70.0](https://github.com/rust-lang/rust/pull/105587).

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:once-cell-kotlin:0.1.1")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`matklad/once_cell`](https://github.com/matklad/once_cell). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the once_cell authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`matklad/once_cell`](https://github.com/matklad/once_cell) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
