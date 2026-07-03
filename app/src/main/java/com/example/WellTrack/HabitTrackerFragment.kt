package com.example.WellTrack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitTrackerFragment : Fragment(), HabitAdapter.OnItemClickListener {

    private lateinit var habitAdapter: HabitAdapter
    private lateinit var habits: MutableList<Habit>
    private lateinit var dailyCompletions: MutableList<DailyCompletion>
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val gson = Gson()
    private lateinit var loadingOverlay: LoadingOverlay
    
    private var currentDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    
    private lateinit var currentDateText: TextView
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_habit_tracker, container, false)

        sharedPreferences = UserDataManager.getUserSpecificPreferences(requireContext(), "habits")
        loadingOverlay = LoadingOverlay(view as ViewGroup)

        loadingOverlay.show("Loading habits...")
        loadHabits()
        loadDailyCompletions()
        loadingOverlay.hide()

        // Initialize views
        currentDateText = view.findViewById(R.id.current_date_text)
        progressText = view.findViewById(R.id.progress_text)
        progressBar = view.findViewById(R.id.progress_bar)
        
        val prevDayButton = view.findViewById<ImageButton>(R.id.prev_day_button)
        val nextDayButton = view.findViewById<ImageButton>(R.id.next_day_button)
        
        prevDayButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDateDisplay()
            refreshHabitList()
        }
        
        nextDayButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            updateDateDisplay()
            refreshHabitList()
        }

        /*display
        view of habits
         */
        val habitRecyclerView = view.findViewById<RecyclerView>(R.id.habit_recycler_view)
        habitAdapter = HabitAdapter(habits, getCompletionsForCurrentDate(), this)
        habitRecyclerView.adapter = habitAdapter
        habitRecyclerView.layoutManager = LinearLayoutManager(context)

        val addHabitButton = view.findViewById<FloatingActionButton>(R.id.add_habit_button)
        addHabitButton.setOnClickListener {
            showAddEditHabitDialog()
        }
        
        updateDateDisplay()
        updateProgress()

        return view
    }

    private fun updateDateDisplay() {
        val today = Calendar.getInstance()
        val dateStr = if (isSameDay(currentDate, today)) {
            "Today - ${displayDateFormat.format(currentDate.time)}"
        } else {
            displayDateFormat.format(currentDate.time)
        }
        currentDateText.text = dateStr
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun getCurrentDateString(): String {
        return dateFormat.format(currentDate.time)
    }
    
    private fun getCompletionsForCurrentDate(): Map<String, Boolean> {
        val dateStr = getCurrentDateString()
        return dailyCompletions
            .filter { it.date == dateStr }
            .associate { it.habitId to it.isCompleted }
    }
    
    private fun refreshHabitList() {
        habitAdapter = HabitAdapter(habits, getCompletionsForCurrentDate(), this)
        view?.findViewById<RecyclerView>(R.id.habit_recycler_view)?.adapter = habitAdapter
        updateProgress()
    }
    
    private fun updateProgress() {
        if (habits.isEmpty()) {
            progressText.text = "No habits yet"
            progressBar.progress = 0
            return
        }
        
        val completions = getCompletionsForCurrentDate()
        val completedCount = completions.values.count { it }
        val totalCount = habits.size
        val percentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
        
        progressText.text = "Progress: $completedCount/$totalCount habits completed ($percentage%)"
        progressBar.progress = percentage
    }
/*
add habits
 */
    private fun showAddEditHabitDialog(position: Int? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_habit, null)
        val habitNameEditText = dialogView.findViewById<EditText>(R.id.habit_name_edit_text)
        val dialogTitle = if (position == null) "Add Habit" else "Edit Habit"

        if (position != null) {
            habitNameEditText.setText(habits[position].name)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton(if (position == null) "Add" else "Save") { _, _ ->
                val habitName = habitNameEditText.text.toString()
                if (habitName.isNotBlank()) {
                    if (position == null) {
                        habits.add(Habit(name = habitName))
                        habitAdapter.notifyItemInserted(habits.size - 1)
                    } else {
                        habits[position].name = habitName
                        habitAdapter.notifyItemChanged(position)
                    }
                    saveHabits()
                    updateProgress()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /*
    edit habits
     */
    override fun onEditClick(position: Int) {
        showAddEditHabitDialog(position)
    }
/*
delete habits
 */
    override fun onDeleteClick(position: Int) {
        val habitId = habits[position].id
        habits.removeAt(position)
        
        // Remove all completions for this habit
        dailyCompletions.removeAll { it.habitId == habitId }
        
        habitAdapter.notifyItemRemoved(position)
        saveHabits()
        saveDailyCompletions()
        updateProgress()
    }
  /*
  complete tick
   */
    override fun onCompletionToggle(habitId: String, isCompleted: Boolean) {
        val dateStr = getCurrentDateString()
        
        // Find existing completion or create new one
        val existing = dailyCompletions.find { it.habitId == habitId && it.date == dateStr }
        if (existing != null) {
            existing.isCompleted = isCompleted
        } else {
            dailyCompletions.add(DailyCompletion(habitId, dateStr, isCompleted))
        }
        
        saveDailyCompletions()
        updateProgress()
        
        // Update widget
        HabitsWidgetProvider.updateAllWidgets(requireContext())
    }

    private fun saveHabits() {
        loadingOverlay.show("Saving habits...")
        val habitsJson = gson.toJson(habits)
        sharedPreferences.edit().putString("habits_list", habitsJson).apply()
        view?.postDelayed({ loadingOverlay.hide() }, 300)
    }
    
    private fun saveDailyCompletions() {
        val completionsJson = gson.toJson(dailyCompletions)
        sharedPreferences.edit().putString("daily_completions", completionsJson).apply()
    }

    private fun loadHabits() {
        val habitsJson = sharedPreferences.getString("habits_list", null)
        if (habitsJson != null) {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            habits = gson.fromJson(habitsJson, type)
        } else {
            // Start with empty list - users can add their own habits
            habits = mutableListOf()
        }
    }
    
    private fun loadDailyCompletions() {
        val completionsJson = sharedPreferences.getString("daily_completions", null)
        if (completionsJson != null) {
            val type = object : TypeToken<MutableList<DailyCompletion>>() {}.type
            dailyCompletions = gson.fromJson(completionsJson, type)
        } else {
            dailyCompletions = mutableListOf()
        }
    }
}