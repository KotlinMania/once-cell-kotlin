<div align="center">

# `once-cell-kotlin`

**Single-assignment cells and lazy values for Kotlin Multiplatform — without RAII guards.**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/once-cell-kotlin?style=for-the-badge&logo=apachemaven&label=Maven%20Central&color=5C4EE5)](https://central.sonatype.com/artifact/io.github.kotlinmania/once-cell-kotlin)
[![CI Status](https://img.shields.io/github/actions/workflow/status/KotlinMania/once-cell-kotlin/ci.yml?branch=main&style=for-the-badge&logo=githubactions&label=CI)](https://github.com/KotlinMania/once-cell-kotlin/actions/workflows/ci.yml)
[![CodeQL](https://img.shields.io/github/actions/workflow/status/KotlinMania/once-cell-kotlin/codeql.yml?branch=main&style=for-the-badge&logo=github&label=CodeQL)](https://github.com/KotlinMania/once-cell-kotlin/actions/workflows/codeql.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0--Beta1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![KMP Targets](https://img.shields.io/badge/Targets-JVM%20%7C%20Android%20%7C%20iOS%20%7C%20macOS%20%7C%20Linux%20%7C%20Windows%20%7C%20JS%20%7C%20Wasm-F88909?style=for-the-badge)](https://kotlinlang.org/docs/multiplatform.html)
[![Swift Export](https://img.shields.io/badge/Swift%20Export-Swift%206-F05138?style=for-the-badge&logo=swift&logoColor=white)](https://swift.org)
[![License: MIT OR Apache-2.0](https://img.shields.io/badge/License-MIT%20OR%20Apache--2.0-00B4AB?style=for-the-badge)](LICENSE)

</div>

---

## Overview

`once-cell-kotlin` provides single-assignment cells and lazy initialization primitives for **Kotlin Multiplatform** across JVM, Android, Apple (iOS, macOS, tvOS, watchOS with native Swift 6 Export), Linux, Windows, JavaScript, and WebAssembly.

Unlike a mutex or read-write lock, a `OnceCell` can be written to **at most once**. Because an initialized value is never overwritten through a shared reference, `get()` returns a direct reference (`T?`) without lock guards, wrapper proxies, or contention after initialization.

### Three Specialized Modules

| Package | Primary Types | Concurrency Model | Best For |
| :--- | :--- | :--- | :--- |
| **`io.github.kotlinmania.oncecell.sync`** | `OnceCell<T>`, `Lazy<T>`, `StringOnceCell`, `IntOnceCell`, `StringLazy`, `IntLazy` | Thread-safe synchronized initialization; guarantees **at most one** initializer runs while concurrent callers wait. | Shared globals, thread-safe caches, expensive one-time setup, cyclic graph construction. |
| **`io.github.kotlinmania.oncecell.unsync`** | `OnceCell<T>`, `Lazy<T>`, `StringOnceCell`, `IntOnceCell`, `StringLazy`, `IntLazy` | Single-threaded zero-atomic fast path with deterministic reentrant-initialization detection. | Local lazy structs, single-threaded event loops, coroutines confined to a single dispatcher. |
| **`io.github.kotlinmania.oncecell.race`** | `OnceNonZeroUsize`, `OnceBool`, `OnceRef<T>`, `OnceBox<T>` | Lock-free, wait-free atomic compare-and-exchange (`CAS`); concurrent initializers may race, and the **first winner** is observed by all threads. | Wait-free atomic flags, idempotent counters, non-blocking pointer/box cells. |

---

## Installation

### Gradle (Kotlin DSL)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.kotlinmania:once-cell-kotlin:0.1.1")
        }
    }
}
```

### Gradle Version Catalog (`gradle/libs.versions.toml`)

```toml
[versions]
once-cell = "0.1.1"

[libraries]
once-cell-kotlin = { module = "io.github.kotlinmania:once-cell-kotlin", version.ref = "once-cell" }
```

---

## Quick Start

### 1. Thread-Safe `sync.OnceCell`

`sync.OnceCell<T>` allows safe shared initialization across threads. If an initializer throws, the exception propagates to the caller and the cell remains uninitialized so a subsequent call can retry.

```kotlin
import io.github.kotlinmania.oncecell.sync.OnceCell
import io.github.kotlinmania.oncecell.sync.SetResult
import io.github.kotlinmania.oncecell.sync.TryInsertResult

val configCell = OnceCell.new<Map<Int, String>>()

// Non-blocking read returns null before initialization
check(configCell.get() == null)

// Initialize once; guaranteed that only one initializer succeeds
val map = configCell.getOrInit {
    mapOf(13 to "Spica", 74 to "Hoyten")
}
check(map[13] == "Spica")

// Subsequent writes are rejected without overwriting the existing value
val secondWrite = configCell.set(emptyMap())
check(secondWrite is SetResult.Err)

// tryInsert distinguishes freshly inserted vs. already-existing values
when (val outcome = configCell.tryInsert(emptyMap())) {
    is TryInsertResult.Inserted -> println("Installed: ${outcome.value}")
    is TryInsertResult.Existing -> println("Kept existing: ${outcome.current}")
}
```

### 2. Thread-Safe `sync.Lazy`

`sync.Lazy<T>` combines a `OnceCell<T>` with an initializer lambda, evaluating on first access via `.value` or `Lazy.force(lazy)`:

```kotlin
import io.github.kotlinmania.oncecell.sync.Lazy

val globalData: Lazy<Map<Int, String>> = Lazy.new {
    buildMap {
        put(13, "Spica")
        put(74, "Hoyten")
    }
}

fun main() {
    // Evaluated on first access; subsequent reads are lock-free loads
    println(globalData.value)
}
```

### 3. Blocking Wait Across Threads (`sync.OnceCell.wait`)

When one thread is responsible for setting a cell and other threads need the result, `wait()` blocks until initialization completes:

```kotlin
import io.github.kotlinmania.oncecell.sync.OnceCell

val cell = OnceCell.new<Int>()

// Producer sets the value
cell.set(92)

// Consumer awaits initialization and receives the non-null value directly
val answer: Int = cell.wait()
check(answer == 92)
```

### 4. Wait-Free Atomic Cells (`race`)

The `race` module provides wait-free cells built on single atomic compare-and-set operations. No thread ever blocks waiting on another thread:

```kotlin
import io.github.kotlinmania.oncecell.race.OnceBool
import io.github.kotlinmania.oncecell.race.OnceBox
import io.github.kotlinmania.oncecell.race.OnceNonZeroUsize

// Wait-free non-zero unsigned integer cell
val idCell = OnceNonZeroUsize.new()
val id: ULong = idCell.getOrInit { 42uL }
check(id == 42uL)

// Wait-free boolean cell
val flag = OnceBool.new()
check(flag.getOrInit { true })

// Wait-free heap-allocated reference cell
val boxCell = OnceBox.new<String>()
val greeting = boxCell.getOrInit { "Hello from OnceBox" }
check(greeting == "Hello from OnceBox")
```

### 5. Single-Threaded Cells (`unsync`)

When thread synchronization is unnecessary, `unsync.OnceCell` and `unsync.Lazy` eliminate atomic operations altogether and immediately detect illegal reentrant initialization:

```kotlin
import io.github.kotlinmania.oncecell.unsync.Lazy
import io.github.kotlinmania.oncecell.unsync.OnceCell

val cell = OnceCell.new<String>()
val value = cell.getOrInit { "initialized" }
check(value == "initialized")

val sum = Lazy.new { listOf(1, 2, 3).sum() }
check(sum.value == 6)
```

### 6. Swift 6 Export (`OnceCellLibrary`)

`once-cell-kotlin` ships first-class **Kotlin Swift Export** support for Apple platforms. In addition to generic Kotlin classes, strongly-typed facades (`StringOnceCell`, `IntOnceCell`, `StringLazy`, `IntLazy`) and SAM `fun interface` initializers bridge cleanly into Swift 6 without `Any?` erasure:

```swift
import OnceCell

let syncCell = sync.StringOnceCell()
_ = syncCell.set(newValue: "hello from Swift")
assert(syncCell.get() == "hello from Swift")

let flag = race.OnceBool.Companion.shared.new()
_ = flag.set(value: true)
assert(flag.get() == true)
```

---

## Multiplatform Target Matrix

Every commit and release of `once-cell-kotlin` is built and verified across the full KotlinMania target surface with `allWarningsAsErrors = true`:

| Platform Family | Targets |
| :--- | :--- |
| **JVM & Android** | `jvm`, `android` (`compileSdk 36`, `minSdk 24`) |
| **Apple (macOS / iOS / tvOS / watchOS)** | `macosArm64`, `iosArm64`, `iosSimulatorArm64`, `tvosArm64`, `tvosSimulatorArm64`, `watchosDeviceArm64`, `watchosSimulatorArm64` + **Swift 6 Export (`OnceCellLibrary`)** |
| **Linux Native** | `linuxX64`, `linuxArm64` |
| **Android Native** | `androidNativeX64`, `androidNativeArm64` |
| **Windows Native** | `mingwX64` |
| **JavaScript & WebAssembly** | `js` (Browser & Node.js), `wasmJs` (Browser & Node.js), `wasmWasi` (Node.js) |

---

## Building & Testing Locally

```bash
# Run the multiplatform test suite (including Swift Export SPM tests on macOS)
./gradlew test

# Run the full multiplatform build, static analysis (Detekt + Ktlint), and target link gate
./gradlew build
```

---

## Acknowledgments & License

### Special Thank You

A heartfelt thank you to **[Aleksey Kladov (`@matklad`)](https://github.com/matklad)** and all contributors to the original Rust [**`once_cell`**](https://github.com/matklad/once_cell) crate. Their elegant API design, lock-free state machine architecture, and comprehensive test suite inspired both Rust's standard library (`std::cell::OnceCell` / `std::sync::OnceLock`) and this Kotlin Multiplatform transliteration.

### License

`once-cell-kotlin` is dual-licensed under the same terms as the upstream project, at your option:

- **MIT License** — see [`LICENSE-MIT`](LICENSE-MIT) or [`LICENSE`](LICENSE)
- **Apache License, Version 2.0** — see [`LICENSE-APACHE`](LICENSE-APACHE)

**Original Rust `once_cell` work:** Copyright (c) 2018 Aleksey Kladov and `once_cell` contributors ([`matklad/once_cell`](https://github.com/matklad/once_cell)).  
**Kotlin Multiplatform port:** Copyright (c) 2026 Sydney Renee and The Solace Project ([`KotlinMania/once-cell-kotlin`](https://github.com/KotlinMania/once-cell-kotlin)).
