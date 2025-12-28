package com.example.easydiarysatti.ui.notifications


import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.easydiarysatti.R
import com.kizitonwose.calendar.view.ViewContainer

class DayViewContainer(view: View) : ViewContainer(view) {
    // These IDs must match your calendar day layout XML
    val textView: TextView? = view.findViewById(R.id.calendarDayText)
    val imageView: ImageView? = view.findViewById(R.id.calendarDayIcon)
    val parentLayout: View? = view // Or find specific clickable container
}