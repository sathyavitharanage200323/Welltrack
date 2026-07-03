package com.example.WellTrack

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class HydrationReminderFragment : Fragment() {

    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val gson = Gson()
    
    private lateinit var glassesCountText: TextView
    private lateinit var hydrationProgressBar: ProgressBar
    private lateinit var addGlassButton: Button
    private lateinit var reminderSwitch: SwitchMaterial
    private lateinit var intervalRadioGroup: RadioGroup
    private lateinit var dailyGoalSlider: Slider
    private lateinit var dailyGoalValue: TextView
    private lateinit var weekSummaryText: TextView
    
    private var dailyGoal = 8
    private var currentGlasses = 0
    private var reminderInterval = 60L // minutes
    private var hydrationHistory = mutableMapOf<String, Int>() // date -> glasses count
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (reminderSwitch.isChecked) {
                scheduleReminders()
            }
        } else {
            reminderSwitch.isChecked = false
            Toast.makeText(context, "Notification permission required for reminders", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hydration_reminder, container, false)

        sharedPreferences = UserDataManager.getUserSpecificPreferences(requireContext(), "hydration")
        
        // Initialize views
        glassesCountText = view.findViewById(R.id.glasses_count_text)
        hydrationProgressBar = view.findViewById(R.id.hydration_progress_bar)
        addGlassButton = view.findViewById(R.id.add_glass_button)
        reminderSwitch = view.findViewById(R.id.reminder_switch)
        intervalRadioGroup = view.findViewById(R.id.interval_radio_group)
        dailyGoalSlider = view.findViewById(R.id.daily_goal_slider)
        dailyGoalValue = view.findViewById(R.id.daily_goal_value)
        weekSummaryText = view.findViewById(R.id.week_summary_text)
        
        loadSettings()
        loadTodayProgress()
        loadHistory()
        updateUI()
        updateWeekSummary()
        
        // Hydration buttons
        val removeGlassButton = view.findViewById<Button>(R.id.remove_glass_button)
        val resetButton = view.findViewById<Button>(R.id.reset_button)
        // Daily Water Tracking
        addGlassButton.setOnClickListener {
            addGlass()
        }
        
        removeGlassButton.setOnClickListener {
            removeGlass()
        }
        
        resetButton.setOnClickListener {
            resetGlasses()
        }
        
        // Reminder switch
        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkNotificationPermissionAndSchedule()
            } else {
                cancelReminders()
            }
            saveSettings()
        }
        /*
        notification send at time
         */
        intervalRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            reminderInterval = when (checkedId) {
                R.id.interval_30_min -> 30L
                R.id.interval_1_hour -> 60L
                R.id.interval_2_hours -> 120L
                R.id.interval_3_hours -> 180L
                else -> 60L
            }
            saveSettings()
            if (reminderSwitch.isChecked) {
                scheduleReminders()
            }
        }
        
        // Daily goal slider
        dailyGoalSlider.addOnChangeListener { _, value, _ ->
            dailyGoal = value.toInt()
            dailyGoalValue.text = dailyGoal.toString()
            updateUI()
            saveSettings()
        }

        return view
    }
/*
notification permission
 */
    private fun checkNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleReminders()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            scheduleReminders()
        }
    }

    private fun scheduleReminders() {
        val workRequest = PeriodicWorkRequestBuilder<HydrationWorker>(
            reminderInterval,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            HydrationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        
        Toast.makeText(context, "Reminders enabled every $reminderInterval minutes", Toast.LENGTH_SHORT).show()
    }

    private fun cancelReminders() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(HydrationWorker.WORK_NAME)
        Toast.makeText(context, "Reminders disabled", Toast.LENGTH_SHORT).show()
    }

    /*add
    water intake
     */
    private fun addGlass() {
        currentGlasses++
        saveTodayProgress()
        updateUI()
        
        if (currentGlasses == dailyGoal) {
            Toast.makeText(context, "🎉 Daily goal reached! Great job!", Toast.LENGTH_SHORT).show()
            sendGoalReachedNotification()
        }
    }
    
    private fun removeGlass() {
        if (currentGlasses > 0) {
            currentGlasses--
            saveTodayProgress()
            updateUI()
            Toast.makeText(context, "Glass removed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Already at 0 glasses", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun resetGlasses() {
        if (currentGlasses == 0) {
            Toast.makeText(context, "Already at 0 glasses", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Hydration")
            .setMessage("Are you sure you want to reset today's water intake to 0?")
            .setPositiveButton("Reset") { _, _ ->
                currentGlasses = 0
                saveTodayProgress()
                updateUI()
                Toast.makeText(context, "Hydration reset to 0", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    /*send
    notifications
     */
    private fun sendGoalReachedNotification() {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "hydration_goal",
                "Hydration Goal Achievements",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when daily hydration goal is reached"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app
        val intent = android.content.Intent(requireContext(), MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_hydration", true)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = androidx.core.app.NotificationCompat.Builder(requireContext(), "hydration_goal")
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle("🎉 Hydration Goal Reached!")
            .setContentText("Congratulations! You've reached your daily goal of $dailyGoal glasses!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL) //sound option
            .build()

        notificationManager.notify(1002, notification)
    }

    private fun updateUI() {
        glassesCountText.text = "$currentGlasses / $dailyGoal"
        val percentage = if (dailyGoal > 0) (currentGlasses * 100) / dailyGoal else 0
        hydrationProgressBar.progress = percentage.coerceAtMost(100)
    }

    private fun loadSettings() {
        dailyGoal = sharedPreferences.getInt("daily_goal", 8)
        reminderInterval = sharedPreferences.getLong("reminder_interval", 60L)
        val remindersEnabled = sharedPreferences.getBoolean("reminders_enabled", false)
        
        dailyGoalSlider.value = dailyGoal.toFloat()
        dailyGoalValue.text = dailyGoal.toString()
        reminderSwitch.isChecked = remindersEnabled
        
        // Set radio button based on interval
        val radioButtonId = when (reminderInterval) {
            30L -> R.id.interval_30_min
            60L -> R.id.interval_1_hour
            120L -> R.id.interval_2_hours
            180L -> R.id.interval_3_hours
            else -> R.id.interval_1_hour
        }
        intervalRadioGroup.check(radioButtonId)
    }

    private fun saveSettings() {
        sharedPreferences.edit().apply {
            putInt("daily_goal", dailyGoal)
            putLong("reminder_interval", reminderInterval)
            putBoolean("reminders_enabled", reminderSwitch.isChecked)
            apply()
        }
    }

    private fun loadTodayProgress() {
        val today = dateFormat.format(Calendar.getInstance().time)
        currentGlasses = sharedPreferences.getInt("glasses_$today", 0)
    }

    private fun saveTodayProgress() {
        val today = dateFormat.format(Calendar.getInstance().time)
        sharedPreferences.edit().putInt("glasses_$today", currentGlasses).apply()
        
        // Update history
        hydrationHistory[today] = currentGlasses
        saveHistory()
        updateWeekSummary()
    }

    private fun loadHistory() {
        val historyJson = sharedPreferences.getString("hydration_history", null)
        if (historyJson != null) {
            val type = object : TypeToken<MutableMap<String, Int>>() {}.type
            hydrationHistory = gson.fromJson(historyJson, type)
        }
    }


    private fun saveHistory() {
        val historyJson = gson.toJson(hydrationHistory)
        sharedPreferences.edit().putString("hydration_history", historyJson).apply()
    }
    /*
       show weekly stat
        */
    private fun updateWeekSummary() {
        val calendar = Calendar.getInstance()
        val last7Days = mutableListOf<String>()
        
        for (i in 0..6) {
            last7Days.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        val weekData = last7Days.mapNotNull { date ->
            hydrationHistory[date]
        }
        
        if (weekData.isNotEmpty()) {
            val average = weekData.average()
            val goalsReached = weekData.count { it >= dailyGoal }
            
            weekSummaryText.text = "Average: %.1f glasses/day\nGoal reached: %d/7 days".format(average, goalsReached)
        } else {
            weekSummaryText.text = "No data for this week yet"
        }
    }
}
