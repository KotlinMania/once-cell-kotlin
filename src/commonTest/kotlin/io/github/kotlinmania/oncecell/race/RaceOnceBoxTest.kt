// port-lint: tests ../tests/it/race_once_box.rs
package io.github.kotlinmania.oncecell.race

import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RaceOnceBoxTest {
    private class Heap {
        val total = AtomicInt(0)

        fun total(): Int = total.load()

        fun <T> newPebble(value: T): Pebble<T> {
            total.fetchAndAdd(1)
            return Pebble(value, total)
        }
    }

    private class Pebble<T>(
        val value: T,
        val total: AtomicInt,
    ) {
        fun drop() {
            total.fetchAndAdd(-1)
        }
    }

    @Test
    fun onceBoxSmokeTest() {
        val heap = Heap()
        val globalCnt = AtomicInt(0)
        val cell = OnceBox.new<Pebble<Unit>>()

        val localCnt = AtomicInt(0)
        cell.getOrInit {
            globalCnt.fetchAndAdd(1)
            localCnt.fetchAndAdd(1)
            heap.newPebble(Unit)
        }
        assertEquals(1, localCnt.load())

        cell.getOrInit {
            globalCnt.fetchAndAdd(1)
            localCnt.fetchAndAdd(1)
            heap.newPebble(Unit)
        }
        assertEquals(1, localCnt.load())

        assertTrue(cell.get() != null)
        assertEquals(1, globalCnt.load())
        assertEquals(1, heap.total())
    }

    @Test
    fun onceBoxSet() {
        val heap = Heap()
        val cell = OnceBox.new<Pebble<String>>()
        assertNull(cell.get())

        assertTrue(cell.set(heap.newPebble("hello")).isOk)
        assertEquals("hello", cell.get()!!.value)
        assertEquals(1, heap.total())

        val second = heap.newPebble("world")
        assertTrue(cell.set(second).isErr)
        second.drop()
        assertEquals("hello", cell.get()!!.value)
        assertEquals(1, heap.total())
    }

    @Test
    fun onceBoxFirstWins() {
        val cell = OnceBox.new<Int>()
        val val1 = 92
        val val2 = 62

        val r1 = cell.getOrInit { val1 }
        assertEquals(val1, r1)

        val r2 = cell.getOrInit { val2 }
        assertEquals(val1, r2)
        assertEquals(val1, cell.get())
    }

    @Test
    fun onceBoxReentrant() {
        val cell = OnceBox.new<String>()
        val res =
            cell.getOrInit {
                cell.getOrInit { "hello" }
                "world"
            }
        assertEquals("hello", res)
    }

    @Test
    fun onceBoxDefault() {
        class Foo

        val cell = OnceBox.default<Foo>()
        assertNull(cell.get())
    }

    @Test
    fun onceBoxWithValue() {
        val cell = OnceBox.withValue(92)
        assertEquals(92, cell.get())
    }

    @Test
    fun onceBoxClone() {
        val cell1 = OnceBox.new<Int>()
        val cell2 = cell1.clone()
        cell1.set(92)
        val cell3 = cell1.clone()
        assertEquals(92, cell1.get())
        assertNull(cell2.get())
        assertEquals(92, cell3.get())
    }
}
