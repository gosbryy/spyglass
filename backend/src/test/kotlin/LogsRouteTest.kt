package gosbry

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.*

class LogsRouteTest {

    private suspend fun HttpClient.uploadLog(
        content: String?,
        fileName: String? = "latest.log",
        player: String? = null,
        loader: String? = null,
        customFileName: String? = null,
    ): HttpResponse = submitFormWithBinaryData(
        url = "/api/v1/logs",
        formData = formData {
            if (content != null) {
                val disposition = if (fileName != null) "filename=\"$fileName\"" else ""
                append(
                    "file",
                    content.toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, disposition)
                    },
                )
            }
            if (player != null) append("player", player)
            if (loader != null) append("loader", loader)
            if (customFileName != null) append("fileName", customFileName)
        },
    )

    @Test
    fun `uploaded log is parsed`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, fileName = "client.log")

        assertEquals(HttpStatusCode.Created, response.status)
        val report = Json.decodeFromString<LogReport>(response.bodyAsText())
        assertEquals("client.log", report.fileName)
        assertEquals("Steve", report.player)
        assertEquals(Loader.FABRIC, report.loader)
        assertEquals(listOf("fabric-api", "sodium"), report.mods.map { it.id })
        assertEquals(IntegrityStatus.OK, report.integrityStatus)
    }

    @Test
    fun `player from form is used when log has none`() = testApplication {
        configure()
        val content = FabricLogs.unix(FabricLogs.withoutLine("[18:31:20]"))
        val response = client.uploadLog(content, player = "  Alex ")

        assertEquals("Alex", Json.decodeFromString<LogReport>(response.bodyAsText()).player)
    }

    @Test
    fun `blank player from form is ignored`() = testApplication {
        configure()
        val content = FabricLogs.unix(FabricLogs.withoutLine("[18:31:20]"))
        val response = client.uploadLog(content, player = "   ")

        assertNull(Json.decodeFromString<LogReport>(response.bodyAsText()).player)
    }

    @Test
    fun `real tampered log is flagged`() = testApplication {
        configure()
        val response = client.uploadLog(RealLogs.fabricTampered)

        assertEquals(HttpStatusCode.Created, response.status)
        val report = Json.decodeFromString<LogReport>(response.bodyAsText())
        assertEquals(IntegrityStatus.SUSPICIOUS, report.integrityStatus)
    }

    // без filename ktor считает часть обычным полем формы, а не файлом
    @Test
    fun `file without filename is treated as missing`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, fileName = null)

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `missing file is bad request`() = testApplication {
        configure()
        val response = client.uploadLog(content = null, player = "Steve")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("FILE_MISSING", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    @Test
    fun `non-log file is unsupported media type`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, fileName = "latest.txt")

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertEquals("UNSUPPORTED_FILE_TYPE", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    @Test
    fun `loader from form is case insensitive`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, loader = "fabric")

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(Loader.FABRIC, Json.decodeFromString<LogReport>(response.bodyAsText()).loader)
    }

    @Test
    fun `blank loader from form is ignored`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, loader = "  ")

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `unknown loader is bad request`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, loader = "quilt")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("BAD_REQUEST", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    @Test
    fun `neoforge loader is not supported yet`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, loader = "NEOFORGE")

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals("UNSUPPORTED_LOADER", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    // маленький лимит, чтобы не гонять в тестах файлы по 10 МБ
    private val testLimit = 64 * 1024

    private fun ApplicationTestBuilder.configureWithLimit() = configure(
        overrides = { put("spyglass.upload.maxFileSize", testLimit.toString()) },
    )

    @Test
    fun `file over limit is payload too large`() = testApplication {
        configureWithLimit()
        // Content-Length укладывается в запас, поэтому срабатывает проверка при чтении файла
        val response = client.uploadLog("a".repeat(testLimit + 1))

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertEquals("FILE_TOO_LARGE", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    @Test
    fun `request with huge content length is rejected before reading`() = testApplication {
        configureWithLimit()
        // больше лимита + запаса на multipart (64 КБ), отказ по заголовку Content-Length
        val response = client.uploadLog("a".repeat(testLimit + 64 * 1024 + 1))

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertEquals("FILE_TOO_LARGE", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    @Test
    fun `file exactly at limit is accepted`() = testApplication {
        configureWithLimit()
        // дополняем по байтам, а не символам: в логе есть многобайтовый '§'
        val padding = testLimit - FabricLogs.content.toByteArray().size
        val response = client.uploadLog(FabricLogs.content + "\n".repeat(padding))

        assertEquals(HttpStatusCode.Created, response.status)
    }

    // лог без строки "Setting user", чтобы в отчёт попал ник из формы
    private val logWithoutPlayer = FabricLogs.unix(FabricLogs.withoutLine("[18:31:20]"))

    @Test
    fun `player at max length is accepted`() = testApplication {
        configure()
        val nick = "a".repeat(16)
        val response = client.uploadLog(logWithoutPlayer, player = nick)

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(nick, Json.decodeFromString<LogReport>(response.bodyAsText()).player)
    }

    @Test
    fun `player over max length is bad request`() = testApplication {
        configure()
        val response = client.uploadLog(logWithoutPlayer, player = "a".repeat(17))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("PLAYER_TOO_LONG", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }

    @Test
    fun `surrounding spaces do not count towards player length`() = testApplication {
        configure()
        val nick = "a".repeat(16)
        val response = client.uploadLog(logWithoutPlayer, player = "   $nick   ")

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(nick, Json.decodeFromString<LogReport>(response.bodyAsText()).player)
    }

    @Test
    fun `file name from form is used`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, fileName = "client.log", customFileName = "  мой лог  ")

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("мой лог", Json.decodeFromString<LogReport>(response.bodyAsText()).fileName)
    }

    @Test
    fun `blank file name from form falls back to uploaded file name`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, fileName = "client.log", customFileName = "   ")

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("client.log", Json.decodeFromString<LogReport>(response.bodyAsText()).fileName)
    }

    @Test
    fun `empty uploaded file name falls back to latest log`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, fileName = "")

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("latest.log", Json.decodeFromString<LogReport>(response.bodyAsText()).fileName)
    }

    // имя из формы не обходит проверку расширения у самого файла
    @Test
    fun `file name from form does not bypass log extension check`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, fileName = "latest.txt", customFileName = "latest.log")

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `file name at max length is accepted`() = testApplication {
        configure()
        val name = "a".repeat(255)
        val response = client.uploadLog(FabricLogs.content, customFileName = "  $name  ")

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(name, Json.decodeFromString<LogReport>(response.bodyAsText()).fileName)
    }

    @Test
    fun `file name over max length is bad request`() = testApplication {
        configure()
        val response = client.uploadLog(FabricLogs.content, customFileName = "a".repeat(256))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("FILE_NAME_TOO_LONG", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }
}
