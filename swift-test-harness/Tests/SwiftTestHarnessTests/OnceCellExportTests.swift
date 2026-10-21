import Testing
import OnceCell

@Suite("OnceCell Export Tests")
struct OnceCellExportTests {
    @Test("Swift sync.StringOnceCell initializes, sets, and reads concrete String value")
    func testSyncStringOnceCell() throws {
        let cell = sync.StringOnceCell()
        #expect(!cell.isInitialized())
        #expect(cell.get() == nil)

        let setResult = cell.set(newValue: "Hello Swift")
        #expect(setResult.isOk)
        #expect(!setResult.isErr)
        #expect(setResult.error == nil)
        #expect(cell.isInitialized())
        #expect(cell.get() == "Hello Swift")
        #expect(cell.wait() == "Hello Swift")

        let secondSet = cell.set(newValue: "Duplicate Write")
        #expect(!secondSet.isOk)
        #expect(secondSet.isErr)
        #expect(secondSet.error == "Duplicate Write")

        let tryRes = cell.tryInsert(newValue: "Third Write")
        #expect(!tryRes.isInserted)
        #expect(tryRes.isExisting)
        #expect(tryRes.current == "Hello Swift")
        #expect(tryRes.value == "Third Write")

        let taken = cell.take()
        #expect(taken == "Hello Swift")
        #expect(!cell.isInitialized())

        let reinit = cell.getOrInit { "Reinitialized" }
        #expect(reinit == "Reinitialized")
        #expect(cell.intoInner() == "Reinitialized")
    }

    @Test("Swift sync.IntOnceCell tryInsert, take, and getOrInit concrete Int32 value")
    func testSyncIntOnceCell() throws {
        let cell = sync.IntOnceCell()
        #expect(cell.get() == nil)

        let inserted = cell.tryInsert(newValue: 42)
        #expect(inserted.isInserted)
        #expect(inserted.value == 42)
        #expect(inserted.current == nil)

        let existing = cell.tryInsert(newValue: 99)
        #expect(existing.isExisting)
        #expect(existing.current == 42)
        #expect(existing.value == 99)
        #expect(cell.get() == 42)

        #expect(cell.take() == 42)
        #expect(cell.get() == nil)
        #expect(cell.getOrInit { 77 } == 77)
        #expect(cell.intoInner() == 77)
    }

    @Test("Swift unsync.StringOnceCell and IntOnceCell set and read concrete values")
    func testUnsyncStringOnceCell() throws {
        let cell = unsync.StringOnceCell()
        let setResult = cell.set(value: "Hello Unsync Swift")
        #expect(setResult.isOk)
        #expect(!setResult.isErr)
        #expect(setResult.error == nil)
        #expect(cell.get() == "Hello Unsync Swift")

        let duplicateSet = cell.set(value: "Duplicate Write")
        #expect(!duplicateSet.isOk)
        #expect(duplicateSet.isErr)
        #expect(duplicateSet.error == "Duplicate Write")

        #expect(cell.intoInner() == "Hello Unsync Swift")

        let intCell = unsync.IntOnceCell()
        #expect(intCell.getOrInit { 123 } == 123)
        #expect(intCell.take() == 123)
        #expect(intCell.get() == nil)
    }

    @Test("Swift race.OnceBool and OnceNonZeroUsize set, getOrInit, and read values")
    func testRaceOnceBool() throws {
        let cell = race.OnceBool()
        #expect(cell.get() == nil)

        let setResult = cell.set(value: true)
        #expect(setResult.isOk)
        #expect(!setResult.isErr)

        #expect(cell.get() == true)

        let duplicateSet = cell.set(value: false)
        #expect(!duplicateSet.isOk)
        #expect(duplicateSet.isErr)
        #expect(cell.get() == true)

        let lazyBool = race.OnceBool()
        #expect(lazyBool.getOrInit { false } == false)
        #expect(lazyBool.get() == false)

        let usizeCell = race.OnceNonZeroUsize()
        #expect(usizeCell.get() == nil)
        #expect(usizeCell.getOrInit { 92 } == 92)
        #expect(usizeCell.get() == 92)
    }
}
