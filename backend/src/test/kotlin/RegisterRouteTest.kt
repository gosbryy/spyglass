package gosbry

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RegisterRouteTest {
    private fun ApplicationTestBuilder.configureRegistration() {
        environment {
            config = MapApplicationConfig("spyglass.upload.maxFileSize" to "5242880")
        }
        application {
            configureSerialization()
            configureStatusPages()
            configureRouting()
        }

    }

    private suspend fun HttpClient.register(email: String = "steve@example.com", password: String = "password123") =
        post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest("Steve", email, password)))
        }

    @Test
    fun `registration validates the trimmed email`() = testApplication {
        configureRegistration()

        val invalidEmails = listOf(
            "   ", "steve.example.com", "steve@@example.com", "@example.com", "steve@",
            "ste ve@example.com", "steve@exam\tple.com", "steve@example",
            "steve@example..com", "steve@.example.com", "steve@example.com.",
        )
        for (email in invalidEmails) {
            val response = client.register(email)
            assertEquals(HttpStatusCode.BadRequest, response.status, email)
            assertEquals("INVALID_EMAIL", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code, email)
        }

        for (email in listOf("steve@example.com", " \tsteve@sub.example.com \n")) {
            // Регистрация пока не создаёт пользователя; проверяем только, что валидация пропустила адрес.
            val response = client.register(email)
            assertNotEquals(HttpStatusCode.BadRequest, response.status, email)
        }
    }

    @Test
    fun `registration enforces the password length from the contract`() = testApplication {
        configureRegistration()

        for (password in listOf("a".repeat(7), "a".repeat(129), "😀".repeat(7))) {
            val response = client.register(password = password)
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("INVALID_PASSWORD_LENGTH", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code)
        }

        for (password in listOf("a".repeat(8), "a".repeat(128), "😀".repeat(128), " aaaaaa ")) {
            val response = client.register(password = password)
            assertNotEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.status.value < 500, "Unexpected response: ${response.status}")
        }
    }
}
