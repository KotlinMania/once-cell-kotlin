// port-lint: source tests/it/sync_lazy.rs
package io.github.kotlinmania.oncecell.sync

import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SyncLazyTest {
    @Test
    fun lazyNew() {
        val called = AtomicInt(0)
        val lazy = Lazy.new {
            called.fetchAndAdd(1)
            92
        }

        assertEquals(0, called.load())
        assertEquals(62, lazy.value - 30)
        assertEquals(1, called.load())
        assertEquals(62, Lazy.force(lazy) - 30)
        assertEquals(1, called.load())
    }

    @Test
    fun staticLazyViaFunction() {
        val cell = OnceCell.new<List<Int>>()

        fun xs(): List<Int> =
            cell.getOrInit {
                val xs = mutableListOf<Int>()
                xs += 1
                xs += 2
                xs += 3
                xs
            }

        assertEquals(listOf(1, 2, 3), xs())
        assertEquals(listOf(1, 2, 3), xs())
    }

    @Test
    fun lazyIntoValue() {
        val uninitialized = Lazy.new { 92 }
        assertIs<LazyValueResult.Initializer<Int>>(Lazy.intoValue(uninitialized))

        val initialized = Lazy.new { 92 }
        assertEquals(92, Lazy.force(initialized))
        val value = Lazy.intoValue(initialized)
        assertIs<LazyValueResult.Value<Int>>(value)
        assertEquals(92, value.value)
    }
}

class SyncOnceCellTest {
    @Test
    fun onceCell() {
        val cell = OnceCell.new<Int>()
        assertNull(cell.get())
        assertEquals(92, cell.getOrInit { 92 })
        assertEquals(92, cell.get())
        assertEquals(92, cell.getOrInit { error("Kabom!") })
    }

    @Test
    fun onceCellWithValue() {
        val cell = OnceCell.withValue(12)
        assertEquals(12, cell.get())
    }

    @Test
    fun setAndTryInsert() {
        val cell = OnceCell.new<Int>()

        assertIs<SetResult.Ok>(cell.set(92))

        val setAgain = cell.set(62)
        assertIs<SetResult.Err<Int>>(setAgain)
        assertEquals(62, setAgain.value)

        val insertAgain = cell.tryInsert(31)
        assertIs<TryInsertResult.Existing<Int>>(insertAgain)
        assertEquals(92, insertAgain.current)
        assertEquals(31, insertAgain.value)
    }

    @Test
    fun getOrTryInit() {
        val cell = OnceCell.new<String>()

        assertNull(cell.get())
        val failed = cell.getOrTryInit { Result.failure<String>(IllegalStateException("not yet")) }
        assertEquals(true, failed.isFailure)
        assertNull(cell.get())

        val initialized = cell.getOrTryInit { Result.success("hello") }
        assertEquals("hello", initialized.getOrThrow())
        assertEquals("hello", cell.get())
    }

    @Test
    fun copyAndEquality() {
        val empty = OnceCell.new<String>()
        assertEquals(empty, empty.copy())

        val full = OnceCell.withValue("value")
        assertEquals(full, full.copy())
        assertEquals(false, full == OnceCell.withValue("other"))
    }
}
