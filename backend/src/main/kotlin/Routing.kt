package gosbry

import gosbry.routes.authRoutes
import gosbry.routes.logRoutes
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.*

fun Application.configureRouting() {
    // лимит задаётся только в application.yaml; если ключа нет, сервер упадёт при старте
    val maxFileSize = environment.config.property("spyglass.upload.maxFileSize").getString().toLong()

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")
        logRoutes(maxFileSize)
        authRoutes()
    }
}
