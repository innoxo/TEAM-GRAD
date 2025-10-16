package com.example.apptracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestAdapter : RecyclerView.Adapter<QuestAdapter.ViewHolder>() {

    private val quests = mutableListOf<QuestItem>()

    fun submitList(list: List<QuestItem>) {
        quests.clear()
        quests.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(quests[position])
    }

    override fun getItemCount() = quests.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvApp: TextView = itemView.findViewById(R.id.tvApp)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val progress: ProgressBar = itemView.findViewById(R.id.progressQuest)

        fun bind(item: QuestItem) {
            val label = "${item.appName} (${item.goalType} ${item.targetMinutes}분)"
            tvApp.text = "$label\n${item.deadlineDate} ${item.deadlineTime} 까지"

            val percent = if (item.targetMinutes == 0) 0 else
                (item.currentMinutes * 100 / item.targetMinutes).coerceAtMost(100)

            tvStatus.text =
                if (item.completed) "✅ 완료!" else "진행중 (${item.currentMinutes}분 / ${item.targetMinutes}분)"
            progress.progress = percent
        }
    }
}
