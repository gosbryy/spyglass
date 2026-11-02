package gosbry.services

import gosbry.Loader
import gosbry.UnsupportedLoaderException
import io.ktor.server.plugins.BadRequestException

fun parseLoader(value: String): Loader? {
    val raw = value.trim().takeIf { it.isNotEmpty() } ?: return null
    val loader = Loader.entries.firstOrNull {
        it.name.equals(raw, ignoreCase = true)
    } ?: throw BadRequestException("Unknown loader: $raw")
    if (loader == Loader.NEOFORGE) throw UnsupportedLoaderException(loader.name)
    return loader
}
