// port-lint: tests once_cell/src/imp_std.rs
package io.github.kotlinmania.oncecell.imp

import io.github.kotlinmania.oncecell.imp.std.OnceCell
import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImpStdTest {
    @Test
    fun smokeOnce() {
        val cell = OnceCell.new<Int>()
        assertNull(cell.get())
        assertTrue(cell.initialize { Result.success(92) }.isSuccess)
        assertEquals(92, cell.get())
    }

    @Test
    fun stampedeOnce() {
        val cell = OnceCell.new<Int>()
        val cnt = AtomicInt(0)
        repeat(10) {
            cell.initialize {
                cnt.fetchAndAdd(1)
                Result.success(92)
            }
        }
        assertEquals(92, cell.get())
        assertEquals(1, cnt.load())
    }

    @Test
    fun poisonBad() {
        val cell = OnceCell.new<Int>()
        assertFailsWith<IllegalStateException> {
            cell.initialize { Result.failure(IllegalStateException("failed")) }.getOrThrow()
        }
        assertNull(cell.get())
        assertTrue(cell.initialize { Result.success(42) }.isSuccess)
        assertEquals(42, cell.get())
    }

    @Test
    fun waitForForceToFinish() {
        val cell = OnceCell.new<String>()
        cell.initialize { Result.success("ready") }
        assertEquals("ready", cell.get())
    }

    @Test
    fun testSize() {
        val cell = OnceCell.new<Long>()
        assertTrue(cell.intoInner() == null)
    }
}
