package gosbry

// настоящие логи из src/test/resources/logs, обезличены: без чата, IP и путей с именем пользователя
object RealLogs {
    // windows, 261 мод, проверок не проваливает
    val fabricClean by lazy { read("fabric-clean.log") }

    // из списка вырезан мод carmona, файл пересохранён в редакторе (сплошной CRLF)
    val fabricTampered by lazy { read("fabric-tampered.log") }

    // lunar client: строки "Loading Minecraft ... with Fabric Loader" нет
    val lunar by lazy { read("lunar.log") }

    private fun read(name: String): String =
        requireNotNull(RealLogs::class.java.getResource("/logs/$name")) { "fixture $name not found" }.readText()
}
