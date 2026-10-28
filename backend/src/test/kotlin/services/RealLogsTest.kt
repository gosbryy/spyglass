package gosbry.services

import gosbry.IntegrityStatus
import gosbry.Loader
import gosbry.RealLogs
import kotlin.test.*

class RealLogsTest {

    @Test
    fun `clean fabric log`() {
        val report = unitLog("latest.log", RealLogs.fabricClean)

        assertEquals(Loader.FABRIC, report.loader)
        assertEquals("26.1.2", report.minecraftVersion)
        assertEquals("fishbry", report.player)
        assertEquals(89, report.modCount)
        assertContains(report.mods.map { it.id }, "sodium")
        assertTrue(report.mods.none { it.id in setOf("minecraft", "java", "fabricloader") })
        assertEquals(
            listOf("SodiumTranslations.zip", "Reimagined_[v1.63.5].zip", "Icons v.1.13.4.zip"),
            report.resourcePacks.map { it.name },
        )
        assertEquals(IntegrityStatus.OK, report.integrityStatus)
        assertEquals(emptyList(), report.integrity.issues)
    }

    @Test
    fun `tampered fabric log`() {
        val report = unitLog("latest.log", RealLogs.fabricTampered)

        assertEquals(IntegrityStatus.SUSPICIOUS, report.integrityStatus)
        assertEquals(
            listOf("MOD_COUNT_MISMATCH", "UNKNOWN_MOD_RESOURCES", "LINE_ENDINGS_NORMALIZED"),
            report.integrity.issues.map { it.code },
        )
        assertEquals(
            "Header declares 260 mods, but the list contains 259",
            report.integrity.issues[0].message,
        )
        assertEquals(
            "Resources loaded for mods missing from the mod list: carmona",
            report.integrity.issues[1].message,
        )
    }

    @Test
    fun `lunar client log is not recognized as fabric`() {
        assertFailsWith<LogParseException> { unitLog("latest.log", RealLogs.lunar) }
    }
}
