package com.example.WellTrack

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HabitsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_habits)
            
            // Load habits data
            val (completed, total, percentage) = getHabitsData(context)
            
            // Update widget views
            views.setTextViewText(R.id.widget_percentage, "$percentage%")
            views.setProgressBar(R.id.widget_progress_bar, 100, percentage, false)
            views.setTextViewText(R.id.widget_summary, "$completed of $total completed")
            
            // Set click intent to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_progress_bar, pendingIntent)
            
            // Update widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        private fun getHabitsData(context: Context): Triple<Int, Int, Int> {
            // Check if user is logged in
            val currentUserEmail = UserDataManager.getCurrentUserEmail(context)
            if (currentUserEmail == null) {
                return Triple(0, 0, 0)
            }
            
            // Get user-specific preferences
            val habitsPrefs = UserDataManager.getUserSpecificPreferences(context, "habits")
            val gson = Gson()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // Load habits
            val habitsJson = habitsPrefs.getString("habits_list", null)
            val habits: List<Habit> = if (habitsJson != null) {
                val type = object : TypeToken<List<Habit>>() {}.type
                gson.fromJson(habitsJson, type)
            } else {
                emptyList()
            }
            
            if (habits.isEmpty()) {
                return Triple(0, 0, 0)
            }
            
            // Load today's completions
            val completionsJson = habitsPrefs.getString("daily_completions", null)
            val allCompletions: List<DailyCompletion> = if (completionsJson != null) {
                val type = object : TypeToken<List<DailyCompletion>>() {}.type
                gson.fromJson(completionsJson, type)
            } else {
                emptyList()
            }
            
            val today = dateFormat.format(Calendar.getInstance().time)
            val todayCompletions = allCompletions.filter { it.date == today && it.isCompleted }
            
            val completedCount = todayCompletions.size
            val totalCount = habits.size
            val percentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
            
            return Triple(completedCount, totalCount, percentage)
        }
        
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, HabitsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, HabitsWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
