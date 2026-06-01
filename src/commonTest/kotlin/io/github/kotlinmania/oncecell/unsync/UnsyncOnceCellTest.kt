// port-lint: source tests/it/unsync_once_cell.rs
package io.github.kotlinmania.oncecell.unsync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnsyncOnceCellTest {

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
        val cell = OnceCell.withValue(12)
        assertEquals(12, cell.get())
    }

    @Test
    fun onceCellGetMut() {
        val c = OnceCell.new<Int>()
        assertNull(c.getMut())
        c.set(90)
        // The Rust test mutates through &mut; in Kotlin the value is replaced via take/set.
        val current = c.getMut()!!
        c.take()
        c.set(current + 2)
        assertEquals(92, c.getMut())
    }

    @Test
    fun onceCellDropEmpty() {
        // Constructing and dropping an empty cell should not throw.
        OnceCell.new<String>()
    }

    @Test
    fun clone() {
        val s = OnceCell.new<String>()
        val empty = s.copy()
        assertNull(empty.get())

        s.set("hello")
        val populated = s.copy()
        assertEquals("hello", populated.get())
    }

    @Test
    fun getOrTryInit() {
        val cell = OnceCell.new<String>()
        assertNull(cell.get())

        // Mirror Rust's `catch_unwind`: when the initializer panics, the panic
        // is propagated to the caller, and the cell remains uninitialized.
        val thrown = runCatching { cell.getOrTryInit { error("boom!") } }
        assertTrue(thrown.isFailure)
        assertNull(cell.get())

        val failed = cell.getOrTryInit { Result.failure(IllegalStateException("nope")) }
        assertTrue(failed.isFailure)

        val ok = cell.getOrTryInit { Result.success("hello") }
        assertEquals(Result.success("hello"), ok)
        assertEquals("hello", cell.get())
    }

    @Test
    fun fromImpl() {
        assertEquals("value", OnceCell.withValue("value").get())
        assertNotEquals("bar", OnceCell.withValue("foo").get())
    }

    @Test
    fun partialeqImpl() {
        assertTrue(OnceCell.withValue("value") == OnceCell.withValue("value"))
        assertFalse(OnceCell.withValue("foo") == OnceCell.withValue("bar"))

        assertTrue(OnceCell.new<String>() == OnceCell.new<String>())
        assertFalse(OnceCell.new<String>() == OnceCell.withValue("value"))
    }

    @Test
    fun intoInner() {
        val empty = OnceCell.new<String>()
        assertNull(empty.intoInner())
        val cell = OnceCell.new<String>()
        cell.set("hello")
        assertEquals("hello", cell.intoInner())
    }

    @Test
    fun debugImpl() {
        val cell = OnceCell.new<List<String>>()
        assertEquals("OnceCell(Uninit)", cell.toString())
        cell.set(listOf("hello", "world"))
        // The Kotlin representation differs from Rust's `{:#?}` pretty-printed
        // output; the test verifies the value appears within OnceCell(...).
        assertTrue(cell.toString().startsWith("OnceCell("))
        assertTrue(cell.toString().contains("hello"))
        assertTrue(cell.toString().contains("world"))
    }

    @Test
    fun reentrantInit() {
        val x = OnceCell.new<Int>()
        assertFailsWith<IllegalStateException> {
            x.getOrInit {
                x.getOrInit { 92 }
                62
            }
        }
    }
}
