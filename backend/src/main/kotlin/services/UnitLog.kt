package gosbry.services

import gosbry.LogReport
import java.time.Instant
import java.util.UUID

// пока поддерживается только fabric
fun unitLog(fileName: String, content: String): LogReport {
    val parsed = parseFabricLog(fileName, content)
    val integrity = check(parsed, content)

    return LogReport(
        id = UUID.randomUUID().toString(),
        fileName = fileName,
        player = parsed.player,
        loader = parsed.loader,
        minecraftVersion = parsed.minecraftVersion,
        modCount = parsed.modCount,
        resourcePackCount = parsed.resourcePackCount,
        integrityStatus = integrity.status,
        uploadedAt = Instant.now().toString(),
        mods = parsed.mods,
        resourcePacks = parsed.resourcePacks,
        integrity = integrity,
    )
}
