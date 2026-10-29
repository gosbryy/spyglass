package gosbry

import gosbry.services.UnsupportedFileTypeException
import gosbry.services.parseLoader
import gosbry.services.unitLog
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readBuffer
import io.ktor.utils.io.readText

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
        post("/api/v1/logs") {
            var fileName: String? = null
            var content: String? = null
            var player: String? = null
            var loader: Loader? = null

            call.receiveMultipart().forEachPart { part ->
                try {
                    when {
                        part is PartData.FileItem && part.name == "file" -> {
                            fileName = part.originalFileName
                            if (fileName == null || !fileName!!.endsWith(".log", ignoreCase = true)) {
                                throw UnsupportedFileTypeException(fileName)
                            }
                            content = part.provider().readBuffer().readText()
                        }

                        part is PartData.FormItem && part.name == "player" -> {
                            player = part.value
                        }

                        part is PartData.FormItem && part.name == "loader" -> {
                            loader = parseLoader(part.value)
                        }
                    }
                } finally {
                    part.release()
                }
            }

            val text = content ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("FILE_MISSING", "Field 'file' is required"),
            )
            val defaultPlayer = player?.trim()?.takeIf { it.isNotEmpty() }
            call.respond(HttpStatusCode.Created, unitLog(fileName ?: "latest.log", text, defaultPlayer))
        }
    }
}
