package com.example.WellTrack

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MoodStatisticsFragment : Fragment() {

    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val gson = Gson()
    private lateinit var moodEntries: List<MoodEntry>
    
    private lateinit var weeklyLineChart: LineChart
    private lateinit var moodPieChart: PieChart
    private lateinit var weekRangeText: TextView
    private lateinit var totalEntriesText: TextView
    private lateinit var mostCommonMoodText: TextView
    private lateinit var weekEntriesText: TextView
    
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mood_statistics, container, false)

        sharedPreferences = UserDataManager.getUserSpecificPreferences(requireContext(), "mood_journal")
        loadMoodEntries()

        // Initialize views
        weeklyLineChart = view.findViewById(R.id.weekly_line_chart)
        moodPieChart = view.findViewById(R.id.mood_pie_chart)
        weekRangeText = view.findViewById(R.id.week_range_text)
        totalEntriesText = view.findViewById(R.id.total_entries_text)
        mostCommonMoodText = view.findViewById(R.id.most_common_mood_text)
        weekEntriesText = view.findViewById(R.id.week_entries_text)
        
        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupWeeklyLineChart()
        setupMoodPieChart()
        updateSummaryStats()

        return view
    }

    private fun loadMoodEntries() {
        val entriesJson = sharedPreferences.getString("mood_entries", null)
        moodEntries = if (entriesJson != null) {
            val type = object : TypeToken<List<MoodEntry>>() {}.type
            gson.fromJson(entriesJson, type)
        } else {
            emptyList()
        }
    }

    /*
    line chart
     */
    private fun setupWeeklyLineChart() {
        val calendar = Calendar.getInstance()
        val today = calendar.clone() as Calendar
        
        // Get last 7 days
        val last7Days = mutableListOf<String>()
        val dayLabels = mutableListOf<String>()
        
        for (i in 6 downTo 0) {
            calendar.time = today.time
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            last7Days.add(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time))
            dayLabels.add(SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time))
        }
        
        // Set week range text
        calendar.time = today.time
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = dateFormat.format(calendar.time)
        val endDate = dateFormat.format(today.time)
        weekRangeText.text = "$startDate - $endDate, ${SimpleDateFormat("yyyy", Locale.getDefault()).format(today.time)}"
        
        // Count entries per day
        val entriesPerDay = mutableMapOf<String, Int>()
        last7Days.forEach { date ->
            entriesPerDay[date] = moodEntries.count { entry ->
                val entryDate = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(entryDate.time) == date
            }
        }
        
        // Create chart entries
        val chartEntries = mutableListOf<Entry>()
        last7Days.forEachIndexed { index, date ->
            chartEntries.add(Entry(index.toFloat(), entriesPerDay[date]?.toFloat() ?: 0f))
        }
        
        val dataSet = LineDataSet(chartEntries, "Mood Entries").apply {
            color = Color.parseColor("#2196F3")
            setCircleColor(Color.parseColor("#2196F3"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        val lineData = LineData(dataSet)
        
        weeklyLineChart.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = true
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(dayLabels)
                granularity = 1f
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
            }
            
            axisRight.isEnabled = false
            
            animateX(1000)
            invalidate()
        }
    }

    /*
    mood pie chart
     */
    private fun setupMoodPieChart() {
        val calendar = Calendar.getInstance()
        val last7DaysEntries = moodEntries.filter { entry ->
            val entryDate = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
            val daysDiff = ((calendar.timeInMillis - entryDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            daysDiff <= 6
        }
        
        if (last7DaysEntries.isEmpty()) {
            moodPieChart.setNoDataText("No mood entries in the last 7 days")
            moodPieChart.invalidate()
            return
        }
        
        // Count mood frequencies
        val moodCounts = mutableMapOf<String, Int>()
        last7DaysEntries.forEach { entry ->
            val key = "${entry.emoji} ${entry.moodName}"
            moodCounts[key] = (moodCounts[key] ?: 0) + 1
        }
        
        // Create pie entries
        val pieEntries = moodCounts.map { (mood, count) ->
            PieEntry(count.toFloat(), mood)
        }
        
        val dataSet = PieDataSet(pieEntries, "Moods").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        
        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(moodPieChart))
        }
        
        moodPieChart.apply {
            data = pieData
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 40f
            transparentCircleRadius = 45f
            setDrawEntryLabels(false)
            legend.isEnabled = true
            setUsePercentValues(true)
            animateY(1000)
            invalidate()
        }
    }

    /*
    summary mood
     */
    private fun updateSummaryStats() {
        // Total entries
        totalEntriesText.text = moodEntries.size.toString()
        
        // Entries this week
        val calendar = Calendar.getInstance()
        val weekEntries = moodEntries.filter { entry ->
            val entryDate = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
            val daysDiff = ((calendar.timeInMillis - entryDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            daysDiff <= 6
        }
        weekEntriesText.text = weekEntries.size.toString()
        
        // Most common mood
        if (weekEntries.isNotEmpty()) {
            val moodCounts = mutableMapOf<String, Int>()
            weekEntries.forEach { entry ->
                val key = "${entry.emoji} ${entry.moodName}"
                moodCounts[key] = (moodCounts[key] ?: 0) + 1
            }
            val mostCommon = moodCounts.maxByOrNull { it.value }
            mostCommonMoodText.text = mostCommon?.key ?: "N/A"
        } else {
            mostCommonMoodText.text = "N/A"
        }
    }
}
