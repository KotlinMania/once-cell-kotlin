// port-lint: source imp_cs.rs
@file:OptIn(
    kotlin.experimental.ExperimentalObjCRefinement::class,
    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,
)

package io.github.kotlinmania.oncecell.imp.cs

import kotlin.concurrent.atomics.AtomicBoolean
import io.github.kotlinmania.oncecell.unsync.OnceCell as UnsyncOnceCell

internal class OnceCell<T : Any> private constructor(
    initial: T?,
) {
    private val initialized: AtomicBoolean = AtomicBoolean(initial != null)
    private val value: UnsyncOnceCell<T> =
        if (initial == null) {
            UnsyncOnceCell.new()
        } else {
            UnsyncOnceCell.withValue(initial)
        }

    public constructor() : this(null)

    public fun isInitialized(): Boolean = initialized.load()

    public fun initialize(f: () -> Result<T>): Result<Unit> =
        value.getOrTryInit { f() }.map {
            initialized.store(true)
        }

    public fun getUnchecked(): T {
        check(isInitialized())
        return value.get()!!
    }

    public fun getMut(): T? = value.getMut()

    public fun intoInner(): T? = value.intoInner()

    public companion object {
        public fun <T : Any> new(): OnceCell<T> = OnceCell()

        public fun <T : Any> withValue(value: T): OnceCell<T> = OnceCell(value)
    }
}
