package com.example.WellTrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MoodEntryAdapter(
    private val moodEntries: MutableList<MoodEntry>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<MoodEntryAdapter.MoodEntryViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_entry, parent, false)
        return MoodEntryViewHolder(view)
    }

    /*
    display moods
     */
    override fun onBindViewHolder(holder: MoodEntryViewHolder, position: Int) {
        val entry = moodEntries[position]
        holder.moodEmoji.text = entry.emoji
        holder.moodName.text = entry.moodName
        holder.moodTimestamp.text = formatTimestamp(entry.timestamp)
        
        if (entry.note.isNotBlank()) {
            holder.moodNote.visibility = View.VISIBLE
            holder.moodNote.text = entry.note
        } else {
            holder.moodNote.visibility = View.GONE
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = moodEntries.size

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val today = Calendar.getInstance()
        val entryDate = Calendar.getInstance().apply { time = date }
        
        return when {
            isSameDay(today, entryDate) -> "Today at ${timeFormat.format(date)}"
            isYesterday(today, entryDate) -> "Yesterday at ${timeFormat.format(date)}"
            else -> "${dateFormat.format(date)} at ${timeFormat.format(date)}"
        }
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun isYesterday(today: Calendar, date: Calendar): Boolean {
        val yesterday = today.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, date)
    }

    class MoodEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val moodEmoji: TextView = itemView.findViewById(R.id.mood_emoji)
        val moodName: TextView = itemView.findViewById(R.id.mood_name)
        val moodTimestamp: TextView = itemView.findViewById(R.id.mood_timestamp)
        val moodNote: TextView = itemView.findViewById(R.id.mood_note)
        val deleteButton: ImageView = itemView.findViewById(R.id.delete_mood_button)
    }
}
