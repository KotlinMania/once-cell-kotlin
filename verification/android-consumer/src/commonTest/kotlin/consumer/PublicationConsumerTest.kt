package consumer

import io.github.kotlinmania.oncecell.sync.SetResult as SyncSetResult
import io.github.kotlinmania.oncecell.sync.TryInsertResult as SyncTryInsertResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublicationConsumerTest {
    @Test
    fun concreteTypedStringAndIntCellsPreservePayloadsAndRejectDuplicates() {
        val syncStr = newSyncStringCell()
        assertFalse(syncStr.isInitialized())
        assertNull(syncStr.get())
        val first = syncStr.set("alpha")
        assertTrue(first.isOk)
        assertNull(first.error)
        assertEquals("alpha", syncStr.get())
        assertEquals("alpha", syncStr.wait())

        val duplicate = syncStr.set("beta")
        assertTrue(duplicate.isErr)
        assertEquals("beta", duplicate.error)

        val insertDup = syncStr.tryInsert("gamma")
        assertTrue(insertDup.isExisting)
        assertEquals("alpha", insertDup.current)
        assertEquals("gamma", insertDup.value)

        val syncInt = newSyncIntCell()
        assertEquals(42, syncInt.getOrInit { 42 })
        val intDup = syncInt.tryInsert(99)
        assertTrue(intDup.isExisting)
        assertEquals(42, intDup.current)
        assertEquals(99, intDup.value)

        val unsyncStr = newUnsyncStringCell()
        assertTrue(unsyncStr.set("delta").isOk)
        assertEquals("delta", unsyncStr.intoInner())

        val unsyncInt = newUnsyncIntCell()
        assertEquals(7, unsyncInt.getOrInit { 7 })
        assertEquals(7, unsyncInt.take())
        assertNull(unsyncInt.get())
    }

    @Test
    fun genericSubclassAndNestedContainerPreserveRelationships() {
        val syncCell = ConsumerSyncCell<List<String>>()
        assertNull(syncCell.get())
        val inserted = syncCell.tryInsert(listOf("kmp", "once_cell"))
        assertTrue(inserted is SyncTryInsertResult.Inserted)
        assertEquals(listOf("kmp", "once_cell"), inserted.value)

        val rejected = syncCell.set(listOf("other"))
        assertTrue(rejected is SyncSetResult.Err)
        assertEquals(listOf("other"), rejected.error)
        assertEquals(listOf("kmp", "once_cell"), syncCell.get())

        val unsyncCell = ConsumerUnsyncCell<Map<String, Int>>()
        assertEquals(mapOf("a" to 1), unsyncCell.getOrInit { mapOf("a" to 1) })
        assertEquals(1, unsyncCell.get()?.get("a"))
    }

    @Test
    fun failedInitDoesNotPoisonOnceCellWhileLazyPoisonsOnPanic() {
        val syncCell = ConsumerSyncCell<String>()
        val error = assertFailsWith<IllegalArgumentException> {
            syncCell.getOrInit { throw IllegalArgumentException("init failed") }
        }
        assertEquals("init failed", error.message)
        assertFalse(syncCell.isInitialized())
        assertEquals("recovered", syncCell.getOrInit { "recovered" })

        val lazy = newSyncLazy<String> { throw IllegalStateException("boom") }
        assertFailsWith<IllegalStateException> { lazy.value }
        val poisoned = assertFailsWith<IllegalStateException> { lazy.value }
        assertEquals("Lazy instance has previously been poisoned", poisoned.message)

        val unsyncLazy = newUnsyncLazy<Int> { throw IllegalStateException("unsync boom") }
        assertFailsWith<IllegalStateException> { unsyncLazy.value }
        val unsyncPoisoned = assertFailsWith<IllegalStateException> { unsyncLazy.value }
        assertEquals("Lazy instance has previously been poisoned", unsyncPoisoned.message)
    }

    @Test
    fun mutationTakeAndCloneFromResetAndOverwriteDeterministically() {
        val first = newSyncStringCell()
        assertTrue(first.set("initial").isOk)
        assertEquals("initial", first.take())
        assertFalse(first.isInitialized())
        assertTrue(first.set("replacement").isOk)
        assertEquals("replacement", first.get())

        val target = newSyncStringCell()
        assertTrue(target.set("stale").isOk)
        target.cloneFrom(first)
        assertEquals("replacement", target.get())

        val empty = newSyncStringCell()
        target.cloneFrom(empty)
        assertFalse(target.isInitialized())
        assertNull(target.get())
    }

    @Test
    fun raceCellsPreserveFirstWriterAndRejectZeroUsize() {
        val onceBool = newRaceBool()
        assertNull(onceBool.get())
        assertTrue(onceBool.getOrInit { true })
        assertFalse(onceBool.set(false).isOk)
        assertEquals(true, onceBool.get())

        val usize = newRaceUsize()
        assertFailsWith<IllegalArgumentException> {
            usize.set(0uL)
        }
        assertNull(usize.get())
        assertEquals(92uL, usize.getOrInit { 92uL })
        assertEquals(92uL, usize.get())

        val box = newRaceBox<String>()
        assertTrue(box.set("boxed").isOk)
        assertTrue(box.set("second").isErr)
        assertEquals("boxed", box.get())
    }
}
