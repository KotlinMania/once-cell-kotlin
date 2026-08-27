// port-lint: tests race.rs
package io.github.kotlinmania.oncecell.race

import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    @Test
    fun onceBoxSmokeTest() {
        val globalCnt = AtomicInt(0)
        val cell = OnceBox.new<String>()

        val r1 = cell.getOrInit {
            globalCnt.fetchAndAdd(1)
            "hello"
        }
        assertEquals("hello", r1)
        assertEquals(1, globalCnt.load())

        val r2 = cell.getOrInit {
            globalCnt.fetchAndAdd(1)
            "world"
        }
        assertEquals("hello", r2)
        assertEquals(1, globalCnt.load())
        assertEquals("hello", cell.get())
    }

    @Test
    fun onceBoxSet() {
        val cell = OnceBox.new<String>()
        assertNull(cell.get())

        assertTrue(cell.set("hello").isOk)
        assertEquals("hello", cell.get())

        val setAgain = cell.set("world")
        assertTrue(setAgain.isErr)
        assertEquals("hello", cell.get())
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
        val res = cell.getOrInit {
            cell.getOrInit { "hello" }
            "world"
        }
        assertEquals("hello", res)
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
