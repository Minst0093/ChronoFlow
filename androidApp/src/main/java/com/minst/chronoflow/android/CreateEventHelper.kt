package com.minst.chronoflow.android

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.minst.chronoflow.domain.model.CalendarEvent
import com.minst.chronoflow.presentation.CalendarViewModel
import kotlinx.datetime.LocalDate

/**
 * Reusable FAB + create dialog. Call this from each screen's Scaffold.floatingActionButton slot.
 *
 * @param viewModel view model to call onCreateEvent
 * @param initialDateProvider returns the date to prefill the new event
 */
@Composable
fun CreateEventFloating(
    viewModel: CalendarViewModel,
    initialDateProvider: () -> LocalDate,
) {
    val showCreate = remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { showCreate.value = true },
        containerColor = Color(0xFF1A73E8),
        modifier = Modifier.padding(16.dp),
        elevation = FloatingActionButtonDefaults.elevation()
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "add", tint = Color.White)
    }

    if (showCreate.value) {
        EventFormDialog(
            event = null,
            initialDate = initialDateProvider(),
            onDismiss = { showCreate.value = false },
            onSave = { input ->
                viewModel.onCreateEvent(input)
                showCreate.value = false
            },
            onUpdate = {}
        )
    }
}


