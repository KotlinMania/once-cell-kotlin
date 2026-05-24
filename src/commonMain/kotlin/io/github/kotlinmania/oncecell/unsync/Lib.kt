// port-lint: source lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oncecell.unsync

import kotlin.native.HiddenFromObjC

/**
 * A cell which can be written to only once. It is not thread safe.
 *
 * Unlike a Kotlin `Lazy` from the standard library, an [OnceCell] separates
 * the act of constructing the cell from the act of installing the value, and
 * exposes the value through plain reads rather than a delegate. Reads after
 * the cell is initialized always return the same reference.
 *
 * # Example
 * ```
 * val cell = OnceCell.new<String>()
 * check(cell.get() == null)
 *
 * val value = cell.getOrInit { "Hello, World!" }
 * check(value == "Hello, World!")
 * check(cell.get() != null)
 * ```
 */
@HiddenFromObjC
public class OnceCell<T : Any> private constructor(initial: T?) {
    private var inner: T? = initial

    /** Gets a reference to the underlying value. Returns `null` if the cell is empty. */
    public fun get(): T? = inner

    /** Gets a mutable reference to the underlying value. Returns `null` if the cell is empty. */
    public fun getMut(): T? = inner

    /**
     * Sets the contents of this cell to [value].
     *
     * Returns [SetResult.Ok] if the cell was empty and [SetResult.Err] carrying
     * [value] if it was already full.
     *
     * # Example
     * ```
     * val cell = OnceCell.new<Int>()
     * check(cell.get() == null)
     *
     * check(cell.set(92) is SetResult.Ok)
     * check(cell.set(62) is SetResult.Err)
     *
     * check(cell.get() != null)
     * ```
     */
    public fun set(value: T): SetResult<T> =
        when (val outcome = tryInsert(value)) {
            is TryInsertResult.Inserted -> SetResult.Ok
            is TryInsertResult.Existing -> SetResult.Err(outcome.value)
        }

    /**
     * Like [set], but also returns a reference to the final cell value.
     *
     * # Example
     * ```
     * val cell = OnceCell.new<Int>()
     * check(cell.get() == null)
     *
     * check(cell.tryInsert(92) is TryInsertResult.Inserted)
     * check(cell.tryInsert(62) is TryInsertResult.Existing)
     *
     * check(cell.get() != null)
     * ```
     */
    public fun tryInsert(value: T): TryInsertResult<T> {
        get()?.let { old -> return TryInsertResult.Existing(old, value) }
        // This is the only place where we set the slot, no races
        // due to reentrancy/concurrency are possible, and we've
        // checked that slot is currently empty, so this write
        // maintains the inner's invariant.
        inner = value
        return TryInsertResult.Inserted(value)
    }

    /**
     * Gets the contents of the cell, initializing it with [f] if the cell was
     * empty.
     *
     * # Panics
     *
     * If [f] throws, the exception is propagated to the caller, and the cell
     * remains uninitialized.
     *
     * It is an error to reentrantly initialize the cell from [f]. Doing so
     * results in a panic.
     *
     * # Example
     * ```
     * val cell = OnceCell.new<Int>()
     * val value = cell.getOrInit { 92 }
     * check(value == 92)
     * val again = cell.getOrInit { error("unreachable") }
     * check(again == 92)
     * ```
     */
    public fun getOrInit(f: () -> T): T = getOrTryInit { Result.success(f()) }.getOrThrow()

    /**
     * Gets the contents of the cell, initializing it with [f] if the cell was
     * empty. If the cell was empty and [f] failed, the failure is returned.
     *
     * # Panics
     *
     * If [f] throws, the exception is propagated to the caller, and the cell
     * remains uninitialized.
     *
     * It is an error to reentrantly initialize the cell from [f]. Doing so
     * results in a panic.
     */
    public fun getOrTryInit(f: () -> Result<T>): Result<T> {
        get()?.let { return Result.success(it) }
        val produced = f()
        val value = produced.getOrElse { return produced }
        // Note that *some* forms of reentrant initialization might lead to
        // surprising aliasing; the upstream comment recommends panicking
        // rather than silently using an old value.
        check(set(value) is SetResult.Ok) { "reentrant init" }
        return Result.success(get()!!)
    }

    /**
     * Takes the value out of this [OnceCell], moving it back to an
     * uninitialized state.
     *
     * Has no effect and returns `null` if the [OnceCell] has not been
     * initialized.
     */
    public fun take(): T? {
        val current = inner
        inner = null
        return current
    }

    /**
     * Consumes the [OnceCell], returning the wrapped value. Returns `null` if
     * the cell was empty.
     */
    public fun intoInner(): T? = inner

    public fun copy(): OnceCell<T> {
        val current = get()
        return if (current == null) new() else withValue(current)
    }

    override fun equals(other: Any?): Boolean =
        other is OnceCell<*> && get() == other.get()

    override fun hashCode(): Int = get()?.hashCode() ?: 0

    override fun toString(): String {
        val current = get()
        return if (current == null) "OnceCell(Uninit)" else "OnceCell($current)"
    }

    public companion object {
        /** Creates a new empty cell. */
        public fun <T : Any> new(): OnceCell<T> = OnceCell(null)

        /** Creates a new initialized cell. */
        public fun <T : Any> withValue(value: T): OnceCell<T> = OnceCell(value)
    }
}

@HiddenFromObjC
public sealed class SetResult<out T : Any> {
    public data object Ok : SetResult<Nothing>()

    public data class Err<T : Any>(public val value: T) : SetResult<T>()

    public val isOk: Boolean
        get() = this is Ok
}

@HiddenFromObjC
public sealed class TryInsertResult<out T : Any> {
    public data class Inserted<T : Any>(public val value: T) : TryInsertResult<T>()

    public data class Existing<T : Any>(
        public val current: T,
        public val value: T,
    ) : TryInsertResult<T>()
}

/**
 * A value which is initialized on the first access.
 *
 * # Example
 * ```
 * val lazy: Lazy<Int> = Lazy.new {
 *     println("initializing")
 *     92
 * }
 * println("ready")
 * println(lazy.value)
 * println(lazy.value)
 *
 * // Prints:
 * //   ready
 * //   initializing
 * //   92
 * //   92
 * ```
 */
@HiddenFromObjC
public class Lazy<T : Any> private constructor(init: () -> T) {
    private val cell: OnceCell<T> = OnceCell.new()
    private var init: (() -> T)? = init

    /** The value of this lazy, computed on first read. */
    public val value: T
        get() = force(this)

    public operator fun invoke(): T = force(this)

    /** Returns the result of this lazy value if it was initialized, otherwise `null`. */
    public fun get(): T? = get(this)

    override fun toString(): String = "Lazy(cell=$cell, init=..)"

    public companion object {
        /** Creates a new lazy value with the given initializing function. */
        public fun <T : Any> new(init: () -> T): Lazy<T> = Lazy(init)

        /**
         * Forces the evaluation of this lazy value and returns a reference to
         * the result.
         */
        public fun <T : Any> force(thisLazy: Lazy<T>): T =
            thisLazy.cell.getOrInit {
                val initializer = thisLazy.init
                thisLazy.init = null
                initializer?.invoke()
                    ?: throw IllegalStateException("Lazy instance has previously been poisoned")
            }

        /**
         * Forces the evaluation of this lazy value and returns a mutable
         * reference to the result.
         */
        public fun <T : Any> forceMut(thisLazy: Lazy<T>): T = force(thisLazy)

        /** Gets the result of this lazy value if it was initialized, otherwise `null`. */
        public fun <T : Any> get(thisLazy: Lazy<T>): T? = thisLazy.cell.get()

        /** Gets the mutable result of this lazy value if it was initialized, otherwise `null`. */
        public fun <T : Any> getMut(thisLazy: Lazy<T>): T? = thisLazy.cell.getMut()

        /**
         * Consumes this [Lazy] returning the stored value.
         *
         * Returns [LazyValueResult.Value] if the [Lazy] is initialized and
         * [LazyValueResult.Initializer] otherwise.
         */
        public fun <T : Any> intoValue(thisLazy: Lazy<T>): LazyValueResult<T> {
            val current = thisLazy.cell.intoInner()
            if (current != null) {
                return LazyValueResult.Value(current)
            }
            val initializer = thisLazy.init
                ?: throw IllegalStateException("Lazy instance has previously been poisoned")
            thisLazy.init = null
            return LazyValueResult.Initializer(initializer)
        }
    }
}

@HiddenFromObjC
public sealed class LazyValueResult<out T : Any> {
    public data class Value<T : Any>(public val value: T) : LazyValueResult<T>()

    public data class Initializer<T : Any>(public val initializer: () -> T) : LazyValueResult<T>()
}
