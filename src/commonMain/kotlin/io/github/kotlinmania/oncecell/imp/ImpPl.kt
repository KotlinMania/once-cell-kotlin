// port-lint: source imp_pl.rs
@file:OptIn(
    kotlin.experimental.ExperimentalObjCRefinement::class,
    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,
)

package io.github.kotlinmania.oncecell.imp.pl

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference

internal class Guard(
    internal val state: AtomicInt,
    internal var newState: Int,
) {
    public fun drop() {
        state.store(newState)
    }
}

internal fun initializeInner(
    state: AtomicInt,
    init: () -> Boolean,
) {
    while (true) {
        if (state.compareAndSet(OnceCell.INCOMPLETE, OnceCell.RUNNING)) {
            val guard = Guard(state, OnceCell.INCOMPLETE)
            try {
                if (init()) {
                    guard.newState = OnceCell.COMPLETE
                }
            } finally {
                guard.drop()
            }
            return
        }
        when (state.load()) {
            OnceCell.COMPLETE -> {
                return
            }

            OnceCell.RUNNING -> {
                while (state.load() == OnceCell.RUNNING) {
                    // Wait while running
                }
            }

            OnceCell.INCOMPLETE -> {
                continue
            }

            else -> {
                check(false)
            }
        }
    }
}

internal class OnceCell<T : Any> private constructor(
    initial: T?,
) {
    private val state: AtomicInt = AtomicInt(if (initial == null) INCOMPLETE else COMPLETE)
    private val value: AtomicReference<T?> = AtomicReference(initial)

    public constructor() : this(null)

    public fun isInitialized(): Boolean = state.load() == COMPLETE

    public fun initialize(f: () -> Result<T>): Result<Unit> {
        var fOpt: (() -> Result<T>)? = f
        var res: Result<Unit> = Result.success(Unit)
        initializeInner(state) {
            val initFn = fOpt!!
            fOpt = null
            initFn().fold(
                onSuccess = { newValue ->
                    check(value.load() == null)
                    value.store(newValue)
                    true
                },
                onFailure = { err ->
                    res = Result.failure(err)
                    false
                },
            )
        }
        return res
    }

    public fun initializeInner(init: () -> Boolean) {
        initializeInner(state, init)
    }

    @kotlin.jvm.JvmName("awaitCompletion")
    public fun wait() {
        while (state.load() != COMPLETE) {
            // Wait until complete
        }
    }

    public fun getUnchecked(): T {
        check(isInitialized())
        return value.load()!!
    }

    public fun getMut(): T? = if (isInitialized()) value.load() else null

    public fun intoInner(): T? = if (isInitialized()) value.exchange(null) else null

    public fun drop() {}

    public fun testSize() {}

    public companion object {
        public fun <T : Any> new(): OnceCell<T> = OnceCell()

        public fun <T : Any> withValue(value: T): OnceCell<T> = OnceCell(value)

        internal const val INCOMPLETE: Int = 0
        internal const val RUNNING: Int = 1
        internal const val COMPLETE: Int = 2
    }
}
