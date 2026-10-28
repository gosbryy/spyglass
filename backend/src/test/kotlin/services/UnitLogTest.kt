package gosbry.services

import gosbry.FabricLogs
import gosbry.IntegrityStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.*

class UnitLogTest {

    @Test
    fun `builds report from parsed log and integrity check`() {
        val report = unitLog("client.log", FabricLogs.content)

        assertEquals("client.log", report.fileName)
        assertEquals("Steve", report.player)
        assertEquals("1.21.1", report.minecraftVersion)
        assertEquals(2, report.modCount)
        assertEquals(report.mods.size, report.modCount)
        assertEquals(2, report.resourcePackCount)
        assertEquals(IntegrityStatus.OK, report.integrityStatus)
        assertEquals(report.integrity.status, report.integrityStatus)
    }

    @Test
    fun `id is uuid and uploadedAt is an instant`() {
        val report = unitLog("latest.log", FabricLogs.content)

        UUID.fromString(report.id)
        Instant.parse(report.uploadedAt)
    }

    @Test
    fun `each call gets a new id`() {
        assertNotEquals(unitLog("latest.log", FabricLogs.content).id, unitLog("latest.log", FabricLogs.content).id)
    }

    @Test
    fun `player from log wins over default`() {
        assertEquals("Steve", unitLog("latest.log", FabricLogs.content, defaultPlayer = "Alex").player)
    }

    @Test
    fun `default player is used when log has none`() {
        val content = FabricLogs.unix(FabricLogs.withoutLine("[18:31:20]"))

        assertEquals("Alex", unitLog("latest.log", content, defaultPlayer = "Alex").player)
        assertNull(unitLog("latest.log", content).player)
    }

    @Test
    fun `suspicious status is propagated`() {
        val content = FabricLogs.unix(FabricLogs.withoutLine("\t- sodium"))

        assertEquals(IntegrityStatus.SUSPICIOUS, unitLog("latest.log", content).integrityStatus)
    }
}
