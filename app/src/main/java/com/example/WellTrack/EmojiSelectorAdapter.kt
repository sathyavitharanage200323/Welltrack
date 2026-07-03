package com.example.WellTrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmojiSelectorAdapter(
    private val moods: List<MoodOption>,
    private val onEmojiSelected: (MoodOption) -> Unit
) : RecyclerView.Adapter<EmojiSelectorAdapter.EmojiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emoji_selector, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val mood = moods[position]
        holder.emojiText.text = mood.emoji
        holder.emojiName.text = mood.name
        holder.itemView.setOnClickListener {
            onEmojiSelected(mood)
        }
    }

    override fun getItemCount(): Int = moods.size

    class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiText: TextView = itemView.findViewById(R.id.emoji_text)
        val emojiName: TextView = itemView.findViewById(R.id.emoji_name)
    }
}
