package gosbry

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.*

class ServerTest {

    @Test
    fun `unknown route returns json error`() = testApplication {
        configure()
        val response = client.get("/no-such-route")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("NOT_FOUND", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
    }
}
