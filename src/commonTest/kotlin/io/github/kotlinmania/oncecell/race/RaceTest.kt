// port-lint: tests race.rs
package io.github.kotlinmania.oncecell.race

import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RaceTest {
    @Test
    fun onceNonZeroUsizeSmokeTest() {
        val cnt = AtomicInt(0)
        val cell = OnceNonZeroUsize.new()
        val `val` = 92uL

        assertEquals(
            `val`,
            cell.getOrInit {
                cnt.fetchAndAdd(1)
                `val`
            },
        )
        assertEquals(1, cnt.load())

        assertEquals(
            `val`,
            cell.getOrInit {
                cnt.fetchAndAdd(1)
                `val`
            },
        )
        assertEquals(1, cnt.load())

        assertEquals(`val`, cell.get())
        assertEquals(1, cnt.load())
    }

    @Test
    fun onceNonZeroUsizeSet() {
        val val1 = 92uL
        val val2 = 62uL

        val cell = OnceNonZeroUsize.new()

        assertTrue(cell.set(val1).isOk)
        assertEquals(val1, cell.get())

        assertTrue(cell.set(val2).isErr)
        assertEquals(val1, cell.get())
    }

    @Test
    fun onceNonZeroUsizeFirstWins() {
        val val1 = 92uL
        val val2 = 62uL

        val cell = OnceNonZeroUsize.new()
        val r1 = cell.getOrInit { val1 }
        assertEquals(val1, r1)

        val r2 = cell.getOrInit { val2 }
        assertEquals(val1, r2)
        assertEquals(val1, cell.get())
    }

    @Test
    fun onceBoolSmokeTest() {
        val cnt = AtomicInt(0)
        val cell = OnceBool.new()

        assertEquals(
            false,
            cell.getOrInit {
                cnt.fetchAndAdd(1)
                false
            },
        )
        assertEquals(1, cnt.load())

        assertEquals(
            false,
            cell.getOrInit {
                cnt.fetchAndAdd(1)
                false
            },
        )
        assertEquals(1, cnt.load())

        assertEquals(false, cell.get())
        assertEquals(1, cnt.load())
    }

    @Test
    fun onceBoolSet() {
        val cell = OnceBool.new()

        assertTrue(cell.set(false).isOk)
        assertEquals(false, cell.get())

        assertTrue(cell.set(true).isErr)
        assertEquals(false, cell.get())
    }

    @Test
    fun getUnchecked() {
        val cell = OnceNonZeroUsize.new()
        cell.set(92uL)
        val value = cell.getUnchecked()
        assertEquals(92uL, value)
    }

    @Test
    fun onceRefSmokeTest() {
        val cell = OnceRef.new<String>()
        assertNull(cell.get())

        val setRes = cell.set("hello")
        assertTrue(setRes.isOk)
        assertEquals("hello", cell.get())

        val setRes2 = cell.set("world")
        assertTrue(setRes2.isErr)
        assertEquals("hello", cell.get())
    }

    @Test
    fun onceRefFirstWins() {
        val cell = OnceRef.new<String>()
        val val1 = "first"
        val val2 = "second"

        val r1 = cell.getOrInit { val1 }
        assertEquals("first", r1)

        val r2 = cell.getOrInit { val2 }
        assertEquals("first", r2)
        assertEquals("first", cell.get())
    }
}
