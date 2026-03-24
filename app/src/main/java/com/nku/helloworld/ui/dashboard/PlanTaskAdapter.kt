package com.nku.helloworld.ui.dashboard

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nku.helloworld.R
import com.nku.helloworld.databinding.ItemPlanTaskBinding

data class PlanTaskItem(
    val title: String,
    val meta: String,
    val status: String,
    val colorRes: Int,
)

class PlanTaskAdapter(
    private val items: List<PlanTaskItem>,
) : RecyclerView.Adapter<PlanTaskAdapter.PlanTaskViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PlanTaskViewHolder {
        val binding = ItemPlanTaskBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return PlanTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanTaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PlanTaskViewHolder(
        private val binding: ItemPlanTaskBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlanTaskItem) {
            binding.taskTitle.text = item.title
            binding.taskMeta.text = item.meta
            binding.taskStatus.text = item.status

            val color = ContextCompat.getColor(binding.root.context, item.colorRes)
            binding.priorityBar.backgroundTintList = ColorStateList.valueOf(color)
            binding.taskStatus.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, chipColorFor(item.colorRes)),
            )
            binding.taskStatus.setTextColor(color)
        }

        private fun chipColorFor(statusColorRes: Int): Int {
            return when (statusColorRes) {
                R.color.status_done -> R.color.brand_green_soft
                R.color.status_pending -> R.color.brand_orange_soft
                else -> R.color.brand_primary_soft
            }
        }
    }
}

