package io.github.kotlinmania.oncecell.sync

import kotlin.concurrent.atomics.AtomicReference

/**
 * Initializer callback for thread-safe [Lazy].
 */
public fun interface LazyInitializer<out T : Any> {
    public operator fun invoke(): T
}

/**
 * A value that is initialized on the first access.
 *
 * This type is thread-safe and can be used for shared lazy values.
 */
// generic by design: arbitrary type container
public class Lazy<T : Any> private constructor(
    init: LazyInitializer<T>,
) {
    private val cell: OnceCell<T> = OnceCell.new()
    private val init: AtomicReference<LazyInitializer<T>?> = AtomicReference(init)

    public val value: T
        get() = force(this)

    public operator fun invoke(): T = force(this)

    public fun get(): T? = get(this)

    public fun deref(): T = value

    public fun derefMut(): T = forceMut(this)

    public fun fmt(): String = toString()

    override fun toString(): String = "Lazy(cell=$cell, init=..)"

    public companion object {
        public fun <T : Any> new(init: LazyInitializer<T>): Lazy<T> = Lazy(init)

        public fun <T : Any> default(init: LazyInitializer<T>): Lazy<T> = new(init)

        public fun <T : Any> force(thisLazy: Lazy<T>): T =
            thisLazy.cell.getOrInit {
                thisLazy.init.exchange(null)?.invoke()
                    ?: throw IllegalStateException("Lazy instance has previously been poisoned")
            }

        public fun <T : Any> forceMut(thisLazy: Lazy<T>): T = force(thisLazy)

        public fun <T : Any> get(thisLazy: Lazy<T>): T? = thisLazy.cell.get()

        public fun <T : Any> getMut(thisLazy: Lazy<T>): T? = thisLazy.cell.getMut()

        public fun <T : Any> intoValue(thisLazy: Lazy<T>): LazyValueResult<T> {
            val current = thisLazy.cell.get()
            if (current != null) {
                return LazyValueResult.Value(current)
            }
            val initializer =
                thisLazy.init.exchange(null)
                    ?: throw IllegalStateException("Lazy instance has previously been poisoned")
            return LazyValueResult.Initializer(initializer)
        }
    }
}

internal fun dummy() {}

public sealed class LazyValueResult<out T : Any> {
    public data class Value<T : Any>(
        public val value: T,
    ) : LazyValueResult<T>()

    public data class Initializer<T : Any>(
        public val initializer: LazyInitializer<T>,
    ) : LazyValueResult<T>()
}
