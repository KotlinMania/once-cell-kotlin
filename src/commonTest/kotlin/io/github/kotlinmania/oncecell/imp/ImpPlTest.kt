// port-lint: tests once_cell/src/imp_pl.rs
package io.github.kotlinmania.oncecell.imp

import io.github.kotlinmania.oncecell.imp.pl.OnceCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImpPlTest {
    @Test
    fun testSize() {
        val cell = OnceCell.new<Boolean>()
        assertNull(cell.intoInner())
        assertTrue(cell.initialize { Result.success(true) }.isSuccess)
        assertEquals(true, cell.getUnchecked())
    }
}
