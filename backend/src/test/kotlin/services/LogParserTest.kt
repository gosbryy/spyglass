package gosbry.services

import gosbry.FabricLogs
import gosbry.Loader
import gosbry.LogParseException
import gosbry.ModEntry
import gosbry.ResourcePackEntry
import kotlin.test.*

class LogParserTest {

    @Test
    fun `reads loader and minecraft version from header`() {
        val parsed = parseFabricLog(FabricLogs.content)

        assertEquals(Loader.FABRIC, parsed.loader)
        assertEquals("1.21.1", parsed.minecraftVersion)
    }

    @Test
    fun `keeps only top-level mods without platform`() {
        val parsed = parseFabricLog(FabricLogs.content)

        assertEquals(
            listOf(
                ModEntry(id = "fabric-api", version = "0.110.0+1.21.1"),
                ModEntry(id = "sodium", version = "0.6.13+mc1.21.1"),
            ),
            parsed.mods,
        )
        assertEquals(2, parsed.modCount)
    }

    @Test
    fun `reads player from Setting user`() {
        assertEquals("Steve", parseFabricLog(FabricLogs.content).player)
    }

    @Test
    fun `player is null when log has no Setting user`() {
        val content = FabricLogs.unix(FabricLogs.withoutLine("[18:31:20]"))

        assertNull(parseFabricLog(content).player)
    }

    @Test
    fun `takes user packs from first reload and strips formatting codes`() {
        val parsed = parseFabricLog(FabricLogs.content)

        assertEquals(
            listOf(ResourcePackEntry("Faithful 32x"), ResourcePackEntry("Clean UI")),
            parsed.resourcePacks,
        )
        assertEquals(2, parsed.resourcePackCount)
    }

    @Test
    fun `no resource manager line gives no packs`() {
        val content = FabricLogs.unix(FabricLogs.lines.filterNot { "Reloading ResourceManager" in it })

        assertTrue(parseFabricLog(content).resourcePacks.isEmpty())
    }

    @Test
    fun `no mods header gives no mods`() {
        val content = FabricLogs.unix(FabricLogs.lines.filterNot { it.startsWith("\t") || "Loading 7 mods" in it })

        assertTrue(parseFabricLog(content).mods.isEmpty())
    }

    @Test
    fun `windows line endings parse the same`() {
        assertEquals(
            parseFabricLog(FabricLogs.content),
            parseFabricLog(FabricLogs.windows(FabricLogs.lines)),
        )
    }

    @Test
    fun `non-fabric log is rejected`() {
        val content = "[12:00:00] [main/INFO]: ModLauncher running: args [--fml.neoForgeVersion, 21.1.77]\n"

        assertFailsWith<LogParseException> { parseFabricLog(content) }
    }

    @Test
    fun `empty file is rejected`() {
        assertFailsWith<LogParseException> { parseFabricLog("") }
    }
}
