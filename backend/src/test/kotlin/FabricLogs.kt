package gosbry

// синтетический лог fabric: 7 уникальных модов в дереве, из них 2 пользовательских верхнего уровня
object FabricLogs {
    val lines = listOf(
        "[18:31:15] [main/INFO]: Loading Minecraft 1.21.1 with Fabric Loader 0.16.10",
        "[18:31:16] [main/INFO]: Loading 7 mods:",
        "\t- fabric-api 0.110.0+1.21.1",
        "\t   |-- fabric-api-base 0.4.42+6573ed8c19",
        "\t   \\-- fabric-rendering-v1 5.0.5+df16efd319",
        "\t- fabricloader 0.16.10",
        "\t- java 21",
        "\t- minecraft 1.21.1",
        "\t- sodium 0.6.13+mc1.21.1",
        "[18:31:20] [Render thread/INFO]: Setting user: Steve",
        "[18:31:25] [Render thread/INFO]: Reloading ResourceManager: vanilla, fabric, fabric-api, sodium, file/§6Faithful 32x, file/Clean UI",
        "[18:31:40] [Render thread/INFO]: Reloading ResourceManager: vanilla, fabric, fabric-api, sodium",
    )

    // как пишет игра на linux/macos
    val content = unix(lines)

    fun unix(lines: List<String>) = lines.joinToString("\n", postfix = "\n")

    // как пишет игра на windows: запись кончается CRLF, переносы внутри записи остаются LF
    fun windows(lines: List<String>) = buildString {
        lines.forEachIndexed { i, line ->
            append(line)
            val next = lines.getOrNull(i + 1)
            append(if (next == null || next.startsWith("[")) "\r\n" else "\n")
        }
    }

    fun withoutLine(prefix: String) = lines.filterNot { it.startsWith(prefix) }

    fun replacing(prefix: String, replacement: String) =
        lines.map { if (it.startsWith(prefix)) replacement else it }
}
