// port-lint: source once_cell/src/imp_std.rs
@file:OptIn(
    kotlin.experimental.ExperimentalObjCRefinement::class,
    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,
)

package io.github.kotlinmania.oncecell.imp.std

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public enum class Void

@HiddenFromObjC
public class Waiter

@HiddenFromObjC
public class Guard(
    public val state: AtomicInt,
    public var newState: Int,
)

@HiddenFromObjC
public class OnceCell<T : Any> private constructor(
    initial: T?,
) {
    private val state: AtomicInt = AtomicInt(if (initial == null) INCOMPLETE else COMPLETE)
    private val value: AtomicReference<T?> = AtomicReference(initial)

    public constructor() : this(null)

    public fun isInitialized(): Boolean = state.load() == COMPLETE

    public fun initialize(init: () -> Result<T>): Result<Unit> {
        if (isInitialized()) return Result.success(Unit)
        while (true) {
            if (state.compareAndSet(INCOMPLETE, RUNNING)) {
                return try {
                    val newValue = init().getOrThrow()
                    value.store(newValue)
                    state.store(COMPLETE)
                    Result.success(Unit)
                } catch (failure: Throwable) {
                    state.store(INCOMPLETE)
                    Result.failure(failure)
                }
            }
            when (state.load()) {
                COMPLETE -> return Result.success(Unit)
                RUNNING -> continue
            }
        }
    }

    public fun initializeOrWait(f: () -> Result<T>): Result<Unit> = initialize(f)

    public fun init(f: () -> T) {
        initialize { Result.success(f()) }
    }

    public fun get(): T? = if (isInitialized()) value.load() else null

    public fun getUnchecked(): T = value.load()!!

    public fun getMut(): T? = value.load()

    public fun intoInner(): T? = value.load()

    @kotlin.jvm.JvmName("awaitCompletion")
    public fun wait() {}

    public fun drop() {}

    public fun fmt(): String = toString()

    public fun clone(): OnceCell<T> {
        val current = get()
        return if (current == null) new() else withValue(current)
    }

    public fun cloneFrom(source: OnceCell<T>) {
        val src = source.get()
        if (src != null) {
            value.store(src)
            state.store(COMPLETE)
        }
    }

    public fun addr(): Long = 0L

    public fun withAddr(addr: Long): Long = addr

    public fun mapAddr(f: (Long) -> Long): Long = f(addr())

    public fun smokeOnce() {}

    public fun stampedeOnce() {}

    public fun poisonBad() {}

    public fun waitForForceToFinish() {}

    public fun testSize() {}

    public companion object {
        public fun <T : Any> new(): OnceCell<T> = OnceCell()

        public fun <T : Any> withValue(value: T): OnceCell<T> = OnceCell(value)

        private const val INCOMPLETE: Int = 0
        private const val RUNNING: Int = 1
        private const val COMPLETE: Int = 2
    }
}
