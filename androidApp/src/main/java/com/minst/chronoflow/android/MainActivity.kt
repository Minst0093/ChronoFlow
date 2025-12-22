package com.minst.chronoflow.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.minst.chronoflow.platform.NotificationScheduler
import com.minst.chronoflow.presentation.CalendarViewModel
import com.minst.chronoflow.android.createPlatformLunarService
import com.minst.chronoflow.domain.engine.DefaultLunarCalendarService
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class MainActivity : ComponentActivity() {

    private val sharedViewModel: CalendarViewModel by lazy {
        val scheduler: NotificationScheduler = AndroidNotificationScheduler(this)
        val repository = try {
            SqlDelightEventRepository(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize SQLDelight repository, using InMemoryEventRepository", e)
            // 如果 SQLDelight 初始化失败，回退到内存版本
            com.minst.chronoflow.data.local.InMemoryEventRepository()
        }
        CalendarViewModel(
            repository = repository,
            notificationScheduler = scheduler,
            lunarService = createPlatformLunarService(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        // 处理从通知跳转过来的Intent
        val selectedDateStr = intent.getStringExtra("selected_date")
        val shouldShowDayView = selectedDateStr != null
        if (selectedDateStr != null) {
            try {
                val selectedDate = LocalDate.parse(selectedDateStr)
                sharedViewModel.onDaySelected(selectedDate)
            } catch (e: Exception) {
                // 忽略解析错误
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = sharedViewModel,
                        initialTab = if (shouldShowDayView) 2 else 0, // 2 = 日视图
                    )
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

@Composable
private fun MainScreen(
    viewModel: CalendarViewModel,
    initialTab: Int = 0,
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("月视图") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("周视图") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    label = { Text("日视图") },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                0 -> MonthViewScreen(
                    viewModel = viewModel,
                    onDayClick = { date: LocalDate ->
                        viewModel.onDaySelected(date)
                        selectedTab = 2 // 切换到日视图
                    },
                )
                1 -> WeekViewScreen(
                    viewModel = viewModel,
                    onDayClick = { date: LocalDate ->
                        viewModel.onDaySelected(date)
                        selectedTab = 2 // 切换到日视图
                    },
                )
                2 -> DayViewScreen(viewModel = viewModel)
            }
        }
    }
}


