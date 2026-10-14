// port-lint: source lib.rs
package io.github.kotlinmania.oncecell.unsync

/**
 * Initializer callback for [OnceCell.getOrInit].
 */
public fun interface OnceCellInitializer<out T : Any> {
    public operator fun invoke(): T
}

/**
 * Initializer callback returning a [Result] for [OnceCell.getOrTryInit].
 */
internal fun interface OnceCellTryInitializer<out T : Any> {
    public operator fun invoke(): Result<T>
}

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
// generic by design: arbitrary type container
public open class OnceCell<T : Any> protected constructor(
    initial: T?,
) {
    public constructor() : this(null)

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
    public fun getOrInit(f: OnceCellInitializer<T>): T = getOrTryInit { Result.success(f()) }.getOrThrow()

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
    internal fun getOrTryInit(f: OnceCellTryInitializer<T>): Result<T> {
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

    public fun clone(): OnceCell<T> = copy()

    public fun cloneFrom(source: OnceCell<T>) {
        inner = source.get()
    }

    public fun deref(): T? = get()

    public fun derefMut(): T? = getMut()

    public fun fmt(): String = toString()

    public fun eq(other: Any?): Boolean = equals(other)

    public companion object {
        /** Creates a new empty cell. */
        public fun <T : Any> new(): OnceCell<T> = OnceCell(null)

        public fun <T : Any> default(): OnceCell<T> = new()

        public fun <T : Any> from(value: T): OnceCell<T> = withValue(value)

        /** Creates a new initialized cell. */
        public fun <T : Any> withValue(value: T): OnceCell<T> = OnceCell(value)
    }
}

/**
 * Strongly-typed [OnceCell] holding a [String] value, optimized for Swift Export.
 */
public class StringOnceCell(
    initial: String? = null,
) : OnceCell<String>(initial) {
    public constructor() : this(null)
}

/**
 * Strongly-typed [OnceCell] holding an [Int] value, optimized for Swift Export.
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
