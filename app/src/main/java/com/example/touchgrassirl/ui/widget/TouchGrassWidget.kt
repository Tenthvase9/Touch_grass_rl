package com.example.touchgrassirl.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.touchgrassirl.MainActivity
import com.example.touchgrassirl.R
import com.example.touchgrassirl.data.local.TouchGrassDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class TouchGrassWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = TouchGrassDatabase.getInstance(context)
                val today = LocalDate.now().toEpochDay()
                val log = withContext(Dispatchers.IO) {
                    db.dailyLogDao().getForDay(today)
                }

                val minutes = log?.outdoorMinutes ?: 0
                val steps = log?.steps ?: 0
                val xp = log?.xpEarned ?: 0
                val touched = log?.touchedGrass ?: false
                val progress = (minutes.toFloat() / 30f).coerceIn(0f, 1f)

                val views = RemoteViews(context.packageName, R.layout.widget_touch_grass)
                views.setTextViewText(R.id.widget_status_text, if (touched) "\uD83C\uDF3F Touched grass!" else "\uD83C\uDF31 Not yet")
                views.setTextViewText(R.id.widget_minutes, "$minutes min")
                views.setTextViewText(R.id.widget_steps, "$steps steps")
                views.setTextViewText(R.id.widget_xp, "+$xp XP")
                views.setProgressBar(R.id.widget_progress, 100, (progress * 100).toInt(), false)

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
