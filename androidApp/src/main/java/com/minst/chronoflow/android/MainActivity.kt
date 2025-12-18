package com.minst.chronoflow.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.minst.chronoflow.platform.NotificationScheduler
import com.minst.chronoflow.presentation.CalendarViewModel

class MainActivity : ComponentActivity() {

    private val sharedViewModel: CalendarViewModel by lazy {
        val scheduler: NotificationScheduler = AndroidNotificationScheduler(this)
        CalendarViewModel(notificationScheduler = scheduler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        setContent {
            MaterialTheme {
                Surface {
                    DayViewScreen(viewModel = sharedViewModel)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001,
                )
            }
        }
    }
}


