package gosbry.services

import gosbry.IntegrityReport
import gosbry.IntegrityStatus
import gosbry.Loader
import gosbry.LogReport
import gosbry.ModEntry
import gosbry.ResourcePackEntry
import java.time.Instant
import java.util.UUID

// [18:31:15] [main/INFO]: Loading Minecraft 26.1.2 with Fabric Loader 0.19.3
private val FABRIC_HEADER = Regex("""Loading Minecraft (\S+) with Fabric Loader (\S+)""")

// [18:31:16] [main/INFO]: Loading 260 mods:
private val MODS_HEADER = Regex("""^\[[^]]*] \[[^]]*]: Loading \d+ mods:$""")

// "\t- sodium 0.6.0+mc26.1" — мод верхнего уровня; вложенные (jar-in-jar) идут с |-- или \--
private val TOP_LEVEL_MOD = Regex("""^\t- (\S+) (\S+)$""")

// [18:31:35] [Render thread/INFO]: Setting user: fishbry
private val SETTING_USER = Regex("""Setting user: (\S+)""")

private val RESOURCE_MANAGER = Regex("""Reloading ResourceManager: (.*)$""")

private val FORMATTING_CODE = Regex("§.")

// не моды в привычном смысле, а сама платформа
private val PLATFORM_MOD_IDS = setOf("minecraft", "java", "fabricloader")

class LogParseException(message: String) : Exception(message)

fun parseFabricLog(fileName: String, content: String): LogReport {
    val lines = content.lines()

    val header = lines.firstNotNullOfOrNull { FABRIC_HEADER.find(it) }
        ?: throw LogParseException("Not a Fabric log: loader header not found")

    val mods = parseMods(lines)
    val resourcePacks = parseResourcePacks(lines)

    return LogReport(
        id = UUID.randomUUID().toString(),
        fileName = fileName,
        player = lines.firstNotNullOfOrNull { SETTING_USER.find(it) }?.groupValues?.get(1),
        loader = Loader.FABRIC,
        minecraftVersion = header.groupValues[1],
        modCount = mods.size,
        resourcePackCount = resourcePacks.size,
        integrityStatus = IntegrityStatus.NOT_CHECKED,
        uploadedAt = Instant.now().toString(),
        mods = mods,
        resourcePacks = resourcePacks,
        integrity = IntegrityReport(IntegrityStatus.NOT_CHECKED, emptyList()),
    )
}

// берём только моды верхнего уровня: вложенные библиотеки пользователь сам не ставил
private fun parseMods(lines: List<String>): List<ModEntry> {
    val start = lines.indexOfFirst { MODS_HEADER.matches(it) }
    if (start == -1) return emptyList()

    return lines.asSequence()
        .drop(start + 1)
        .takeWhile { it.startsWith("\t") }
        .mapNotNull { TOP_LEVEL_MOD.matchEntire(it) }
        .map { ModEntry(id = it.groupValues[1], version = it.groupValues[2]) }
        .filter { it.id !in PLATFORM_MOD_IDS }
        .toList()
}

// первая перезагрузка ресурсов перечисляет все включённые паки; пользовательские имеют префикс file/
private fun parseResourcePacks(lines: List<String>): List<ResourcePackEntry> {
    val packs = lines.firstNotNullOfOrNull { RESOURCE_MANAGER.find(it) }?.groupValues?.get(1)
        ?: return emptyList()

    return packs.split(", ")
        .filter { it.startsWith("file/") }
        .map { ResourcePackEntry(it.removePrefix("file/").replace(FORMATTING_CODE, "")) }
}
