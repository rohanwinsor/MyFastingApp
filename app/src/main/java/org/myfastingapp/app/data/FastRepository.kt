package org.myfastingapp.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.myfastingapp.app.backup.BackupCodec
import org.myfastingapp.app.domain.FastPlan
import org.myfastingapp.app.domain.FastPlans
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.UserSettings
import org.myfastingapp.app.domain.WeightEntry

class FastRepository(
    private val dao: FastSessionDao,
    private val weightDao: WeightEntryDao,
    private val settingsStore: SettingsStore,
    private val backupCodec: BackupCodec,
) {
    val sessions: Flow<List<FastSession>> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .distinctUntilChanged()
    val activeSession: Flow<FastSession?> = dao.observeActive()
        .map { it?.toDomain() }
        .distinctUntilChanged()
    val weights: Flow<List<WeightEntry>> = weightDao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .distinctUntilChanged()

    suspend fun startFast(plan: FastPlan, nowEpochMillis: Long = System.currentTimeMillis()): Long {
        check(dao.activeCount() == 0) { "A fast is already active." }
        val session = FastSessionEntity(
            planId = plan.id,
            planName = plan.name,
            targetSeconds = plan.fastingMinutes * 60L,
            startEpochMillis = nowEpochMillis,
            endEpochMillis = null,
            createdEpochMillis = nowEpochMillis,
            updatedEpochMillis = nowEpochMillis,
        )
        return dao.insert(session)
    }

    suspend fun endActiveFast(nowEpochMillis: Long = System.currentTimeMillis()) {
        val active = dao.getActive() ?: return
        val safeEnd = nowEpochMillis.coerceAtLeast(active.startEpochMillis + 60_000L)
        dao.update(active.copy(endEpochMillis = safeEnd, updatedEpochMillis = nowEpochMillis))
    }

    suspend fun editFast(
        id: Long,
        startEpochMillis: Long,
        endEpochMillis: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) {
        require(endEpochMillis > startEpochMillis) { "End time must be after start time." }
        val current = dao.getById(id) ?: return
        val isActive = current.endEpochMillis == null
        val targetSeconds = ((endEpochMillis - startEpochMillis) / 1_000L).coerceAtLeast(1L)
        require(targetSeconds >= 30 * 60L) { "Fast target must be at least 30 minutes." }
        require(startEpochMillis <= nowEpochMillis) { "Start time cannot be in the future." }
        val overlapEnd = if (isActive) Long.MAX_VALUE else endEpochMillis
        require(dao.overlappingCount(startEpochMillis, overlapEnd, id) == 0) {
            "This session overlaps another fast."
        }
        val builtInTargetSeconds = FastPlans.builtInById(current.planId)?.fastingMinutes?.times(60L)
        val activeTargetChanged = isActive && targetSeconds != (builtInTargetSeconds ?: current.targetSeconds)
        dao.update(
            current.copy(
                planId = if (activeTargetChanged) FastPlans.CUSTOM_ID else current.planId,
                planName = if (activeTargetChanged) "Custom" else FastPlans.builtInById(current.planId)?.name ?: current.planName,
                targetSeconds = if (isActive) targetSeconds else current.targetSeconds,
                startEpochMillis = startEpochMillis,
                endEpochMillis = if (isActive) null else endEpochMillis,
                updatedEpochMillis = nowEpochMillis,
            ),
        )
    }

    suspend fun repairActivePlanTarget(nowEpochMillis: Long = System.currentTimeMillis()) {
        val active = dao.getActive() ?: return
        val plan = FastPlans.builtInById(active.planId) ?: return
        val targetSeconds = plan.fastingMinutes * 60L
        if (active.targetSeconds == targetSeconds && active.planName == plan.name) return
        dao.update(
            active.copy(
                planName = plan.name,
                targetSeconds = targetSeconds,
                updatedEpochMillis = nowEpochMillis,
            ),
        )
    }

    suspend fun addCompletedFast(
        planName: String,
        targetSeconds: Long,
        startEpochMillis: Long,
        endEpochMillis: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Long {
        require(endEpochMillis > startEpochMillis) { "End time must be after start time." }
        require(targetSeconds >= 30 * 60L) { "Fast target must be at least 30 minutes." }
        require(endEpochMillis <= nowEpochMillis + 60_000L) { "Finished fasts cannot end in the future." }
        require(dao.overlappingCount(startEpochMillis, endEpochMillis) == 0) {
            "This session overlaps another fast."
        }
        return dao.insert(
            FastSessionEntity(
                planId = "manual",
                planName = planName.ifBlank { "Manual fast" },
                targetSeconds = targetSeconds,
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                createdEpochMillis = nowEpochMillis,
                updatedEpochMillis = nowEpochMillis,
            ),
        )
    }

    suspend fun deleteFast(id: Long) {
        dao.deleteById(id)
    }

    suspend fun allSessionsSnapshot(): List<FastSession> = dao.getAllSnapshot().map { it.toDomain() }
    suspend fun allWeightsSnapshot(): List<WeightEntry> = weightDao.getAllSnapshot().map { it.toDomain() }

    suspend fun addWeight(weightKg: Double, recordedEpochMillis: Long = System.currentTimeMillis()) {
        require(weightKg in 20.0..500.0) { "Enter a weight between 20 kg and 500 kg." }
        val now = System.currentTimeMillis()
        weightDao.insert(
            WeightEntryEntity(
                weightKg = weightKg,
                recordedEpochMillis = recordedEpochMillis,
                createdEpochMillis = now,
            ),
        )
    }

    suspend fun deleteWeight(id: Long) {
        weightDao.deleteById(id)
    }

    suspend fun exportBackupJson(settings: UserSettings): String {
        return backupCodec.encode(settings, allSessionsSnapshot(), allWeightsSnapshot(), System.currentTimeMillis())
    }

    suspend fun exportSessionsCsv(): String {
        return backupCodec.encodeCsv(allSessionsSnapshot())
    }

    suspend fun importBackupJson(json: String) {
        val backup = backupCodec.decode(json)
        val activeCount = backup.sessions.count { it.endEpochMillis == null }
        require(activeCount <= 1) { "Backup contains more than one active fast." }
        backup.sessions
            .sortedBy { it.startEpochMillis }
            .zipWithNext()
            .forEach { (left, right) ->
                val leftEnd = left.endEpochMillis ?: Long.MAX_VALUE
                require(leftEnd <= right.startEpochMillis) { "Backup contains overlapping fasts." }
            }
        dao.replaceAll(
            backup.sessions.map { session ->
                FastSessionEntity(
                    id = 0,
                    planId = session.planId,
                    planName = session.planName,
                    targetSeconds = session.targetSeconds,
                    startEpochMillis = session.startEpochMillis,
                    endEpochMillis = session.endEpochMillis,
                    createdEpochMillis = session.createdEpochMillis,
                    updatedEpochMillis = session.updatedEpochMillis,
                )
            },
        )
        weightDao.replaceAll(
            backup.weights.map { entry ->
                WeightEntryEntity(
                    id = 0,
                    weightKg = entry.weightKg,
                    recordedEpochMillis = entry.recordedEpochMillis,
                    createdEpochMillis = entry.createdEpochMillis,
                )
            },
        )
        settingsStore.replace(backup.settings)
    }

    suspend fun deleteAllLocalData() {
        dao.deleteAll()
        weightDao.deleteAll()
        settingsStore.reset()
    }

}
