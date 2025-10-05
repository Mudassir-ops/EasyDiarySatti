package com.example.easydiarysatti.ui.model

import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import com.example.easydiarysatti.R
import com.google.android.material.textview.MaterialTextView
import com.kizitonwose.calendar.view.ViewContainer

class MonthViewContainer(view: View) : ViewContainer(view) {
    val legendLayout: LinearLayout = view.findViewById(R.id.legendLayout)
}

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView: MaterialTextView? = view.findViewById(R.id.calendarDayText)
    val imageView: AppCompatImageView? = view.findViewById(R.id.calendarDayIcon)
}