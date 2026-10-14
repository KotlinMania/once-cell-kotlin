// port-lint: tests ../tests/it/sync_lazy.rs
package io.github.kotlinmania.oncecell.sync

import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SyncLazyTest {
    @Test
    fun lazyNew() {
        val called = AtomicInt(0)
        val lazy =
            Lazy.new {
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

    @Test
    fun staticLazy() {
        val xs =
            Lazy.new {
                val acc = mutableListOf<Int>()
                acc += 1
                acc += 2
                acc += 3
                acc.toList()
            }
        assertEquals(listOf(1, 2, 3), xs.value)
        assertEquals(listOf(1, 2, 3), xs.value)
    }

    @Test
    fun lazyPoisoning() {
        val x = Lazy.new<String> { throw RuntimeException("kaboom") }
        repeat(2) {
            assertFailsWith<RuntimeException> { Lazy.force(x) }
        }
    }
}
