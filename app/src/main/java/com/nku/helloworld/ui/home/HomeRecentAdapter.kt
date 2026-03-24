package com.nku.helloworld.ui.home

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nku.helloworld.databinding.ItemHomeRecentBinding

data class HomeRecentItem(
    val title: String,
    val category: String,
    val time: String,
    val colorRes: Int,
)

class HomeRecentAdapter(
    private val items: List<HomeRecentItem>,
) : RecyclerView.Adapter<HomeRecentAdapter.HomeRecentViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HomeRecentViewHolder {
        val binding = ItemHomeRecentBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return HomeRecentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeRecentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HomeRecentViewHolder(
        private val binding: ItemHomeRecentBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeRecentItem) {
            binding.titleText.text = item.title
            binding.categoryText.text = item.category
            binding.timeText.text = item.time
            val color = ContextCompat.getColor(binding.root.context, item.colorRes)
            binding.statusDot.backgroundTintList = ColorStateList.valueOf(color)
        }
    }
}

