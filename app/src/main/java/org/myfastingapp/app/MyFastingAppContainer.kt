package org.myfastingapp.app

import android.content.Context
import androidx.room.Room
import org.myfastingapp.app.backup.BackupCodec
import org.myfastingapp.app.data.FastRepository
import org.myfastingapp.app.data.MyFastingAppDatabase
import org.myfastingapp.app.data.SettingsStore
import org.myfastingapp.app.notify.FastReminderScheduler

class MyFastingAppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: MyFastingAppDatabase = Room.databaseBuilder(
        appContext,
        MyFastingAppDatabase::class.java,
        "myfastingapp.db",
    )
        .build()

    val settingsStore = SettingsStore(appContext)
    val backupCodec = BackupCodec()
    val repository = FastRepository(database.fastSessionDao(), database.weightEntryDao(), settingsStore, backupCodec)
    val reminderScheduler = FastReminderScheduler(appContext)
}
