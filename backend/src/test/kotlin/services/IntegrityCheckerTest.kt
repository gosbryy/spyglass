package gosbry.services

import gosbry.FabricLogs
import gosbry.IntegrityReport
import gosbry.IntegrityStatus
import gosbry.Loader
import gosbry.ParsedLog
import kotlin.test.*

class IntegrityCheckerTest {

    private fun checkContent(content: String): IntegrityReport = check(parseFabricLog("latest.log", content), content)

    private fun IntegrityReport.codes() = issues.map { it.code }

    @Test
    fun `untouched log is OK`() {
        val report = checkContent(FabricLogs.content)

        assertEquals(IntegrityStatus.OK, report.status)
        assertEquals(emptyList(), report.issues)
    }

    @Test
    fun `untouched windows log is OK`() {
        assertEquals(IntegrityStatus.OK, checkContent(FabricLogs.windows(FabricLogs.lines)).status)
    }

    @Test
    fun `neoforge is not checked`() {
        val parsed = ParsedLog(
            player = null,
            loader = Loader.NEOFORGE,
            minecraftVersion = null,
            modCount = 0,
            resourcePackCount = 0,
            mods = emptyList(),
            resourcePacks = emptyList(),
        )

        assertEquals(IntegrityReport(IntegrityStatus.NOT_CHECKED, emptyList()), check(parsed, ""))
    }

    @Test
    fun `missing mods header is reported`() {
        val content = FabricLogs.unix(FabricLogs.lines.filterNot { it.startsWith("\t") || "Loading 7 mods" in it })
        val report = checkContent(content)

        assertEquals(IntegrityStatus.SUSPICIOUS, report.status)
        assertEquals(listOf("MOD_LIST_MISSING"), report.codes())
    }

    @Test
    fun `removed top-level mod line breaks mod count`() {
        val content = FabricLogs.unix(FabricLogs.withoutLine("\t- sodium"))

        assertContains(checkContent(content).codes(), "MOD_COUNT_MISMATCH")
    }

    @Test
    fun `removed nested mod line breaks mod count`() {
        val content = FabricLogs.unix(FabricLogs.withoutLine("\t   |-- fabric-api-base"))

        assertContains(checkContent(content).codes(), "MOD_COUNT_MISMATCH")
    }

    @Test
    fun `edited declared count is reported`() {
        val content = FabricLogs.unix(
            FabricLogs.replacing("[18:31:16]", "[18:31:16] [main/INFO]: Loading 9 mods:"),
        )
        val issue = checkContent(content).issues.single { it.code == "MOD_COUNT_MISMATCH" }

        assertEquals("Header declares 9 mods, but the list contains 7", issue.message)
    }

    @Test
    fun `resources of a mod missing from the list are reported`() {
        val content = FabricLogs.unix(
            FabricLogs.replacing(
                "[18:31:25]",
                "[18:31:25] [Render thread/INFO]: Reloading ResourceManager: vanilla, fabric, iris, sodium, xray:default",
            ),
        )
        val issue = checkContent(content).issues.single { it.code == "UNKNOWN_MOD_RESOURCES" }

        assertEquals("Resources loaded for mods missing from the mod list: iris, xray:default", issue.message)
    }

    @Test
    fun `builtin, user, server and mod-provided packs are not orphans`() {
        val content = FabricLogs.unix(
            FabricLogs.replacing(
                "[18:31:25]",
                "[18:31:25] [Render thread/INFO]: Reloading ResourceManager: vanilla, fabric, programmer_art, " +
                    "high_contrast, file/Pack, server/resources, sodium:extra, sodium_translations",
            ),
        )

        assertEquals(IntegrityStatus.OK, checkContent(content).status)
    }

    @Test
    fun `log re-saved with CRLF everywhere is reported`() {
        val content = FabricLogs.lines.joinToString("\r\n", postfix = "\r\n")

        assertEquals(listOf("LINE_ENDINGS_NORMALIZED"), checkContent(content).codes())
    }

    @Test
    fun `CRLF everywhere without multiline entries is fine`() {
        val content = FabricLogs.lines.filter { it.startsWith("[") }.joinToString("\r\n", postfix = "\r\n")

        assertFalse("LINE_ENDINGS_NORMALIZED" in checkContent(content).codes())
    }

    @Test
    fun `timestamp going backwards is reported`() {
        val content = FabricLogs.unix(FabricLogs.lines + "[18:30:00] [Render thread/INFO]: Stopping!")
        val issue = checkContent(content).issues.single { it.code == "TIMESTAMP_ORDER" }

        assertEquals("Timestamp goes backwards at line ${FabricLogs.lines.size + 1}", issue.message)
    }

    @Test
    fun `crossing midnight is not reported`() {
        val content = FabricLogs.unix(
            FabricLogs.lines + "[23:59:58] [Render thread/INFO]: tick" + "[00:00:03] [Render thread/INFO]: tick",
        )

        assertEquals(IntegrityStatus.OK, checkContent(content).status)
    }

    @Test
    fun `several issues are reported together`() {
        val tampered = FabricLogs.withoutLine("\t- sodium") + "[18:00:00] [Render thread/INFO]: Stopping!"
        val codes = checkContent(FabricLogs.unix(tampered)).codes()

        assertEquals(listOf("MOD_COUNT_MISMATCH", "UNKNOWN_MOD_RESOURCES", "TIMESTAMP_ORDER"), codes)
    }
}
