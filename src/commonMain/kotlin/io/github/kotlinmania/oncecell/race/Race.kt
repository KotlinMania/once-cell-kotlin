// port-lint: source once_cell/src/race.rs
@file:OptIn(
    kotlin.experimental.ExperimentalObjCRefinement::class,
    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,
    kotlin.ExperimentalUnsignedTypes::class,
)

package io.github.kotlinmania.oncecell.race

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.native.HiddenFromObjC

/**
 * An empty type representing an uninhabited type.
 */
@HiddenFromObjC
public enum class Void

/**
 * A thread-safe cell which can be written to only once.
 *
 * Stores non-zero unsigned integer values where 0 represents an uninitialized cell.
 */
@HiddenFromObjC
public class OnceNonZeroUsize private constructor(
    initial: ULong?,
) {
    private val inner: AtomicLong = AtomicLong(initial?.toLong() ?: 0L)

    public constructor() : this(null)

    /**
     * Gets the underlying value.
     */
    public fun get(): ULong? {
        val v = inner.load()
        return if (v == 0L) null else v.toULong()
    }

    /**
     * Get the reference to the underlying value, without checking if the cell
     * is initialized.
     */
    public fun getUnchecked(): ULong = inner.load().toULong()

    /**
     * Sets the contents of this cell to [value].
     *
     * Returns `SetResult.Ok` if the cell was empty and `SetResult.Err` carrying
     * [value] if it was full.
     */
    public fun set(value: ULong): SetResult<ULong> {
        require(value > 0uL) { "OnceNonZeroUsize value must be non-zero" }
        return when (val outcome = compareExchange(value)) {
            is CompareExchangeResult.Success -> SetResult.Ok
            is CompareExchangeResult.Failure -> SetResult.Err(value)
        }
    }

    /**
     * Gets the contents of the cell, initializing it with [f] if the cell was
     * empty.
     *
     * If several threads concurrently run `getOrInit`, more than one `f` can
     * be called. However, all threads will return the same value, produced by
     * some `f`.
     */
    public fun getOrInit(f: () -> ULong): ULong =
        getOrTryInit { Result.success(f()) }.getOrThrow()

    /**
     * Gets the contents of the cell, initializing it with [f] if
     * the cell was empty. If the cell was empty and [f] failed, an
     * error is returned.
     *
     * If several threads concurrently run `getOrInit`, more than one `f` can
     * be called. However, all threads will return the same value, produced by
     * some `f`.
     */
    public fun getOrTryInit(f: () -> Result<ULong>): Result<ULong> {
        val existing = get()
        if (existing != null) {
            return Result.success(existing)
        }
        return init(f)
    }

    private fun init(f: () -> Result<ULong>): Result<ULong> {
        val produced = f()
        if (produced.isFailure) return produced
        val nz = produced.getOrThrow()
        require(nz > 0uL) { "OnceNonZeroUsize value must be non-zero" }
        var resultVal = nz
        val exchange = compareExchange(nz)
        if (exchange is CompareExchangeResult.Failure) {
            resultVal = exchange.old.toULong()
        }
        return Result.success(resultVal)
    }

    private fun compareExchange(valParam: ULong): CompareExchangeResult {
        val raw = valParam.toLong()
        return if (inner.compareAndSet(0L, raw)) {
            CompareExchangeResult.Success
        } else {
            CompareExchangeResult.Failure(inner.load())
        }
    }

    public fun asConstPtr(): ULong? = get()

    public fun fmt(): String = toString()

    public fun drop() {}

    override fun equals(other: Any?): Boolean =
        other is OnceNonZeroUsize && get() == other.get()

    override fun hashCode(): Int = get()?.hashCode() ?: 0

    override fun toString(): String {
        val current = get()
        return if (current == null) "OnceNonZeroUsize(Uninit)" else "OnceNonZeroUsize($current)"
    }

    public companion object {
        /**
         * Creates a new empty cell.
         */
        public fun new(): OnceNonZeroUsize = OnceNonZeroUsize()

        public fun default(): OnceNonZeroUsize = new()

        /**
         * Creates a new initialized cell.
         */
        public fun withValue(value: ULong): OnceNonZeroUsize {
            require(value > 0uL) { "OnceNonZeroUsize value must be non-zero" }
            return OnceNonZeroUsize(value)
        }
    }
}

/**
 * A thread-safe cell which can be written to only once.
 */
@HiddenFromObjC
public class OnceBool private constructor(
    private val inner: OnceNonZeroUsize,
) {
    public constructor() : this(OnceNonZeroUsize.new())

    /**
     * Gets the underlying value.
     */
    public fun get(): Boolean? = inner.get()?.let { fromUsize(it) }

    /**
     * Sets the contents of this cell to [value].
     *
     * Returns `SetResult.Ok` if the cell was empty and `SetResult.Err` carrying
     * [value] if it was full.
     */
    public fun set(value: Boolean): SetResult<Boolean> =
        when (inner.set(toUsize(value))) {
            is SetResult.Ok -> SetResult.Ok
            is SetResult.Err -> SetResult.Err(value)
        }

    /**
     * Gets the contents of the cell, initializing it with [f] if the cell was
     * empty.
     *
     * If several threads concurrently run `getOrInit`, more than one `f` can
     * be called. However, all threads will return the same value, produced by
     * some `f`.
     */
    public fun getOrInit(f: () -> Boolean): Boolean =
        fromUsize(inner.getOrInit { toUsize(f()) })

    /**
     * Gets the contents of the cell, initializing it with [f] if
     * the cell was empty. If the cell was empty and [f] failed, an
     * error is returned.
     *
     * If several threads concurrently run `getOrInit`, more than one `f` can
     * be called. However, all threads will return the same value, produced by
     * some `f`.
     */
    public fun getOrTryInit(f: () -> Result<Boolean>): Result<Boolean> =
        inner.getOrTryInit { f().map { toUsize(it) } }.map { fromUsize(it) }

    public fun fmt(): String = toString()

    public fun drop() {}

    override fun equals(other: Any?): Boolean =
        other is OnceBool && get() == other.get()

    override fun hashCode(): Int = get()?.hashCode() ?: 0

    override fun toString(): String {
        val current = get()
        return if (current == null) "OnceBool(Uninit)" else "OnceBool($current)"
    }

    public companion object {
        /**
         * Creates a new empty cell.
         */
        public fun new(): OnceBool = OnceBool()

        public fun default(): OnceBool = new()

        public fun fromUsize(value: ULong): Boolean = value == 1uL

        public fun toUsize(value: Boolean): ULong = if (value) 1uL else 2uL
    }
}

/**
 * A thread-safe cell which can be written to only once.
 */
@HiddenFromObjC
public class OnceRef<T : Any> private constructor(
    initial: T?,
) {
    private val inner: AtomicReference<T?> = AtomicReference(initial)

    public constructor() : this(null)

    /**
     * Gets a reference to the underlying value.
     */
    public fun get(): T? = inner.load()

    /**
     * Sets the contents of this cell to [value].
     *
     * Returns `SetResult.Ok` if the cell was empty and `SetResult.Err` carrying
     * [value] if it was full.
     */
    public fun set(value: T): SetResult<T> =
        if (compareExchange(value)) {
            SetResult.Ok
        } else {
            SetResult.Err(value)
        }

    /**
     * Gets the contents of the cell, initializing it with [f] if the cell was
     * empty.
     *
     * If several threads concurrently run `getOrInit`, more than one `f` can
     * be called. However, all threads will return the same value, produced by
     * some `f`.
     */
    public fun getOrInit(f: () -> T): T =
        getOrTryInit { Result.success(f()) }.getOrThrow()

    /**
     * Gets the contents of the cell, initializing it with [f] if
     * the cell was empty. If the cell was empty and [f] failed, an
     * error is returned.
     *
     * If several threads concurrently run `getOrInit`, more than one `f` can
     * be called. However, all threads will return the same value, produced by
     * some `f`.
     */
    public fun getOrTryInit(f: () -> Result<T>): Result<T> {
        val existing = get()
        if (existing != null) {
            return Result.success(existing)
        }
        return init(f)
    }

    private fun init(f: () -> Result<T>): Result<T> {
        val produced = f()
        if (produced.isFailure) return produced
        val value = produced.getOrThrow()
        if (!compareExchange(value)) {
            return Result.success(inner.load()!!)
        }
        return Result.success(value)
    }

    private fun compareExchange(value: T): Boolean =
        inner.compareAndSet(null, value)

    public fun fmt(): String = toString()

    public fun drop() {}

    private fun dummy() {}

    override fun equals(other: Any?): Boolean =
        other is OnceRef<*> && get() == other.get()

    override fun hashCode(): Int = get()?.hashCode() ?: 0

    override fun toString(): String {
        val current = get()
        return if (current == null) "OnceRef(Uninit)" else "OnceRef($current)"
    }

    public companion object {
        /**
         * Creates a new empty cell.
         */
        public fun <T : Any> new(): OnceRef<T> = OnceRef()

        public fun <T : Any> default(): OnceRef<T> = new()
    }
}

/**
 * A thread-safe cell which can be written to only once.
 */
@HiddenFromObjC
public class OnceBox<T : Any> private constructor(
    initial: T?,
) {
    private val inner: AtomicReference<T?> = AtomicReference(initial)

    public constructor() : this(null)

    /**
     * Gets a reference to the underlying value.
     */
    public fun get(): T? = inner.load()

    /**
     * Sets the contents of this cell to [value].
     *
     * Returns `SetResult.Ok` if the cell was empty and `SetResult.Err` carrying
     * [value] if it was full.
     */
    public fun set(value: T): SetResult<T> =
        if (inner.compareAndSet(null, value)) {
            SetResult.Ok
        } else {
            SetResult.Err(value)
        }

    /**
     * Gets the contents of the cell, initializing it with [f] if the cell was
     * empty.
     *
     * If several threads concurrently run `getOrInit`, more than one `f` can
     * be called. However, all threads will return the same value, produced by
     * some `f`.
     */
    public fun getOrInit(f: () -> T): T =
        getOrTryInit { Result.success(f()) }.getOrThrow()

    /**
     * Gets the contents of the cell, initializing it with [f] if
     * the cell was empty. If the cell was empty and [f] failed, an
     * error is returned.
     *
     * If several threads concurrently run `getOrInit`, more than one `f` can
     * be called. However, all threads will return the same value, produced by
     * some `f`.
     */
    public fun getOrTryInit(f: () -> Result<T>): Result<T> {
        val existing = get()
        if (existing != null) {
            return Result.success(existing)
        }
        return init(f)
    }

    private fun init(f: () -> Result<T>): Result<T> {
        val produced = f()
        if (produced.isFailure) return produced
        val value = produced.getOrThrow()
        if (!inner.compareAndSet(null, value)) {
            return Result.success(inner.load()!!)
        }
        return Result.success(value)
    }

    public fun copy(): OnceBox<T> {
        val current = get()
        return if (current != null) withValue(current) else new()
    }

    public fun clone(): OnceBox<T> = copy()

    public fun fmt(): String = toString()

    public fun drop() {}

    private fun dummy() {}

    override fun equals(other: Any?): Boolean =
        other is OnceBox<*> && get() == other.get()

    override fun hashCode(): Int = get()?.hashCode() ?: 0

    override fun toString(): String {
        val current = get()
        return if (current == null) "OnceBox(Uninit)" else "OnceBox($current)"
    }

    public companion object {
        /**
         * Creates a new empty cell.
         */
        public fun <T : Any> new(): OnceBox<T> = OnceBox()

        public fun <T : Any> default(): OnceBox<T> = new()

        /**
         * Creates a new cell with the given value.
         */
        public fun <T : Any> withValue(value: T): OnceBox<T> = OnceBox(value)
    }
}

@HiddenFromObjC
public sealed class SetResult<out T : Any> {
    public data object Ok : SetResult<Nothing>()

    public data class Err<T : Any>(
        public val value: T,
    ) : SetResult<T>()

    public val isOk: Boolean
        get() = this is Ok

    public val isErr: Boolean
        get() = this is Err
}

private sealed class CompareExchangeResult {
    data object Success : CompareExchangeResult()
    data class Failure(val old: Long) : CompareExchangeResult()
}
