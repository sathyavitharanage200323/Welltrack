package com.example.WellTrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HabitAdapter(
    private val habits: MutableList<Habit>,
    private val completions: Map<String, Boolean>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    interface OnItemClickListener {
        fun onEditClick(position: Int)
        fun onDeleteClick(position: Int)
        fun onCompletionToggle(habitId: String, isCompleted: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            HabitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.habitName.text = habit.name
        
        val isCompleted = completions[habit.id] ?: false
        holder.habitCheckBox.isChecked = isCompleted
        
        holder.habitCheckBox.setOnCheckedChangeListener(null)
        holder.habitCheckBox.setOnCheckedChangeListener { _, isChecked ->
            listener.onCompletionToggle(habit.id, isChecked)
        }
        
        holder.editButton.setOnClickListener {
            listener.onEditClick(position)
        }
        holder.deleteButton.setOnClickListener {
            listener.onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = habits.size

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val habitName: TextView = itemView.findViewById(R.id.habit_name_text_view)
        val habitCheckBox: CheckBox = itemView.findViewById(R.id.habit_checkbox)
        val editButton: ImageView = itemView.findViewById(R.id.edit_button)
        val deleteButton: ImageView = itemView.findViewById(R.id.delete_button)
    }
}