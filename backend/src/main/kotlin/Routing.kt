package gosbry

import gosbry.services.parseLoader
import gosbry.services.unitLog
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.request.contentLength
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

// запас на границы multipart и остальные поля формы
private const val MULTIPART_OVERHEAD = 64L * 1024

// максимальная длина игрового ника
private const val MAX_PLAYER_LENGTH = 16

// имя в отчёте по-умолчанию
private const val DEFAULT_FILE_NAME = "latest.log"

// максимальная длина имени лога
private const val MAX_FILE_NAME_LENGTH = 255

fun Application.configureRouting() {
    // лимит задаётся только в application.yaml; если ключа нет, сервер упадёт при старте
    val maxFileSize = environment.config.property("spyglass.upload.maxFileSize").getString().toLong()

    routing {
        post("/api/v1/logs") {
            // если клиент честно прислал Content-Length, отказываем сразу, не читая тело
            val contentLength = call.request.contentLength()
            if (contentLength != null && contentLength > maxFileSize + MULTIPART_OVERHEAD) {
                throw FileTooLargeException(maxFileSize)
            }

            var uploadedFileName: String? = null
            var customFileName: String? = null
            var content: String? = null
            var player: String? = null
            var loader: Loader? = null

            call.receiveMultipart(formFieldLimit = maxFileSize + MULTIPART_OVERHEAD).forEachPart { part ->
                try {
                    when {
                        part is PartData.FileItem && part.name == "file" -> {
                            // filename="" считаем отсутствующим именем: тогда проверять расширение не на чем
                            val name = part.originalFileName?.takeIf { it.isNotBlank() }
                            if (name != null && !name.endsWith(".log", ignoreCase = true)) {
                                throw UnsupportedFileTypeException(name)
                            }
                            uploadedFileName = name
                            // читаем не больше лимита + 1 байт: если байт лишний, файл слишком большой
                            val bytes = part.provider().readBuffer(maxFileSize + 1).readByteArray()
                            if (bytes.size > maxFileSize) throw FileTooLargeException(maxFileSize)
                            content = bytes.decodeToString()
                        }

                        part is PartData.FormItem && part.name == "player" -> {
                            val value = part.value.trim()
                            if (value.length > MAX_PLAYER_LENGTH) {
                                throw PlayerTooLongException(MAX_PLAYER_LENGTH)
                            }
                            player = value
                        }

                        part is PartData.FormItem && part.name == "fileName" -> {
                            val value = part.value.trim()
                            if (value.length > MAX_FILE_NAME_LENGTH) {
                                throw FileNameTooLongException(MAX_FILE_NAME_LENGTH)
                            }
                            customFileName = value.takeIf { it.isNotEmpty() }
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
            val defaultPlayer = player?.takeIf { it.isNotEmpty() }
            // приоритет: имя из формы -> имя загруженного файла -> latest.log
            val fileName = customFileName ?: uploadedFileName ?: DEFAULT_FILE_NAME
            call.respond(HttpStatusCode.Created, unitLog(fileName, text, defaultPlayer))
        }
    }
}
