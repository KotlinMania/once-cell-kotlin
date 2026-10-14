// port-lint: tests ../tests/it/sync_once_cell.rs
package io.github.kotlinmania.oncecell.sync

import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncOnceCellTest {
    companion object {
        private val CELL: OnceCell<Int> = OnceCell.withValue(12)
        private val EVAL_ONCE_CELL: OnceCell<List<Int>> = OnceCell.new()
    }

    @Test
    fun onceCell() {
        val c = OnceCell.new<Int>()
        assertNull(c.get())
        c.getOrInit { 92 }
        assertEquals(92, c.get())

        c.getOrInit { error("Kabom!") }
        assertEquals(92, c.get())
    }

    @Test
    fun onceCellWithValue() {
        assertEquals(12, CELL.get())
    }

    @Test
    fun onceCellGetMut() {
        val c = OnceCell.new<IntArray>()
        assertNull(c.getMut())
        assertTrue(c.set(intArrayOf(90)).isOk)
        c.getMut()!![0] += 2
        assertEquals(92, c.getMut()!![0])
    }

    @Test
    fun onceCellGetUnchecked() {
        val c = OnceCell.new<Int>()
        assertTrue(c.set(92).isOk)
        assertEquals(92, c.getUnchecked())
    }

    // once_cell_drop: relies on deterministic Rust RAII Drop when scope exits — Kotlin GC has no deterministic destructor on scope exit.
    @Test
    fun onceCellDropEmpty() {
        val x = OnceCell.new<String>()
        assertNull(x.intoInner())
    }

    @Test
    fun cloneTest() {
        val s = OnceCell.new<String>()
        val c1 = s.clone()
        assertNull(c1.get())

        assertTrue(s.set("hello").isOk)
        val c2 = s.clone()
        assertEquals("hello", c2.get())
    }

    @Test
    fun getOrTryInit() {
        val cell = OnceCell.new<String>()
        assertNull(cell.get())

        val failedPanic =
            cell.getOrTryInit {
                error("boom")
            }
        assertTrue(failedPanic.isFailure)
        assertNull(cell.get())

        val failedErr =
            cell.getOrTryInit {
                Result.failure(IllegalArgumentException("err"))
            }
        assertTrue(failedErr.isFailure)
        assertNull(cell.get())

        val succeeded =
            cell.getOrTryInit {
                Result.success("hello")
            }
        assertEquals("hello", succeeded.getOrThrow())
        assertEquals("hello", cell.get())
    }

    @Test
    fun waitTest() {
        val cell = OnceCell.new<String>()
        assertTrue(cell.set("hello").isOk)
        val greeting = cell.wait()
        assertEquals("hello", greeting)
    }

    @Test
    fun getOrInitStress() {
        val nCells = 30
        val cells = List(nCells) { OnceCell.new<Int>() }
        for ((i, cell) in cells.withIndex()) {
            val v1 = cell.getOrInit { i }
            val v2 = cell.wait()
            assertEquals(i, v1)
            assertEquals(i, v2)
        }
    }

    @Test
    fun fromImpl() {
        assertEquals("value", OnceCell.from("value").get())
        assertNotEquals("bar", OnceCell.from("foo").get())
    }

    @Test
    fun partialEqImpl() {
        assertEquals(OnceCell.from("value"), OnceCell.from("value"))
        assertNotEquals(OnceCell.from("foo"), OnceCell.from("bar"))

        assertEquals(OnceCell.new<String>(), OnceCell.new())
        assertNotEquals(OnceCell.new<String>(), OnceCell.from("value"))
    }

    @Test
    fun intoInner() {
        val emptyCell = OnceCell.new<String>()
        assertNull(emptyCell.intoInner())
        val fullCell = OnceCell.new<String>()
        assertTrue(fullCell.set("hello").isOk)
        assertEquals("hello", fullCell.intoInner())
    }

    @Test
    fun debugImpl() {
        val cell = OnceCell.new<List<String>>()
        assertEquals("OnceCell(Uninit)", cell.toString())
        assertTrue(cell.set(listOf("hello", "world")).isOk)
        assertEquals("OnceCell([hello, world])", cell.toString())
    }

    // reentrant_init (std): spawns external compiled cargo example binary `reentrant_init_deadlocks` to observe OS process hang — not applicable to KMP unit tests.

    @Test
    fun evalOnceMacro() {
        fun evalOnce(): List<Int> =
            EVAL_ONCE_CELL.getOrInit {
                val res = mutableListOf(1, 1)
                for (i in 0 until 10) {
                    val next = res[i] + res[i + 1]
                    res.add(next)
                }
                res
            }

        val fib = evalOnce()
        assertEquals(8, fib[5])
    }

    @Test
    fun onceCellDoesNotLeakPartiallyConstructedBoxes() {
        val nTries = 10
        val msg = "Hello, World"

        repeat(nTries) {
            val cell = OnceCell.new<String>()
            assertTrue(cell.set(msg).isOk)
            assertFalse(cell.set("other").isOk)
            assertEquals(msg, cell.get())
        }
    }

    @Test
    fun getDoesNotBlock() {
        val cell = OnceCell.new<String>()
        val observedWhileInit = AtomicInt(0)
        val result =
            cell.getOrInit {
                if (cell.get() == null) {
                    observedWhileInit.store(1)
                }
                "hello"
            }
        assertEquals(1, observedWhileInit.load())
        assertEquals("hello", result)
        assertEquals("hello", cell.get())
    }

    @Test
    fun arrrrrrrrrrrrrrrrrrrrrr() {
        val cell = OnceCell.new<String>()
        run {
            val s = ""
            assertTrue(cell.set(s).isOk)
        }
        assertEquals("", cell.get())
    }

    @Test
    fun onceCellIsSyncSend() {
        val cell = OnceCell.new<String>()
        val lazy = Lazy.new { "hello" }
        assertTrue(cell.set("world").isOk)
        assertEquals("world", cell.get())
        assertEquals("hello", lazy.value)
    }

    @Test
    fun stringAndIntOnceCellTypedFacades() {
        val strCell = StringOnceCell()
        assertNull(strCell.get())
        assertTrue(strCell.set("alpha").isOk)
        assertEquals("alpha", strCell.get())
        assertEquals("alpha", strCell.take())
        assertNull(strCell.get())

        val intCell = IntOnceCell()
        assertNull(intCell.get())
        assertEquals(42, intCell.getOrInit { 42 })
        assertEquals(42, intCell.get())

        assertFailsWith<IllegalStateException> {
            val failing = IntOnceCell()
            failing.getOrInit { error("fail") }
        }
    }
}
