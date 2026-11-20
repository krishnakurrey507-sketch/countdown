package com.example.countdownwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.util.concurrent.TimeUnit

object CountdownKeys {
    val TITLE = stringPreferencesKey("title")
    val TARGET_EPOCH = longPreferencesKey("target_epoch")
    val BG_HEX = stringPreferencesKey("bg_hex")
    val ACCENT_HEX = stringPreferencesKey("accent_hex")
}

class CountdownWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[CountdownKeys.TITLE] ?: "Special Day"
            val targetEpoch = prefs[CountdownKeys.TARGET_EPOCH]
                ?: (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))
            val bgColor = parseColor(prefs[CountdownKeys.BG_HEX], Color(0xFF0F172A))
            val accentColor = parseColor(prefs[CountdownKeys.ACCENT_HEX], Color(0xFF38BDF8))

            CountdownContent(
                title = title,
                targetEpoch = targetEpoch,
                bgColor = bgColor,
                accentColor = accentColor
            )
        }
    }

    private fun parseColor(hex: String?, fallback: Color): Color = try {
        if (!hex.isNullOrBlank()) Color(android.graphics.Color.parseColor(hex)) else fallback
    } catch (_: Exception) {
        fallback
    }
}

@Composable
private fun CountdownContent(
    title: String,
    targetEpoch: Long,
    bgColor: Color,
    accentColor: Color
) {
    val diffMs = (targetEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .cornerRadius(18.dp)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(10.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimeBox(value = "%02d".format(days), label = "DAYS", accent = accentColor)
            Spacer(GlanceModifier.width(8.dp))
            TimeBox(value = "%02d".format(hours), label = "HRS", accent = accentColor)
            Spacer(GlanceModifier.width(8.dp))
            TimeBox(value = "%02d".format(minutes), label = "MIN", accent = accentColor)
        }
    }
}

@Composable
private fun TimeBox(value: String, label: String, accent: Color) {
    Column(
        modifier = GlanceModifier
            .background(Color(0x1FFFFFFF))
            .cornerRadius(12.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(accent),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(Color(0xCCFFFFFF)),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}
