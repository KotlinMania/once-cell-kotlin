// port-lint: tests ../tests/it/unsync_lazy.rs
package io.github.kotlinmania.oncecell.unsync

import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class UnsyncLazyTest {
    @Test
    fun lazyNew() {
        val called = AtomicInt(0)
        val x =
            Lazy.new {
                called.fetchAndAdd(1)
                92
            }

        assertEquals(0, called.load())

        val y = x.value - 30
        assertEquals(62, y)
        assertEquals(1, called.load())

        val y2 = x() - 30
        assertEquals(62, y2)
        assertEquals(1, called.load())

        assertEquals(62, Lazy.force(x) - 30)
        assertEquals(1, called.load())
    }

    @Test
    fun lazyForceMut() {
        val called = AtomicInt(0)
        val x =
            Lazy.new {
                called.fetchAndAdd(1)
                AtomicInt(92)
            }
        assertEquals(0, called.load())
        val v = Lazy.forceMut(x)
        assertEquals(1, called.load())

        v.store(46)
        assertEquals(46, x.value.load())
        assertEquals(1, called.load())
    }

    @Test
    fun lazyGetMut() {
        val called = AtomicInt(0)
        val x =
            Lazy.new {
                called.fetchAndAdd(1)
                AtomicInt(92)
            }

        assertEquals(0, called.load())
        assertNull(Lazy.get(x))
        assertNull(Lazy.getMut(x))

        assertEquals(92, x.value.load())
        val mutRef = Lazy.getMut(x)!!
        assertEquals(1, called.load())

        mutRef.store(46)
        assertEquals(46, x.value.load())
        assertEquals(1, called.load())
    }

    @Test
    fun lazyIntoValue() {
        val uninit = Lazy.new<Int> { error("unreachable") }
        assertIs<LazyValueResult.Initializer<Int>>(Lazy.intoValue(uninit))

        val init = Lazy.new { 92 }
        assertEquals(92, Lazy.force(init))
        val value = Lazy.intoValue(init)
        assertIs<LazyValueResult.Value<Int>>(value)
        assertEquals(92, value.value)
    }

    @Test
    fun lazyPoisoning() {
        val x = Lazy.new<String> { error("kaboom") }
        repeat(2) {
            assertFailsWith<IllegalStateException> { x.value }
        }
    }
}
