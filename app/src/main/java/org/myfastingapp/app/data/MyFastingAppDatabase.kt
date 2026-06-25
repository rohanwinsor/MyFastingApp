package org.myfastingapp.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FastSessionEntity::class, WeightEntryEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class MyFastingAppDatabase : RoomDatabase() {
    abstract fun fastSessionDao(): FastSessionDao
    abstract fun weightEntryDao(): WeightEntryDao
}
