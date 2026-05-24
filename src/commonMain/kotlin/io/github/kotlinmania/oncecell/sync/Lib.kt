// port-lint: source lib.rs
package io.github.kotlinmania.oncecell.sync

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference

/**
 * A thread-safe cell that can be written to only once.
 *
 * `OnceCell` provides direct references to its contents without RAII guards.
 * Reading an initialized value establishes the Kotlin atomic happens-before
 * relationship with the write that installed it.
 */
public class OnceCell<T : Any> private constructor(initial: T?) {
    private val state: AtomicInt = AtomicInt(if (initial == null) INCOMPLETE else COMPLETE)
    private val value: AtomicReference<T?> = AtomicReference(initial)

    public constructor() : this(null)

    /**
     * Gets the reference to the underlying value.
     *
     * Returns `null` if the cell is empty or being initialized. This method
     * never blocks.
     */
    public fun get(): T? =
        if (isInitialized()) {
            value.load()
        } else {
            null
        }

    /**
     * Gets the reference to the underlying value, blocking until it is set.
     */
    public fun wait(): T {
        while (!isInitialized()) {
            state.load()
        }
        return value.load()!!
    }

    /**
     * Gets the mutable reference to the underlying value if present.
     *
     * Mutating the value itself follows Kotlin reference semantics.
     */
    public fun getMut(): T? = get()

    /**
     * Gets the reference to the underlying value without checking whether the
     * cell is initialized.
     */
    public fun getUnchecked(): T = value.load()!!

    /**
     * Sets the contents of this cell to [newValue].
     *
     * Returns [SetResult.Ok] if the cell was empty and [SetResult.Err] carrying
     * [newValue] if it was already full.
     */
    public fun set(newValue: T): SetResult<T> =
        when (tryInsert(newValue)) {
            is TryInsertResult.Inserted -> SetResult.Ok
            is TryInsertResult.Existing -> SetResult.Err(newValue)
        }

    /**
     * Like [set], but also returns the final cell value.
     */
    public fun tryInsert(newValue: T): TryInsertResult<T> {
        get()?.let { old -> return TryInsertResult.Existing(old, newValue) }
        val installed = initialize { Result.success(newValue) }
        return if (installed === newValue) {
            TryInsertResult.Inserted(installed)
        } else {
            TryInsertResult.Existing(installed, newValue)
        }
    }

    /**
     * Gets the contents of the cell, initializing it with [init] if the cell
     * was empty.
     *
     * If [init] throws, the exception is propagated and the cell remains
     * uninitialized.
     */
    public fun getOrInit(init: () -> T): T = getOrTryInit { Result.success(init()) }.getOrThrow()

    /**
     * Gets the contents of the cell, initializing it with [init] if the cell
     * was empty. If the initializer fails, the error is returned and the cell
     * remains uninitialized.
     */
    public fun getOrTryInit(init: () -> Result<T>): Result<T> =
        try {
            Result.success(initialize(init))
        } catch (failure: Throwable) {
            Result.failure(failure)
        }

    /**
     * Returns the initialized value, or `null` if the cell is empty.
     */
    public fun intoInner(): T? = get()

    public fun isInitialized(): Boolean = state.load() == COMPLETE

    public fun copy(): OnceCell<T> {
        val current = get()
        return if (current == null) {
            new()
        } else {
            withValue(current)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is OnceCell<*> && get() == other.get()

    override fun hashCode(): Int = get()?.hashCode() ?: 0

    override fun toString(): String {
        val current = get()
        return if (current == null) {
            "OnceCell(Uninit)"
        } else {
            "OnceCell($current)"
        }
    }

    private fun initialize(init: () -> Result<T>): T {
        get()?.let { return it }
        while (true) {
            if (state.compareAndSet(INCOMPLETE, RUNNING)) {
                try {
                    val newValue = init().getOrThrow()
                    value.store(newValue)
                    state.store(COMPLETE)
                    return newValue
                } catch (failure: Throwable) {
                    state.store(INCOMPLETE)
                    throw failure
                }
            }
            when (state.load()) {
                COMPLETE -> return value.load()!!
                RUNNING -> continue
            }
        }
    }

    public companion object {
        public fun <T : Any> new(): OnceCell<T> = OnceCell()

        public fun <T : Any> withValue(value: T): OnceCell<T> = OnceCell(value)

        private const val INCOMPLETE: Int = 0
        private const val RUNNING: Int = 1
        private const val COMPLETE: Int = 2
    }
}

public sealed class SetResult<out T : Any> {
    public data object Ok : SetResult<Nothing>()

    public data class Err<T : Any>(public val value: T) : SetResult<T>()

    public val isOk: Boolean
        get() = this is Ok
}

public sealed class TryInsertResult<out T : Any> {
    public data class Inserted<T : Any>(public val value: T) : TryInsertResult<T>()

    public data class Existing<T : Any>(
        public val current: T,
        public val value: T,
    ) : TryInsertResult<T>()
}

/**
 * A value that is initialized on the first access.
 *
 * This type is thread-safe and can be used for shared lazy values.
 */
public class Lazy<T : Any> private constructor(init: () -> T) {
    private val cell: OnceCell<T> = OnceCell.new()
    private val init: AtomicReference<(() -> T)?> = AtomicReference(init)

    public val value: T
        get() = force(this)

    public operator fun invoke(): T = force(this)

    public fun get(): T? = get(this)

    override fun toString(): String = "Lazy(cell=$cell, init=..)"

    public companion object {
        public fun <T : Any> new(init: () -> T): Lazy<T> = Lazy(init)

        public fun <T : Any> force(thisLazy: Lazy<T>): T =
            thisLazy.cell.getOrInit {
                thisLazy.init.exchange(null)?.invoke()
                    ?: throw IllegalStateException("Lazy instance has previously been poisoned")
            }

        public fun <T : Any> get(thisLazy: Lazy<T>): T? = thisLazy.cell.get()

        public fun <T : Any> intoValue(thisLazy: Lazy<T>): LazyValueResult<T> {
            val current = thisLazy.cell.get()
            if (current != null) {
                return LazyValueResult.Value(current)
            }
            val initializer = thisLazy.init.exchange(null)
                ?: throw IllegalStateException("Lazy instance has previously been poisoned")
            return LazyValueResult.Initializer(initializer)
        }
    }
}

public sealed class LazyValueResult<out T : Any> {
    public data class Value<T : Any>(public val value: T) : LazyValueResult<T>()

    public data class Initializer<T : Any>(public val initializer: () -> T) : LazyValueResult<T>()
}
