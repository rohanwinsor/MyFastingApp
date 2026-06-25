package org.myfastingapp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {
    @Query("SELECT * FROM weight_entries ORDER BY recordedEpochMillis DESC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY recordedEpochMillis DESC")
    suspend fun getAllSnapshot(): List<WeightEntryEntity>

    @Insert
    suspend fun insert(entry: WeightEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WeightEntryEntity>)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entries: List<WeightEntryEntity>) {
        deleteAll()
        insertAll(entries)
    }
}
