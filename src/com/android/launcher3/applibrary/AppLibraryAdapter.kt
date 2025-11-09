package com.android.launcher3.applibrary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import com.android.launcher3.model.data.AppInfo

class AppLibraryAdapter : RecyclerView.Adapter<AppLibraryAdapter.CategoryViewHolder>() {
    
    private val categories = mutableListOf<Pair<String, List<AppInfo>>>()
    
    fun updateCategories(newCategories: Map<String, List<AppInfo>>) {
        categories.clear()
        categories.addAll(newCategories.toList())
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_library_card, parent, false)
        return CategoryViewHolder(view as AppLibraryCardView)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val (category, apps) = categories[position]
        holder.bind(category, apps)
    }
    
    override fun getItemCount(): Int = categories.size
    
    class CategoryViewHolder(private val cardView: AppLibraryCardView) : RecyclerView.ViewHolder(cardView) {
        fun bind(category: String, apps: List<AppInfo>) {
            cardView.setCategory(category, apps)
        }
    }
}
