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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.minst.chronoflow.platform.NotificationScheduler
import com.minst.chronoflow.presentation.CalendarViewModel
import com.minst.chronoflow.android.createPlatformLunarService
import com.minst.chronoflow.domain.engine.DefaultLunarCalendarService
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import com.minst.chronoflow.android.ViewMode

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

        // 隐藏原生 Activity 的 ActionBar / 标题，避免出现全局黑色条重复显示应用名
        // ComponentActivity 上可以使用 actionBar 来隐藏系统 ActionBar（如果存在）
        actionBar?.hide()
        // 清空 Activity 标题以防某些系统主题仍在显示标题文本
        title = ""

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = sharedViewModel,
                        initialViewMode = if (shouldShowDayView) ViewMode.DAY else ViewMode.MONTH,
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
    initialViewMode: ViewMode = ViewMode.MONTH,
) {
    var selectedViewMode by remember { mutableStateOf(initialViewMode) }

    val onViewModeChange: (ViewMode) -> Unit = { newMode ->
        selectedViewMode = when (newMode) {
            ViewMode.MONTH -> ViewMode.MONTH
            ViewMode.WEEK -> ViewMode.WEEK
            ViewMode.DAY -> ViewMode.DAY
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedViewMode) {
                ViewMode.MONTH -> MonthViewScreen(
                    viewModel = viewModel,
                    onViewModeChange = onViewModeChange,
                    onDayClick = { date: LocalDate ->
                        viewModel.onDaySelected(date)
                        selectedViewMode = ViewMode.DAY // 切换到日视图
                    },
                )
                ViewMode.WEEK -> WeekViewScreen(
                    viewModel = viewModel,
                    onViewModeChange = onViewModeChange,
                    onDayClick = { date: LocalDate ->
                        viewModel.onDaySelected(date)
                        selectedViewMode = ViewMode.DAY // 切换到日视图
                    },
                )
                ViewMode.DAY -> DayViewScreen(
                    viewModel = viewModel,
                    onViewModeChange = onViewModeChange
                )
            }
        }
    }
}


