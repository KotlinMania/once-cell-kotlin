// port-lint: source imp_std.rs
@file:OptIn(
    kotlin.experimental.ExperimentalObjCRefinement::class,
    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,
)

package io.github.kotlinmania.oncecell.imp.std

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference

internal enum class Void

internal class Waiter

internal const val INCOMPLETE: Int = 0x0
internal const val RUNNING: Int = 0x1
internal const val COMPLETE: Int = 0x2
internal const val STATE_MASK: Int = 0x3

internal fun addr(ptr: Long): Long = ptr

internal fun withAddr(
    ptr: Long,
    addr: Long,
): Long {
    val selfAddr = addr(ptr)
    val offset = addr - selfAddr
    return ptr + offset
}

internal fun mapAddr(
    ptr: Long,
    f: (Long) -> Long,
): Long = withAddr(ptr, f(addr(ptr)))

internal class Guard(
    internal val queue: AtomicInt,
    internal var newQueue: Int,
) {
    public fun drop() {
        val prevQueue = queue.exchange(newQueue)
        val state = addr(prevQueue.toLong()).toInt() and STATE_MASK
        check(state == RUNNING)
    }
}

internal fun wait(
    queue: AtomicInt,
    currQueue: Int,
) {
    val currState = addr(currQueue.toLong()).toInt() and STATE_MASK
    while ((addr(queue.load().toLong()).toInt() and STATE_MASK) == currState) {
        // Spin until state changes from currState
    }
}

internal fun initializeOrWait(
    queue: AtomicInt,
    init: (() -> Boolean)?,
) {
    var currQueue = queue.load()
    while (true) {
        val currState = addr(currQueue.toLong()).toInt() and STATE_MASK
        if (currState == COMPLETE) {
            return
        }
        if (currState == INCOMPLETE && init != null) {
            val nextQueue =
                mapAddr(currQueue.toLong()) { q ->
                    (q and STATE_MASK.toLong().inv()) or RUNNING.toLong()
                }.toInt()
            val witness = queue.compareAndExchange(currQueue, nextQueue)
            if (witness != currQueue) {
                currQueue = witness
                continue
            }
            val guard = Guard(queue, INCOMPLETE)
            try {
                if (init()) {
                    guard.newQueue = COMPLETE
                }
            } finally {
                guard.drop()
            }
            return
        }
        if ((currState == INCOMPLETE && init == null) || currState == RUNNING) {
            wait(queue, currQueue)
            currQueue = queue.load()
        } else {
            check(false)
        }
    }
}

internal class OnceCell<T : Any> private constructor(
    initial: T?,
) {
    private val queue: AtomicInt = AtomicInt(if (initial == null) INCOMPLETE else COMPLETE)
    private val value: AtomicReference<T?> = AtomicReference(initial)

    public constructor() : this(null)

    public fun isInitialized(): Boolean = queue.load() == COMPLETE

    public fun initialize(f: () -> Result<T>): Result<Unit> {
        var fOpt: (() -> Result<T>)? = f
        var res: Result<Unit> = Result.success(Unit)
        initializeOrWait(queue) {
            val initFn = fOpt!!
            fOpt = null
            initFn().fold(
                onSuccess = { newValue ->
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

    public fun initializeOrWait(f: () -> Result<T>): Result<Unit> = initialize(f)

    public fun init(f: () -> T) {
        initialize { Result.success(f()) }
    }

    public fun get(): T? = if (isInitialized()) value.load() else null

    public fun getUnchecked(): T {
        check(isInitialized())
        return value.load()!!
    }

    public fun getMut(): T? = if (isInitialized()) value.load() else null

    public fun intoInner(): T? = if (isInitialized()) value.exchange(null) else null

    @kotlin.jvm.JvmName("awaitCompletion")
    public fun wait() {
        initializeOrWait(queue, null)
    }

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
            queue.store(COMPLETE)
        }
    }

    public fun addr(): Long = addr(queue.load().toLong())

    public fun withAddr(addr: Long): Long = withAddr(this.addr(), addr)

    public fun mapAddr(f: (Long) -> Long): Long = mapAddr(addr(), f)

    public fun smokeOnce() {}

    public fun stampedeOnce() {}

    public fun poisonBad() {}

    public fun waitForForceToFinish() {}

    public fun testSize() {}

    public companion object {
        public fun <T : Any> new(): OnceCell<T> = OnceCell()

        public fun <T : Any> withValue(value: T): OnceCell<T> = OnceCell(value)
    }
}
