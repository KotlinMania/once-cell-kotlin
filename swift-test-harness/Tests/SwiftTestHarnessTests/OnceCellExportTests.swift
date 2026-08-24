import Testing
import OnceCell

@Suite("OnceCell Export Smoke Tests")
struct OnceCellExportTests {
    @Test("Swift module loads cleanly")
    func testSwiftModuleLoads() throws {
        #expect(true)
    }
}
