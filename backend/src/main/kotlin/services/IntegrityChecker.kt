package gosbry.services

import gosbry.IntegrityIssue
import gosbry.IntegrityReport
import gosbry.IntegrityStatus
import gosbry.Loader
import gosbry.ParsedLog

// [18:31:16] [main/INFO]: Loading 260 mods:
private val MODS_COUNT_HEADER = Regex("""^\[[^]]*] \[[^]]*]: Loading (\d+) mods:$""")

// любой мод из дерева: "\t- sodium 0.8.12", "\t   |-- lattice 2.0.1", "\t   |    \-- jankson 1.2.3"
private val ANY_MOD = Regex("""^\t[ |]*(?:- |\|-- |\\-- )(\S+) \S+$""")

private val RESOURCE_MANAGER = Regex("""Reloading ResourceManager: (.*)$""")

private val TIMESTAMP = Regex("""^\[(\d{2}):(\d{2}):(\d{2})]""")

// паки, которые есть всегда и не принадлежат ни одному моду
private val BUILTIN_PACKS = setOf("vanilla", "fabric", "programmer_art", "high_contrast")

// больший откат времени считаем переходом через полночь
private const val MAX_BACKWARD_SECONDS = 12 * 60 * 60

fun check(parsed: ParsedLog, content: String): IntegrityReport {
    val issues = when (parsed.loader) {
        Loader.FABRIC -> checkFabric(content)

        // проверок для neoforge пока нет
        Loader.NEOFORGE -> return IntegrityReport(IntegrityStatus.NOT_CHECKED, emptyList())
    }
    val status = if (issues.isEmpty()) IntegrityStatus.OK else IntegrityStatus.SUSPICIOUS
    return IntegrityReport(status, issues)
}

private fun checkFabric(content: String): List<IntegrityIssue> {
    val rawLines = content.split("\n")
    val lines = rawLines.map { it.removeSuffix("\r") }
    val modIds = allModIds(lines)

    return listOfNotNull(
        checkModCount(lines, modIds),
        checkResourcePacks(lines, modIds),
        checkLineEndings(rawLines),
        checkTimestamps(lines),
    )
}

private fun allModIds(lines: List<String>): Set<String> {
    val start = lines.indexOfFirst { MODS_COUNT_HEADER.matches(it) }
    if (start == -1) return emptySet()

    return lines.asSequence()
        .drop(start + 1)
        .takeWhile { it.startsWith("\t") }
        .mapNotNull { ANY_MOD.matchEntire(it)?.groupValues?.get(1) }
        .toSet()
}

// лоадер пишет число уникальных модов, включая вложенные; удалённая из списка строка его не меняет
private fun checkModCount(lines: List<String>, modIds: Set<String>): IntegrityIssue? {
    val declared = lines.firstNotNullOfOrNull { MODS_COUNT_HEADER.matchEntire(it) }
        ?.groupValues?.get(1)?.toInt()
        ?: return IntegrityIssue("MOD_LIST_MISSING", "Mod list header not found")

    if (declared == modIds.size) return null
    return IntegrityIssue(
        "MOD_COUNT_MISMATCH",
        "Header declares $declared mods, but the list contains ${modIds.size}",
    )
}

// моды с ресурсами попадают в ResourceManager под своим id: пак без мода значит, что мод вырезали из списка;
// file/ — пользовательские паки, server/ — пак, присланный сервером
private fun checkResourcePacks(lines: List<String>, modIds: Set<String>): IntegrityIssue? {
    if (modIds.isEmpty()) return null

    val orphans = lines.asSequence()
        .mapNotNull { RESOURCE_MANAGER.find(it)?.groupValues?.get(1) }
        .flatMap { it.split(", ") }
        .filterNot { it.startsWith("file/") || it.startsWith("server/") || it in BUILTIN_PACKS }
        .filterNot { isProvidedByMod(it, modIds) }
        .toSortedSet()

    if (orphans.isEmpty()) return null
    return IntegrityIssue(
        "UNKNOWN_MOD_RESOURCES",
        "Resources loaded for mods missing from the mod list: ${orphans.joinToString()}",
    )
}

// "continuity:default" и "axiom_translations" — паки, которые добавляет сам мод
private fun isProvidedByMod(pack: String, modIds: Set<String>): Boolean =
    pack in modIds || pack.substringBefore(':') in modIds || pack.substringBefore('_') in modIds

// на Windows игра пишет записи с CRLF, а переносы внутри многострочных записей остаются LF;
// сплошной CRLF значит, что файл пересохранили в редакторе
private fun checkLineEndings(rawLines: List<String>): IntegrityIssue? {
    val terminated = rawLines.dropLast(1)
    val crlf = terminated.count { it.endsWith("\r") }
    if (crlf == 0 || crlf < terminated.size) return null

    val hasMultilineEntries = terminated.any { it.isNotEmpty() && !TIMESTAMP.containsMatchIn(it) }
    if (!hasMultilineEntries) return null

    return IntegrityIssue(
        "LINE_ENDINGS_NORMALIZED",
        "All lines end with CRLF, including continuation lines; the file was likely re-saved in an editor",
    )
}

private fun checkTimestamps(lines: List<String>): IntegrityIssue? {
    var previous: Int? = null
    lines.forEachIndexed { index, line ->
        val (h, m, s) = TIMESTAMP.find(line)?.destructured ?: return@forEachIndexed
        val current = h.toInt() * 3600 + m.toInt() * 60 + s.toInt()
        val back = previous?.minus(current) ?: 0
        if (back in 1..MAX_BACKWARD_SECONDS) {
            return IntegrityIssue(
                "TIMESTAMP_ORDER",
                "Timestamp goes backwards at line ${index + 1}",
            )
        }
        previous = current
    }
    return null
}
