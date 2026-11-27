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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class RegisterRouteTest {
    private fun ApplicationTestBuilder.configureRegistration() {
        environment {
            config = MapApplicationConfig(
                "spyglass.upload.maxFileSize" to "5242880",
                "spyglass.db.url" to (System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/spyglass"),
                "spyglass.db.user" to (System.getenv("DB_USER") ?: "spyglass"),
                "spyglass.db.password" to (System.getenv("DB_PASSWORD") ?: "spyglass"),
            )
        }
        application {
            configureSerialization()
            configureDatabase()
            configureStatusPages()
            configureRouting()
        }
    }

    private fun uniqueUsername() = "u${UUID.randomUUID().toString().replace("-", "").take(15)}"

    private suspend fun HttpClient.register(
        username: String = uniqueUsername(),
        email: String = "$username@example.com",
        password: String = "password123",
    ) = post("/api/v1/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(RegisterRequest(username, email, password)))
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
            val response = client.register(email = email)
            assertEquals(HttpStatusCode.BadRequest, response.status, email)
            assertEquals("INVALID_EMAIL", Json.decodeFromString<ErrorResponse>(response.bodyAsText()).code, email)
        }

        for (domain in listOf("example.com", "sub.example.com")) {
            val email = "${uniqueUsername()}@$domain"
            val response = client.register(email = " \t$email \n")
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(email, Json.decodeFromString<UserResponse>(response.bodyAsText()).email)
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
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun `registration rejects duplicate usernames and emails`() = testApplication {
        configureRegistration()

        val username = uniqueUsername()
        val email = "$username@example.com"
        assertEquals(HttpStatusCode.Created, client.register(username, email).status)

        val duplicateUsername = client.register(username = username, email = "$username@other.example.com")
        assertEquals(HttpStatusCode.Conflict, duplicateUsername.status)
        assertEquals("USER_ALREADY_EXISTS", Json.decodeFromString<ErrorResponse>(duplicateUsername.bodyAsText()).code)

        val duplicateEmail = client.register(email = email)
        assertEquals(HttpStatusCode.Conflict, duplicateEmail.status)
        assertEquals("USER_ALREADY_EXISTS", Json.decodeFromString<ErrorResponse>(duplicateEmail.bodyAsText()).code)
    }
}
