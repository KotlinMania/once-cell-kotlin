// port-lint: source lib.rs
package io.github.kotlinmania.oncecell.sync

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference

public enum class Void

public interface Target

/**
 * Initializer callback for thread-safe [OnceCell.getOrInit].
 */
public fun interface OnceCellInitializer<out T : Any> {
    public operator fun invoke(): T
}

/**
 * Initializer callback returning a [Result] for thread-safe [OnceCell.getOrTryInit].
 */
internal fun interface OnceCellTryInitializer<out T : Any> {
    public operator fun invoke(): Result<T>
}

/**
 * A thread-safe cell that can be written to only once.
 *
 * `OnceCell` provides direct references to its contents without RAII guards.
 * Reading an initialized value establishes the Kotlin atomic happens-before
 * relationship with the write that installed it.
 */
// generic by design: arbitrary type container matching upstream once_cell::sync::OnceCell
public open class OnceCell<T : Any> protected constructor(
    initial: T?,
) {
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
    public fun getOrInit(init: OnceCellInitializer<T>): T = getOrTryInit { Result.success(init()) }.getOrThrow()

    /**
     * Gets the contents of the cell, initializing it with [init] if the cell
     * was empty. If the initializer fails, the error is returned and the cell
     * remains uninitialized.
     */
    internal fun getOrTryInit(init: OnceCellTryInitializer<T>): Result<T> =
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

    private fun initialize(init: OnceCellTryInitializer<T>): T {
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

    public fun clone(): OnceCell<T> = copy()

    public fun cloneFrom(source: OnceCell<T>) {
        val src = source.get()
        if (src != null) {
            set(src)
        }
    }

    public fun deref(): T? = get()

    public fun derefMut(): T? = getMut()

    public fun fmt(): String = toString()

    public fun eq(other: Any?): Boolean = equals(other)

    public companion object {
        public fun <T : Any> new(): OnceCell<T> = OnceCell()

        public fun <T : Any> default(): OnceCell<T> = new()

        public fun <T : Any> from(value: T): OnceCell<T> = withValue(value)

        public fun <T : Any> withValue(value: T): OnceCell<T> = OnceCell(value)

        private const val INCOMPLETE: Int = 0
        private const val RUNNING: Int = 1
        private const val COMPLETE: Int = 2
    }
}

/**
 * Strongly-typed thread-safe [OnceCell] holding a [String] value, optimized for Swift Export.
 */
public class StringOnceCell(
    initial: String? = null,
) : OnceCell<String>(initial) {
    public constructor() : this(null)
}

/**
 * Strongly-typed thread-safe [OnceCell] holding an [Int] value, optimized for Swift Export.
 */
public class IntOnceCell(
    initial: Int? = null,
) : OnceCell<Int>(initial) {
    public constructor() : this(null)
}

public sealed class SetResult<out T : Any> {
    public abstract val isOk: Boolean
    public val isErr: Boolean get() = !isOk
    public open val error: T? get() = null

    public data object Ok : SetResult<Nothing>() {
        override val isOk: Boolean get() = true
    }

    public data class Err<T : Any>(
        public val value: T,
    ) : SetResult<T>() {
        override val isOk: Boolean get() = false
        override val error: T get() = value
    }
}

public sealed class TryInsertResult<out T : Any> {
    public abstract val isInserted: Boolean
    public val isExisting: Boolean get() = !isInserted
    public open val value: T? get() = null
    public open val current: T? get() = null

    public data class Inserted<T : Any>(
        override val value: T,
    ) : TryInsertResult<T>() {
        override val isInserted: Boolean get() = true
    }

    public data class Existing<T : Any>(
        override val current: T,
        override val value: T,
    ) : TryInsertResult<T>() {
        override val isInserted: Boolean get() = false
    }
}
