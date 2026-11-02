package gosbry

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

// ответы регистрации, входа, GET /users/me, PATCH /users/me
@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val createdAt: String,
)

@Serializable
data class PatchMeRequest(
    val username: String? = null,
    val email: String? = null,
)

@Serializable
enum class Loader { FABRIC, NEOFORGE }

@Serializable
enum class IntegrityStatus { NOT_CHECKED, OK, SUSPICIOUS }

@Serializable
data class ModEntry(
    val id: String,
    val name: String? = null,
    val version: String,
)

@Serializable
data class ResourcePackEntry(val name: String)

@Serializable
data class IntegrityIssue(
    val code: String,
    val message: String,
)

@Serializable
data class IntegrityReport(
    val status: IntegrityStatus,
    val issues: List<IntegrityIssue>,
)

// промежуточная модель после парса
data class ParsedLog(
    val player: String?,
    val loader: Loader,
    val minecraftVersion: String?,
    val modCount: Int,
    val resourcePackCount: Int,
    val mods: List<ModEntry>,
    val resourcePacks: List<ResourcePackEntry>,
)

// краткая запись для списка истории
@Serializable
data class LogSummary(
    val id: String,
    val fileName: String,
    val player: String? = null,
    val loader: Loader,
    val minecraftVersion: String? = null,
    val modCount: Int,
    val resourcePackCount: Int,
    val integrityStatus: IntegrityStatus,
    val uploadedAt: String,
)

// те же поля, что у LogSummary, плюс списки
@Serializable
data class LogReport(
    val id: String,
    val fileName: String,
    val player: String? = null,
    val loader: Loader,
    val minecraftVersion: String? = null,
    val modCount: Int,
    val resourcePackCount: Int,
    val integrityStatus: IntegrityStatus,
    val uploadedAt: String,
    val mods: List<ModEntry>,
    val resourcePacks: List<ResourcePackEntry>,
    val integrity: IntegrityReport,
)

@Serializable
data class LogPage(
    val items: List<LogSummary>,
    val page: Int,
    val size: Int,
    val total: Long,
)
