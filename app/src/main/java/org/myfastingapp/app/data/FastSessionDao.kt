package org.myfastingapp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FastSessionDao {
    @Query("SELECT * FROM fast_sessions ORDER BY startEpochMillis DESC")
    fun observeAll(): Flow<List<FastSessionEntity>>

    @Query("SELECT * FROM fast_sessions WHERE endEpochMillis IS NULL ORDER BY startEpochMillis DESC LIMIT 1")
    fun observeActive(): Flow<FastSessionEntity?>

    @Query("SELECT * FROM fast_sessions WHERE endEpochMillis IS NULL ORDER BY startEpochMillis DESC LIMIT 1")
    suspend fun getActive(): FastSessionEntity?

    @Query("SELECT COUNT(*) FROM fast_sessions WHERE endEpochMillis IS NULL")
    suspend fun activeCount(): Int

    @Query("SELECT * FROM fast_sessions ORDER BY startEpochMillis DESC")
    suspend fun getAllSnapshot(): List<FastSessionEntity>

    @Query("SELECT * FROM fast_sessions WHERE id = :id")
    suspend fun getById(id: Long): FastSessionEntity?

    @Insert
    suspend fun insert(session: FastSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<FastSessionEntity>)

    @Update
    suspend fun update(session: FastSessionEntity)

    @Query("DELETE FROM fast_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fast_sessions")
    suspend fun deleteAll()

    @Query(
        """
        SELECT COUNT(*) FROM fast_sessions
        WHERE id != :ignoreId
        AND startEpochMillis < :endEpochMillis
        AND COALESCE(endEpochMillis, 9223372036854775807) > :startEpochMillis
        """,
    )
    suspend fun overlappingCount(
        startEpochMillis: Long,
        endEpochMillis: Long,
        ignoreId: Long = -1,
    ): Int

    @Transaction
    suspend fun replaceAll(sessions: List<FastSessionEntity>) {
        deleteAll()
        insertAll(sessions)
    }
}
