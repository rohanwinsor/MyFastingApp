package org.myfastingapp.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService

class MyFastingAppApplication : Application() {
    val container: MyFastingAppContainer by lazy { MyFastingAppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService<NotificationManager>() ?: return
        val statusChannel = NotificationChannel(
            FASTING_STATUS_CHANNEL_ID,
            getString(R.string.notification_channel_status),
            NotificationManager.IMPORTANCE_LOW,
        )
        val alertChannel = NotificationChannel(
            FASTING_ALERTS_CHANNEL_ID,
            getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannels(listOf(statusChannel, alertChannel))
    }

    companion object {
        const val FASTING_STATUS_CHANNEL_ID = "fasting_status"
        const val FASTING_ALERTS_CHANNEL_ID = "fasting_alerts"
    }
}
