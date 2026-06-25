package org.myfastingapp.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.myfastingapp.app.ui.MyFastingApp
import org.myfastingapp.app.ui.MyFastingAppViewModel

class MainActivity : ComponentActivity() {
    private var refreshExternalSurfaces: (() -> Unit)? = null
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) refreshExternalSurfaces?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MyFastingAppApplication).container
        setContent {
            val viewModel: MyFastingAppViewModel = viewModel(
                factory = MyFastingAppViewModel.factory(
                    application = application as MyFastingAppApplication,
                    repository = container.repository,
                    settingsStore = container.settingsStore,
                    reminderScheduler = container.reminderScheduler,
                ),
            )
            refreshExternalSurfaces = viewModel::refreshExternalSurfaces
            MyFastingApp(viewModel)
        }
        requestNotificationPermissionIfNeeded()
    }

    override fun onDestroy() {
        refreshExternalSurfaces = null
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
