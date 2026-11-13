package gosbry

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.UuidColumnType
import org.jetbrains.exposed.v1.javatime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

// схему создаёт Flyway (V1__init.sql), здесь только маппинг для запросов

// то же, что DEFAULT gen_random_uuid() в миграции
private fun genRandomUuid() = CustomFunction("gen_random_uuid", UuidColumnType())

object Users : Table("users") {
    val id = uuid("id").defaultExpression(genRandomUuid())
    val username = text("username")
    val email = text("email")
    val passwordHash = text("password_hash")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)

    override val primaryKey = PrimaryKey(id)
}

object Logs : Table("logs") {
    val id = uuid("id").defaultExpression(genRandomUuid())
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val fileName = text("file_name")
    val filePath = text("file_path")
    val player = text("player").nullable()
    val loader = enumerationByName<Loader>("loader", 16)
    val minecraftVersion = text("minecraft_version").nullable()
    val modCount = integer("mod_count")
    val resourcePackCount = integer("resource_pack_count")
    val integrityStatus = enumerationByName<IntegrityStatus>("integrity_status", 16)
    val integrityIssues = jsonb<List<IntegrityIssue>>("integrity_issues", Json)
    val checkerVersion = integer("checker_version").nullable()
    val uploadedAt = timestampWithTimeZone("uploaded_at").defaultExpression(CurrentTimestampWithTimeZone)
    val checkedAt = timestampWithTimeZone("checked_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object LogMods : Table("log_mods") {
    val logId = reference("log_id", Logs.id, onDelete = ReferenceOption.CASCADE)
    val modId = text("mod_id")
    val name = text("name").nullable()
    val version = text("version")

    override val primaryKey = PrimaryKey(logId, modId)
}

object LogResourcePacks : Table("log_resource_packs") {
    val logId = reference("log_id", Logs.id, onDelete = ReferenceOption.CASCADE)
    val name = text("name")

    override val primaryKey = PrimaryKey(logId, name)
}
