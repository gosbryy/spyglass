package gosbry

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.*

class LogsRouteTest {

    private suspend fun HttpClient.uploadLog(
        content: String?,
        fileName: String? = "latest.log",
        player: String? = null,
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
}
