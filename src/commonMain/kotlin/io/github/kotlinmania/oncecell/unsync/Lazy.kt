// port-lint: source lib.rs
package io.github.kotlinmania.oncecell.unsync

/**
 * Initializer callback for [Lazy].
 */
public fun interface LazyInitializer<out T : Any> {
    public operator fun invoke(): T
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
// generic by design: arbitrary type container matching upstream once_cell::unsync::Lazy
public class Lazy<T : Any> private constructor(
    init: LazyInitializer<T>,
) {
    private val cell: OnceCell<T> = OnceCell.new()
    private var init: LazyInitializer<T>? = init

    /** The value of this lazy, computed on first read. */
    public val value: T
        get() = force(this)

    public operator fun invoke(): T = force(this)

    /** Returns the result of this lazy value if it was initialized, otherwise `null`. */
    public fun get(): T? = get(this)

    override fun toString(): String = "Lazy(cell=$cell, init=..)"

    public companion object {
        /** Creates a new lazy value with the given initializing function. */
        public fun <T : Any> new(init: LazyInitializer<T>): Lazy<T> = Lazy(init)

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
            val initializer =
                thisLazy.init
                    ?: throw IllegalStateException("Lazy instance has previously been poisoned")
            thisLazy.init = null
            return LazyValueResult.Initializer(initializer)
        }
    }
}

public sealed class LazyValueResult<out T : Any> {
    public data class Value<T : Any>(
        public val value: T,
    ) : LazyValueResult<T>()

    public data class Initializer<T : Any>(
        public val initializer: LazyInitializer<T>,
    ) : LazyValueResult<T>()
}
