package com.example.WellTrack

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(), SensorEventListener {

    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val gson = Gson()
    private lateinit var loadingOverlay: LoadingOverlay

    // Step counter
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var sensorStepCount = 0
    private var previousSensorStepCount = 0
    private var currentSteps = 0
    private val stepGoal = 10000

    // Views
    private lateinit var welcomeText: TextView
    private lateinit var stepsCountText: TextView
    private lateinit var stepsProgressBar: ProgressBar
    private lateinit var stepsGoalText: TextView
    
    private lateinit var habitsPercentageText: TextView
    private lateinit var habitsSummaryText: TextView
    private lateinit var habitsProgressBar: ProgressBar
    
    private lateinit var hydrationPercentageText: TextView
    private lateinit var hydrationSummaryText: TextView
    private lateinit var hydrationProgressBar: ProgressBar
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupStepCounter()
        } else {
            stepsCountText.text = "N/A"
            stepsGoalText.text = "Permission required"
            Toast.makeText(context, "Activity recognition permission is required for step counting", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        sharedPreferences = UserDataManager.getUserSpecificPreferences(requireContext(), "vitalpath_home")
        loadingOverlay = LoadingOverlay(view as ViewGroup)
        
        // Initialize views
        welcomeText = view.findViewById(R.id.welcome_text)
        stepsCountText = view.findViewById(R.id.steps_count_text)
        stepsProgressBar = view.findViewById(R.id.steps_progress_bar)
        stepsGoalText = view.findViewById(R.id.steps_goal_text)
        
        habitsPercentageText = view.findViewById(R.id.habits_percentage_text)
        habitsSummaryText = view.findViewById(R.id.habits_summary_text)
        habitsProgressBar = view.findViewById(R.id.habits_progress_bar)
        
        hydrationPercentageText = view.findViewById(R.id.hydration_percentage_text)
        hydrationSummaryText = view.findViewById(R.id.hydration_summary_text)
        hydrationProgressBar = view.findViewById(R.id.hydration_progress_bar)
        
        val viewHabitsButton = view.findViewById<Button>(R.id.view_habits_button)
        val addWaterButton = view.findViewById<Button>(R.id.add_water_button)
        
        // Check permission and setup step counter
        checkPermissionAndSetupStepCounter()
        
        // Set welcome message
        updateWelcomeMessage()
        
        // Load data with loading overlay
        loadingOverlay.show("Loading your dashboard...")
        view.post {
            loadTodayData()
            loadingOverlay.hide()
        }
        
        // Button listeners
        viewHabitsButton.setOnClickListener {
            navigateToHabits()
        }
        
        addWaterButton.setOnClickListener {
            navigateToHydration()
        }

        return view
    }
    /*
    step counter
     */
    private fun checkPermissionAndSetupStepCounter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ requires runtime permission
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupStepCounter()
                }
                else -> {
                    activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            // Below Android 10, no runtime permission needed
            setupStepCounter()
        }
    }

    private fun setupStepCounter() {
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        val today = dateFormat.format(Calendar.getInstance().time)
        
        if (stepSensor == null) {
            stepsCountText.text = "N/A"
            stepsGoalText.text = "Step sensor not available"
        } else {
            // Load saved steps for today
            currentSteps = sharedPreferences.getInt("steps_$today", 0)
            previousSensorStepCount = sharedPreferences.getInt("sensor_steps_$today", 0)
            updateStepsUI()
        }
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        loadingOverlay.show("Refreshing data...")
        view?.post {
            loadTodayData()
            loadingOverlay.hide()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val today = dateFormat.format(Calendar.getInstance().time)
                sensorStepCount = it.values[0].toInt()
                
                // Check if it's a new day
                val lastDate = sharedPreferences.getString("last_step_date", "")
                if (lastDate != today) {
                    // New day - reset counters
                    previousSensorStepCount = sensorStepCount
                    currentSteps = 0
                    sharedPreferences.edit()
                        .putString("last_step_date", today)
                        .putInt("sensor_steps_$today", sensorStepCount)
                        .putInt("steps_$today", 0)
                        .apply()
                } else {
                    // Same day - calculate steps from sensor
                    if (previousSensorStepCount == 0) {
                        previousSensorStepCount = sensorStepCount
                    }
                    currentSteps = sensorStepCount - previousSensorStepCount
                    
                    // Save current steps
                    sharedPreferences.edit()
                        .putInt("steps_$today", currentSteps)
                        .putInt("sensor_steps_$today", previousSensorStepCount)
                        .apply()
                }
                
                updateStepsUI()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    private fun updateStepsUI() {
        stepsCountText.text = String.format("%,d", currentSteps)
        stepsProgressBar.progress = currentSteps.coerceAtMost(stepGoal)
        stepsGoalText.text = "Goal: ${String.format("%,d", stepGoal)} steps"
    }

    private fun updateWelcomeMessage() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning! 🌅"
            in 12..16 -> "Good Afternoon! ☀️"
            in 17..20 -> "Good Evening! 🌆"
            else -> "Good Night! 🌙"
        }
        welcomeText.text = greeting
    }

    private fun loadTodayData() {
        loadHabitsData()
        loadHydrationData()
    }

    private fun loadHabitsData() {
        val habitsPrefs = UserDataManager.getUserSpecificPreferences(requireContext(), "habits")
        val completionsPrefs = UserDataManager.getUserSpecificPreferences(requireContext(), "habits")
        
        // Load habits
        val habitsJson = habitsPrefs.getString("habits_list", null)
        val habits: List<Habit> = if (habitsJson != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(habitsJson, type)
        } else {
            emptyList()
        }
        
        if (habits.isEmpty()) {
            habitsPercentageText.text = "0%"
            habitsSummaryText.text = "No habits yet"
            habitsProgressBar.progress = 0
            return
        }
        
        // Load today's completions
        val completionsJson = completionsPrefs.getString("daily_completions", null)
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
        
        habitsPercentageText.text = "$percentage%"
        habitsSummaryText.text = "$completedCount of $totalCount habits completed"
        habitsProgressBar.progress = percentage
    }

    private fun loadHydrationData() {
        val hydrationPrefs = UserDataManager.getUserSpecificPreferences(requireContext(), "hydration")
        
        val dailyGoal = hydrationPrefs.getInt("daily_goal", 8)
        val today = dateFormat.format(Calendar.getInstance().time)
        val currentGlasses = hydrationPrefs.getInt("glasses_$today", 0)
        
        val percentage = if (dailyGoal > 0) (currentGlasses * 100) / dailyGoal else 0
        
        hydrationPercentageText.text = "$percentage%"
        hydrationSummaryText.text = "$currentGlasses of $dailyGoal glasses"
        hydrationProgressBar.progress = percentage.coerceAtMost(100)
    }

    private fun navigateToHabits() {
        val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.selectedItemId = R.id.nav_habits
    }

    private fun navigateToHydration() {
        val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.selectedItemId = R.id.nav_hydration
    }
}
