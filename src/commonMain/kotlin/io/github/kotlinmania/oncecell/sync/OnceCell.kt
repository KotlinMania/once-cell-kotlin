package io.github.kotlinmania.oncecell.sync

import io.github.kotlinmania.oncecell.imp.std.OnceCell as Imp

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
// generic by design: arbitrary type container
public open class OnceCell<T : Any> protected constructor(
    initial: T?,
) {
    private var inner: Imp<T> =
        if (initial == null) {
            Imp.new()
        } else {
            Imp.withValue(initial)
        }

    public constructor() : this(null)

    /**
     * Gets the reference to the underlying value.
     *
     * Returns `null` if the cell is empty or being initialized. This method
     * never blocks.
     */
    public fun get(): T? =
        if (inner.isInitialized()) {
            getUnchecked()
        } else {
            null
        }

    /**
     * Gets the reference to the underlying value, blocking until it is set.
     */
    public fun wait(): T {
        if (!inner.isInitialized()) {
            inner.wait()
        }
        check(inner.isInitialized())
        return getUnchecked()
    }

    /**
     * Gets the mutable reference to the underlying value if present.
     *
     * Mutating the value itself follows Kotlin reference semantics.
     */
    public fun getMut(): T? = inner.getMut()

    /**
     * Gets the reference to the underlying value without checking whether the
     * cell is initialized.
     */
    public fun getUnchecked(): T = inner.getUnchecked()

    /**
     * Sets the contents of this cell to [newValue].
     *
     * Returns [SetResult.Ok] if the cell was empty and [SetResult.Err] carrying
     * [newValue] if it was already full.
     */
    public fun set(newValue: T): SetResult<T> =
        when (val result = tryInsert(newValue)) {
            is TryInsertResult.Inserted -> SetResult.Ok
            is TryInsertResult.Existing -> SetResult.Err(result.value)
        }

    /**
     * Like [set], but also returns the final cell value.
     */
    public fun tryInsert(newValue: T): TryInsertResult<T> {
        var candidate: T? = newValue
        val res =
            getOrInit {
                val taken = candidate!!
                candidate = null
                taken
            }
        val leftover = candidate
        return if (leftover == null) {
            TryInsertResult.Inserted(res)
        } else {
            TryInsertResult.Existing(res, leftover)
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
    internal fun getOrTryInit(init: OnceCellTryInitializer<T>): Result<T> {
        get()?.let { return Result.success(it) }
        inner.initialize { init() }.onFailure { return Result.failure(it) }
        check(inner.isInitialized())
        return Result.success(getUnchecked())
    }

    /**
     * Takes the value out of this [OnceCell], moving it back to an
     * uninitialized state.
     *
     * Has no effect and returns `null` if the [OnceCell] has not been
     * initialized.
     */
    public fun take(): T? {
        val previous = inner
        inner = Imp.new()
        return previous.intoInner()
    }

    /**
     * Returns the initialized value, or `null` if the cell is empty.
     */
    public fun intoInner(): T? = inner.intoInner()

    public fun isInitialized(): Boolean = inner.isInitialized()

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

    public fun clone(): OnceCell<T> = copy()

    public fun cloneFrom(source: OnceCell<T>) {
        val src = source.get()
        inner =
            if (src == null) {
                Imp.new()
            } else {
                Imp.withValue(src)
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
    }
}

/**
 * Concrete result of [StringOnceCell.set].
 */
public class StringSetResult internal constructor(
    public val isOk: Boolean,
    public val error: String?,
) {
    public val isErr: Boolean get() = !isOk
}

/**
 * Concrete result of [StringOnceCell.tryInsert].
 */
public class StringTryInsertResult internal constructor(
    public val isInserted: Boolean,
    public val value: String,
    public val current: String?,
) {
    public val isExisting: Boolean get() = !isInserted
}

/**
 * Concrete result of [IntOnceCell.set].
 */
public class IntSetResult internal constructor(
    public val isOk: Boolean,
    public val error: Int?,
) {
    public val isErr: Boolean get() = !isOk
}

/**
 * Concrete result of [IntOnceCell.tryInsert].
 */
public class IntTryInsertResult internal constructor(
    public val isInserted: Boolean,
    public val value: Int,
    public val current: Int?,
) {
    public val isExisting: Boolean get() = !isInserted
}

/**
 * Strongly-typed thread-safe [OnceCell] holding a [String] value, optimized for Swift Export.
 */
public class StringOnceCell(
    initial: String? = null,
) {
    private val delegate: OnceCell<String> =
        if (initial == null) {
            OnceCell.new()
        } else {
            OnceCell.withValue(initial)
        }

    public constructor() : this(null)

    public fun get(): String? = delegate.get()

    public fun wait(): String = delegate.wait()

    public fun getMut(): String? = delegate.getMut()

    public fun getUnchecked(): String = delegate.getUnchecked()

    public fun set(newValue: String): StringSetResult =
        when (val result = delegate.set(newValue)) {
            is SetResult.Ok -> StringSetResult(isOk = true, error = null)
            is SetResult.Err -> StringSetResult(isOk = false, error = result.value)
        }

    public fun tryInsert(newValue: String): StringTryInsertResult =
        when (val result = delegate.tryInsert(newValue)) {
            is TryInsertResult.Inserted -> {
                StringTryInsertResult(isInserted = true, value = result.value, current = null)
            }

            is TryInsertResult.Existing -> {
                StringTryInsertResult(isInserted = false, value = result.value, current = result.current)
            }
        }

    public fun getOrInit(init: () -> String): String = delegate.getOrInit { init() }

    public fun take(): String? = delegate.take()

    public fun intoInner(): String? = delegate.intoInner()

    public fun isInitialized(): Boolean = delegate.isInitialized()

    public fun copy(): StringOnceCell = StringOnceCell(get())

    public fun clone(): StringOnceCell = copy()

    public fun cloneFrom(source: StringOnceCell) {
        delegate.cloneFrom(source.delegate)
    }

    public fun deref(): String? = delegate.deref()

    public fun derefMut(): String? = delegate.derefMut()

    public fun fmt(): String = delegate.fmt()

    public fun eq(other: Any?): Boolean = equals(other)

    override fun equals(other: Any?): Boolean =
        other is StringOnceCell && get() == other.get()

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    public companion object {
        public fun new(): StringOnceCell = StringOnceCell()

        public fun default(): StringOnceCell = new()

        public fun from(value: String): StringOnceCell = withValue(value)

        public fun withValue(value: String): StringOnceCell = StringOnceCell(value)
    }
}

/**
 * Strongly-typed thread-safe [OnceCell] holding an [Int] value, optimized for Swift Export.
 */
public class IntOnceCell(
    initial: Int? = null,
) {
    private val delegate: OnceCell<Int> =
        if (initial == null) {
            OnceCell.new()
        } else {
            OnceCell.withValue(initial)
        }

    public constructor() : this(null)

    public fun get(): Int? = delegate.get()

    public fun wait(): Int = delegate.wait()

    public fun getMut(): Int? = delegate.getMut()

    public fun getUnchecked(): Int = delegate.getUnchecked()

    public fun set(newValue: Int): IntSetResult =
        when (val result = delegate.set(newValue)) {
            is SetResult.Ok -> IntSetResult(isOk = true, error = null)
            is SetResult.Err -> IntSetResult(isOk = false, error = result.value)
        }

    public fun tryInsert(newValue: Int): IntTryInsertResult =
        when (val result = delegate.tryInsert(newValue)) {
            is TryInsertResult.Inserted -> {
                IntTryInsertResult(isInserted = true, value = result.value, current = null)
            }

            is TryInsertResult.Existing -> {
                IntTryInsertResult(isInserted = false, value = result.value, current = result.current)
            }
        }

    public fun getOrInit(init: () -> Int): Int = delegate.getOrInit { init() }

    public fun take(): Int? = delegate.take()

    public fun intoInner(): Int? = delegate.intoInner()

    public fun isInitialized(): Boolean = delegate.isInitialized()

    public fun copy(): IntOnceCell = IntOnceCell(get())

    public fun clone(): IntOnceCell = copy()

    public fun cloneFrom(source: IntOnceCell) {
        delegate.cloneFrom(source.delegate)
    }

    public fun deref(): Int? = delegate.deref()

    public fun derefMut(): Int? = delegate.derefMut()

    public fun fmt(): String = delegate.fmt()

    public fun eq(other: Any?): Boolean = equals(other)

    override fun equals(other: Any?): Boolean =
        other is IntOnceCell && get() == other.get()

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    public companion object {
        public fun new(): IntOnceCell = IntOnceCell()

        public fun default(): IntOnceCell = new()

        public fun from(value: Int): IntOnceCell = withValue(value)

        public fun withValue(value: Int): IntOnceCell = IntOnceCell(value)
    }
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
