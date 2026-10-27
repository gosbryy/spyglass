package gosbry.services

import gosbry.LogReport
import java.time.Instant
import java.util.UUID

// пока поддерживается только fabric
// defaultPlayer - ник из формы, подставляется, если в логе нет "Setting user"
fun unitLog(fileName: String, content: String, defaultPlayer: String? = null): LogReport {
    val parsed = parseFabricLog(fileName, content)
    val integrity = check(parsed, content)
    // TODO: предупреждать, если ник в логе не совпадает с defaultPlayer или лоадер не совпадает с указанным в форме

    return LogReport(
        id = UUID.randomUUID().toString(),
        fileName = fileName,
        player = parsed.player ?: defaultPlayer,
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
