package org.myfastingapp.app.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.myfastingapp.app.domain.FastPlans
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.UserSettings
import org.myfastingapp.app.domain.WeightEntry
import org.myfastingapp.app.domain.WeightUnit
import java.time.Instant

class BackupCodec(
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    },
) {
    fun encode(
        settings: UserSettings,
        sessions: List<FastSession>,
        weights: List<WeightEntry>,
        exportedAtEpochMillis: Long,
    ): String {
        val envelope = BackupEnvelope(
            exportedAtEpochMillis = exportedAtEpochMillis,
            settings = SettingsBackup(
                defaultPlanId = settings.defaultPlanId,
                customFastingMinutes = settings.customFastingMinutes,
                remindersEnabled = settings.remindersEnabled,
                reminderLeadMinutes = settings.reminderLeadMinutes,
                weightUnit = settings.weightUnit.storageValue,
                targetWeightKg = settings.targetWeightKg,
            ),
            sessions = sessions.sortedBy { it.startEpochMillis }.map { it.toBackup() },
            weights = weights.sortedBy { it.recordedEpochMillis }.map { it.toBackup() },
        )
        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    fun decode(rawJson: String): ImportedBackup {
        val envelope = json.decodeFromString(BackupEnvelope.serializer(), rawJson)
        require(envelope.schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported backup schema: ${envelope.schemaVersion}"
        }
        val settings = UserSettings(
            defaultPlanId = envelope.settings.defaultPlanId,
            customFastingMinutes = envelope.settings.customFastingMinutes.coerceIn(30, 7 * 24 * 60),
            remindersEnabled = envelope.settings.remindersEnabled,
            reminderLeadMinutes = envelope.settings.reminderLeadMinutes.coerceIn(0, 24 * 60),
            weightUnit = WeightUnit.fromStorage(envelope.settings.weightUnit),
            targetWeightKg = envelope.settings.targetWeightKg?.coerceIn(20.0, 500.0),
        )
        val sessions = envelope.sessions.map { it.validated() }
        val weights = envelope.weights.map { it.validated() }
        return ImportedBackup(settings, sessions, weights)
    }

    fun encodeCsv(sessions: List<FastSession>): String {
        val header = listOf(
            "id",
            "plan_id",
            "plan_name",
            "target_seconds",
            "start_iso",
            "end_iso",
            "duration_seconds",
        ).joinToString(",")

        val rows = sessions
            .sortedBy { it.startEpochMillis }
            .map { session ->
                val end = session.endEpochMillis
                listOf(
                    session.id.toString(),
                    session.planId,
                    session.planName,
                    session.targetSeconds.toString(),
                    Instant.ofEpochMilli(session.startEpochMillis).toString(),
                    end?.let { Instant.ofEpochMilli(it).toString() }.orEmpty(),
                    end?.let { ((it - session.startEpochMillis) / 1_000L).coerceAtLeast(0L).toString() }.orEmpty(),
                ).joinToString(",") { it.csvEscape() }
            }
        return (listOf(header) + rows).joinToString("\n") + "\n"
    }

    private fun FastSession.toBackup(): SessionBackup = SessionBackup(
        planId = planId,
        planName = planName,
        targetSeconds = targetSeconds,
        startEpochMillis = startEpochMillis,
        endEpochMillis = endEpochMillis,
        createdEpochMillis = createdEpochMillis,
        updatedEpochMillis = updatedEpochMillis,
    )

    private fun WeightEntry.toBackup(): WeightBackup = WeightBackup(
        weightKg = weightKg,
        recordedEpochMillis = recordedEpochMillis,
        createdEpochMillis = createdEpochMillis,
    )

    private fun String.csvEscape(): String {
        val needsQuotes = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return this
        return "\"" + replace("\"", "\"\"") + "\""
    }
}

data class ImportedBackup(
    val settings: UserSettings,
    val sessions: List<SessionBackup>,
    val weights: List<WeightBackup>,
)

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val settings: SettingsBackup = SettingsBackup(),
    val sessions: List<SessionBackup> = emptyList(),
    val weights: List<WeightBackup> = emptyList(),
)

private const val CURRENT_SCHEMA_VERSION = 3

@Serializable
data class SettingsBackup(
    val defaultPlanId: String = FastPlans.DEFAULT_ID,
    val customFastingMinutes: Int = FastPlans.DEFAULT_CUSTOM_MINUTES,
    val remindersEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 15,
    val weightUnit: String = WeightUnit.LB.storageValue,
    val targetWeightKg: Double? = null,
)

@Serializable
data class SessionBackup(
    val planId: String,
    val planName: String,
    val targetSeconds: Long,
    val startEpochMillis: Long,
    val endEpochMillis: Long? = null,
    val createdEpochMillis: Long,
    val updatedEpochMillis: Long,
) {
    fun validated(): SessionBackup {
        require(planId.isNotBlank()) { "Session plan id is required." }
        require(planName.isNotBlank()) { "Session plan name is required." }
        require(targetSeconds in (30 * 60L)..(7 * 24 * 60 * 60L)) { "Invalid target duration." }
        require(startEpochMillis > 0L) { "Invalid start time." }
        endEpochMillis?.let { require(it > startEpochMillis) { "End time must be after start time." } }
        return this
    }
}

@Serializable
data class WeightBackup(
    val weightKg: Double,
    val recordedEpochMillis: Long,
    val createdEpochMillis: Long,
) {
    fun validated(): WeightBackup {
        require(weightKg in 20.0..500.0) { "Invalid weight." }
        require(recordedEpochMillis > 0L) { "Invalid weight date." }
        require(createdEpochMillis > 0L) { "Invalid weight created date." }
        return this
    }
}
