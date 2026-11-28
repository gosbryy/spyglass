package gosbry.routes

import gosbry.ErrorResponse
import gosbry.InvalidEmailException
import gosbry.InvalidPasswordLengthException
import gosbry.InvalidPlayerLengthException
import gosbry.RegisterRequest
import gosbry.UserResponse
import gosbry.Users
import gosbry.services.hashPassword
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private const val MIN_USERNAME_LENGTH = 3
private const val MAX_USERNAME_LENGTH = 16
private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 128

internal fun Route.authRoutes() {
    post("/api/v1/auth/register") {
        val request = call.receive<RegisterRequest>()

        if (request.username.length !in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH) {
            throw InvalidPlayerLengthException(MAX_USERNAME_LENGTH, MIN_USERNAME_LENGTH)
        }
        val passwordLength = request.password.codePointCount(0, request.password.length)
        if (passwordLength !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH) {
            throw InvalidPasswordLengthException(MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH)
        }

        val email = request.email.trim()
        val at = email.indexOf('@')
        if (email.any(Char::isWhitespace) || at <= 0 || at == email.lastIndex || at != email.lastIndexOf('@')) {
            throw InvalidEmailException("Invalid email address")
        }

        val domainParts = email.substring(at + 1).split('.')
        if (domainParts.size < 2 || domainParts.any { it.isEmpty() }) {
            throw InvalidEmailException("Invalid email domain")
        }
        val hashedPassword = hashPassword(request.password)
        val user = try {
            withContext(Dispatchers.IO) {
                transaction {
                    val row = Users.insertReturning(
                        returning = listOf(Users.id, Users.username, Users.email, Users.createdAt),
                    ) { insert ->
                        insert[Users.username] = request.username
                        insert[Users.email] = email
                        insert[Users.passwordHash] = hashedPassword
                    }.single()

                    UserResponse(
                        id = row[Users.id],
                        username = row[Users.username],
                        email = row[Users.email],
                        createdAt = row[Users.createdAt].toString(),
                    )
                }
            }
        } catch (e: ExposedSQLException) {
            if (e.sqlState != "23505") throw e

            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse("USER_ALREADY_EXISTS", "Username or email is already taken"),
            )
            return@post
        }
        call.respond(HttpStatusCode.Created, user)
    }
}
