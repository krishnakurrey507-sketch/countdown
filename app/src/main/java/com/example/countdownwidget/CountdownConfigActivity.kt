package com.example.countdownwidget

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CountdownConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CountdownConfigScreen(
                        appWidgetId = appWidgetId,
                        onSaved = {
                            val resultIntent = Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                appWidgetId
                            )
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownConfigScreen(
    appWidgetId: Int,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("My Countdown") }
    var targetEpoch by remember {
        mutableLongStateOf(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30))
    }
    var selectedBg by remember { mutableStateOf("#0F172A") }
    var selectedAccent by remember { mutableStateOf("#38BDF8") }

    val bgOptions = listOf("#0F172A", "#1E1B4B", "#14532D", "#450A0A", "#18181B")
    val accentOptions = listOf("#38BDF8", "#F59E0B", "#34D399", "#FB7185", "#C084FC")

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Customize Countdown Widget",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Event Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Target: ${dateFormatter.format(Date(targetEpoch))}")
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = targetEpoch }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val picked = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            targetEpoch = picked.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Text("Pick Date")
            }
        }

        Text("Background Color", style = MaterialTheme.typography.titleSmall)
        ColorRow(
            options = bgOptions,
            selectedHex = selectedBg,
            onSelect = { selectedBg = it }
        )

        Text("Accent Color", style = MaterialTheme.typography.titleSmall)
        ColorRow(
            options = accentOptions,
            selectedHex = selectedAccent,
            onSelect = { selectedAccent = it }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceId = manager.getGlanceIdBy(appWidgetId)

                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs: Preferences ->
                        prefs.toMutablePreferences().apply {
                            this[CountdownKeys.TITLE] = title.ifBlank { "Countdown" }
                            this[CountdownKeys.TARGET_EPOCH] = targetEpoch
                            this[CountdownKeys.BG_HEX] = selectedBg
                            this[CountdownKeys.ACCENT_HEX] = selectedAccent
                        }
                    }
                    CountdownWidget().update(context, glanceId)
                    onSaved()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Save & Add Widget")
        }
    }
}

@Composable
private fun ColorRow(
    options: List<String>,
    selectedHex: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { hex ->
            val color = Color(android.graphics.Color.parseColor(hex))
            val isSelected = hex.equals(selectedHex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
