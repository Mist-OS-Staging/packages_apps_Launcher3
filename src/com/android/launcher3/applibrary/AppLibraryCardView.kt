package com.android.launcher3.applibrary

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.launcher3.R
import com.android.launcher3.model.data.AppInfo

class AppLibraryCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val categoryTitle: TextView
    private val appGrid: GridLayout
    
    init {
        LayoutInflater.from(context).inflate(R.layout.app_library_card, this, true)
        categoryTitle = findViewById(R.id.category_title)
        appGrid = findViewById(R.id.app_grid)
        orientation = VERTICAL
        
        // Set rounded background
        setBackgroundResource(R.drawable.app_library_card_background)
    }
    
    fun setCategory(category: String, apps: List<AppInfo>) {
        categoryTitle.text = category
        appGrid.removeAllViews()
        
        // Show up to 4 apps in 2x2 grid
        val appsToShow = apps.take(4)
        for (i in appsToShow.indices) {
            val app = appsToShow[i]
            val iconView = ImageView(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = resources.getDimensionPixelSize(R.dimen.app_library_icon_size)
                    height = resources.getDimensionPixelSize(R.dimen.app_library_icon_size)
                    setMargins(8, 8, 8, 8)
                }
                setImageDrawable(app.iconBitmap)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            appGrid.addView(iconView)
        }
        
        // Fill remaining slots with empty views if needed
        while (appGrid.childCount < 4) {
            val emptyView = ImageView(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = resources.getDimensionPixelSize(R.dimen.app_library_icon_size)
                    height = resources.getDimensionPixelSize(R.dimen.app_library_icon_size)
                    setMargins(8, 8, 8, 8)
                }
                alpha = 0.3f
            }
            appGrid.addView(emptyView)
        }
    }
}
