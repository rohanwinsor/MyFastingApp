package org.myfastingapp.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.myfastingapp.app.domain.FastSession

@Entity(
    tableName = "fast_sessions",
    indices = [
        Index("startEpochMillis"),
        Index("endEpochMillis"),
    ],
)
data class FastSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: String,
    val planName: String,
    val targetSeconds: Long,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val createdEpochMillis: Long,
    val updatedEpochMillis: Long,
) {
    fun toDomain(): FastSession = FastSession(
        id = id,
        planId = planId,
        planName = planName,
        targetSeconds = targetSeconds,
        startEpochMillis = startEpochMillis,
        endEpochMillis = endEpochMillis,
        createdEpochMillis = createdEpochMillis,
        updatedEpochMillis = updatedEpochMillis,
    )
}

fun FastSession.toEntity(): FastSessionEntity = FastSessionEntity(
    id = id,
    planId = planId,
    planName = planName,
    targetSeconds = targetSeconds,
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    createdEpochMillis = createdEpochMillis,
    updatedEpochMillis = updatedEpochMillis,
)
