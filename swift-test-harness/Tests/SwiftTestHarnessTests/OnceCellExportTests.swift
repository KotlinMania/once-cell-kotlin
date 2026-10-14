import Testing
import OnceCell

@Suite("OnceCell Export Tests")
struct OnceCellExportTests {
    @Test("Swift sync.StringOnceCell initializes, sets, and reads value")
    func testSyncStringOnceCell() throws {
        let cell = sync.StringOnceCell()
        #expect(!cell.isInitialized())

        let setResult = cell.set(newValue: "Hello Swift")
        #expect(setResult.isOk)
        #expect(!setResult.isErr)
        #expect(cell.isInitialized())

        let secondSet = cell.set(newValue: "Duplicate Write")
        #expect(!secondSet.isOk)
        #expect(secondSet.isErr)

        let value = cell.intoInner()
        #expect(value != nil)
    }

    @Test("Swift unsync.StringOnceCell sets and reads value")
    func testUnsyncStringOnceCell() throws {
        let cell = unsync.StringOnceCell()
        let setResult = cell.set(value: "Hello Unsync Swift")
        #expect(setResult.isOk)
        #expect(!setResult.isErr)

        let duplicateSet = cell.set(value: "Duplicate Write")
        #expect(!duplicateSet.isOk)
        #expect(duplicateSet.isErr)

        let value = cell.intoInner()
        #expect(value != nil)
    }

    @Test("Swift race.OnceBool sets and reads value")
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
    }
}
