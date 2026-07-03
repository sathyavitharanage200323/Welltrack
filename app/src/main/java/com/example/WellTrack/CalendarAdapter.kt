package com.example.WellTrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

data class CalendarDay(
    val date: Calendar?,
    val dayNumber: String,
    val emoji: String?,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
)

class CalendarAdapter(
    private val days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarDayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        val day = days[position]
        
        holder.dayNumber.text = day.dayNumber
        
        // Show emoji if mood exists for this day
        if (day.emoji != null) {
            holder.dayEmoji.visibility = View.VISIBLE
            holder.dayEmoji.text = day.emoji
        } else {
            holder.dayEmoji.visibility = View.GONE
        }
        
        // Show today indicator
        if (day.isToday) {
            holder.todayIndicator.visibility = View.VISIBLE
        } else {
            holder.todayIndicator.visibility = View.GONE
        }
        
        // Dim days from other months
        if (!day.isCurrentMonth) {
            holder.dayNumber.alpha = 0.3f
            holder.dayEmoji.alpha = 0.3f
        } else {
            holder.dayNumber.alpha = 1.0f
            holder.dayEmoji.alpha = 1.0f
        }
        
        // Click listener
        holder.itemView.setOnClickListener {
            if (day.date != null && day.isCurrentMonth) {
                onDayClick(day)
            }
        }
    }

    override fun getItemCount(): Int = days.size

    class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayNumber: TextView = itemView.findViewById(R.id.day_number)
        val dayEmoji: TextView = itemView.findViewById(R.id.day_emoji)
        val todayIndicator: View = itemView.findViewById(R.id.today_indicator)
    }
}
