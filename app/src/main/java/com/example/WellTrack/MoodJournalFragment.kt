package com.example.WellTrack

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MoodJournalFragment : Fragment() {

    private lateinit var moodEntryAdapter: MoodEntryAdapter
    private lateinit var moodEntries: MutableList<MoodEntry>
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val gson = Gson()
    private lateinit var loadingOverlay: LoadingOverlay
    
    private lateinit var moodEntriesRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private var selectedMood: MoodOption? = null
    
    // Calendar view components
    private var calendarView: View? = null
    private var currentCalendarMonth = Calendar.getInstance()
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var isCalendarViewActive = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mood_journal, container, false)

        sharedPreferences = UserDataManager.getUserSpecificPreferences(requireContext(), "mood_journal")
        loadingOverlay = LoadingOverlay(view as ViewGroup)

        loadingOverlay.show("Loading mood entries...")
        loadMoodEntries()
        loadingOverlay.hide()

        // Initialize views
        moodEntriesRecyclerView = view.findViewById(R.id.mood_entries_recycler_view)
        emptyStateLayout = view.findViewById(R.id.empty_state)
        
        // Setup RecyclerView
        moodEntryAdapter = MoodEntryAdapter(moodEntries) { position ->
            deleteMoodEntry(position)
        }
        moodEntriesRecyclerView.adapter = moodEntryAdapter
        moodEntriesRecyclerView.layoutManager = LinearLayoutManager(context)

        // Statistics button
        val statisticsButton = view.findViewById<ImageButton>(R.id.statistics_button)
        statisticsButton.setOnClickListener {
            showStatistics()
        }
        
        // View toggle buttons
        val listViewButton = view.findViewById<Button>(R.id.list_view_button)
        val calendarViewButton = view.findViewById<Button>(R.id.calendar_view_button)
        
        listViewButton.setOnClickListener {
            showListView()
        }
        
        calendarViewButton.setOnClickListener {
            showCalendarView()
        }

        // FAB to add mood
        val addMoodButton = view.findViewById<FloatingActionButton>(R.id.add_mood_button)
        addMoodButton.setOnClickListener {
            showAddMoodDialog()
        }
        
        updateEmptyState()

        return view
    }

    /*
    add moods emojis
     */
    private fun showAddMoodDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_mood, null)
        val emojiGrid = dialogView.findViewById<RecyclerView>(R.id.emoji_grid)
        val selectedMoodLayout = dialogView.findViewById<LinearLayout>(R.id.selected_mood_layout)
        val selectedEmojiText = dialogView.findViewById<TextView>(R.id.selected_emoji_text)
        val selectedMoodName = dialogView.findViewById<TextView>(R.id.selected_mood_name)
        val moodNoteEditText = dialogView.findViewById<EditText>(R.id.mood_note_edit_text)
        
        selectedMood = null
        
        // Setup emoji grid
        val emojiAdapter = EmojiSelectorAdapter(MoodEmojis.moods) { mood ->
            selectedMood = mood
            selectedEmojiText.text = mood.emoji
            selectedMoodName.text = mood.name
            selectedMoodLayout.visibility = View.VISIBLE
        }
        emojiGrid.adapter = emojiAdapter
        emojiGrid.layoutManager = GridLayoutManager(context, 4)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Log Your Mood")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                selectedMood?.let { mood ->
                    val note = moodNoteEditText.text.toString()
                    val entry = MoodEntry(
                        emoji = mood.emoji,
                        moodName = mood.name,
                        note = note
                    )
                    moodEntries.add(0, entry) // Add to beginning
                    moodEntryAdapter.notifyItemInserted(0)
                    moodEntriesRecyclerView.scrollToPosition(0)
                    saveMoodEntries()
                    updateEmptyState()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Disable save button until mood is selected
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        
        emojiAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = selectedMood != null
            }
        })
        
        // Update button state when mood is selected
        emojiGrid.adapter = EmojiSelectorAdapter(MoodEmojis.moods) { mood ->
            selectedMood = mood
            selectedEmojiText.text = mood.emoji
            selectedMoodName.text = mood.name
            selectedMoodLayout.visibility = View.VISIBLE
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        }
    }

    /*
    delete moods
     */
    private fun deleteMoodEntry(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Mood Entry")
            .setMessage("Are you sure you want to delete this mood entry?")
            .setPositiveButton("Delete") { _, _ ->
                moodEntries.removeAt(position)
                moodEntryAdapter.notifyItemRemoved(position)
                saveMoodEntries()
                updateEmptyState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState() {
        if (moodEntries.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            moodEntriesRecyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            moodEntriesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun saveMoodEntries() {
        val entriesJson = gson.toJson(moodEntries)
        sharedPreferences.edit().putString("mood_entries", entriesJson).apply()
    }

    private fun loadMoodEntries() {
        val entriesJson = sharedPreferences.getString("mood_entries", null)
        if (entriesJson != null) {
            val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
            moodEntries = gson.fromJson(entriesJson, type)
            // Sort by timestamp descending (newest first)
            moodEntries.sortByDescending { it.timestamp }
        } else {
            moodEntries = mutableListOf()
        }
    }
    
    private fun showListView() {
        isCalendarViewActive = false
        val calendarContainer = view?.findViewById<ViewGroup>(R.id.calendar_container)
        calendarContainer?.visibility = View.GONE
        calendarView?.visibility = View.GONE
        moodEntriesRecyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = if (moodEntries.isEmpty()) View.VISIBLE else View.GONE
    }
    /*
    calender view
     */
    private fun showCalendarView() {
        isCalendarViewActive = true
        moodEntriesRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        
        val calendarContainer = view?.findViewById<ViewGroup>(R.id.calendar_container)
        calendarContainer?.visibility = View.VISIBLE
        
        if (calendarView == null) {
            calendarView = layoutInflater.inflate(R.layout.view_calendar, calendarContainer, false)
            calendarContainer?.addView(calendarView)
            setupCalendarView()
        } else {
            calendarView?.visibility = View.VISIBLE
        }
        
        updateCalendarMonth()
    }
    
    private fun setupCalendarView() {
        val prevMonthButton = calendarView?.findViewById<ImageButton>(R.id.prev_month_button)
        val nextMonthButton = calendarView?.findViewById<ImageButton>(R.id.next_month_button)
        
        prevMonthButton?.setOnClickListener {
            currentCalendarMonth.add(Calendar.MONTH, -1)
            updateCalendarMonth()
        }
        
        nextMonthButton?.setOnClickListener {
            currentCalendarMonth.add(Calendar.MONTH, 1)
            updateCalendarMonth()
        }
    }
    
    private fun updateCalendarMonth() {
        val currentMonthText = calendarView?.findViewById<TextView>(R.id.current_month_text)
        currentMonthText?.text = monthFormat.format(currentCalendarMonth.time)
        
        val calendarGrid = calendarView?.findViewById<RecyclerView>(R.id.calendar_grid)
        val days = generateCalendarDays()
        
        val adapter = CalendarAdapter(days) { day ->
            showDayDetails(day)
        }
        
        calendarGrid?.adapter = adapter
        calendarGrid?.layoutManager = GridLayoutManager(context, 7)
    }
    
    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val calendar = currentCalendarMonth.clone() as Calendar
        
        // Set to first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Add empty days for previous month
        val prevMonth = calendar.clone() as Calendar
        prevMonth.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        for (i in firstDayOfWeek - 1 downTo 1) {
            val day = daysInPrevMonth - i + 1
            days.add(CalendarDay(null, day.toString(), null, false, false))
        }
        
        // Add days of current month
        val today = Calendar.getInstance()
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = dateKeyFormat.format(calendar.time)
            val emoji = getMoodEmojiForDate(dateKey)
            val isToday = isSameDay(calendar, today)
            
            days.add(CalendarDay(calendar.clone() as Calendar, day.toString(), emoji, isToday, true))
        }
        
        // Add days from next month to fill grid
        val remainingDays = 42 - days.size // 6 rows * 7 days
        for (day in 1..remainingDays) {
            days.add(CalendarDay(null, day.toString(), null, false, false))
        }
        
        return days
    }
    
    private fun getMoodEmojiForDate(dateKey: String): String? {
        // Get the first mood entry for this date
        return moodEntries.firstOrNull { entry ->
            val entryDate = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
            dateKeyFormat.format(entryDate.time) == dateKey
        }?.emoji
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun showDayDetails(day: CalendarDay) {
        if (day.date == null) return
        
        val selectedDayCard = calendarView?.findViewById<CardView>(R.id.selected_day_card)
        val selectedDayTitle = calendarView?.findViewById<TextView>(R.id.selected_day_title)
        val selectedDayMoods = calendarView?.findViewById<RecyclerView>(R.id.selected_day_moods)
        
        val dateKey = dateKeyFormat.format(day.date.time)
        val dayMoods = moodEntries.filter { entry ->
            val entryDate = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
            dateKeyFormat.format(entryDate.time) == dateKey
        }
        
        if (dayMoods.isNotEmpty()) {
            selectedDayCard?.visibility = View.VISIBLE
            selectedDayTitle?.text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(day.date.time)
            
            val adapter = MoodEntryAdapter(dayMoods.toMutableList()) { position ->
                // Delete from main list
                val entryToDelete = dayMoods[position]
                val mainPosition = moodEntries.indexOf(entryToDelete)
                if (mainPosition >= 0) {
                    deleteMoodEntry(mainPosition)
                    updateCalendarMonth() // Refresh calendar
                }
            }
            
            selectedDayMoods?.adapter = adapter
            selectedDayMoods?.layoutManager = LinearLayoutManager(context)
        } else {
            selectedDayCard?.visibility = View.GONE
        }
    }
    
    private fun showStatistics() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MoodStatisticsFragment())
            .addToBackStack(null)
            .commit()
    }
}
