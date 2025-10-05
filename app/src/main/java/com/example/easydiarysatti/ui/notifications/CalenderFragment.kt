package com.example.easydiarysatti.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.easydiarysatti.DrawablePosition
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentCalenderBinding
import com.example.easydiarysatti.dpToPx
import com.example.easydiarysatti.setRoundedBgColors
import com.example.easydiarysatti.setVectorDrawable
import com.example.easydiarysatti.ui.model.DayViewContainer
import com.example.easydiarysatti.ui.model.MonthViewContainer
import com.example.easydiarysatti.viewBinding
import com.google.android.material.textview.MaterialTextView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@AndroidEntryPoint
class CalenderFragment : Fragment(R.layout.fragment_calender) {
    private val binding by viewBinding(FragmentCalenderBinding::bind)
    private val viewModel by viewModels<CalenderViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100) // 100ms delay lets the UI render smoothly
            setupCalender()
        }

    }

    private fun setupCalender() {

        binding?.calendarView?.apply {
            monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)

                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    if (container.legendLayout.tag == null) {
                        container.legendLayout.tag = data.yearMonth
                        val daysOfWeek = daysOfWeek()
                        container.legendLayout.children.forEachIndexed { index, view ->
                            (view as TextView).text =
                                daysOfWeek[index].getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.getDefault()
                                )
                        }
                    }
                }
            }
        }

        binding?.calendarView?.apply {
            val currentMonth = YearMonth.now()
            val firstMonth = currentMonth.minusMonths(12)
            val lastMonth = currentMonth.plusMonths(12)
            val firstDayOfWeek =
                DayOfWeek.MONDAY

            setup(firstMonth, lastMonth, firstDayOfWeek)
            Log.e("currentMonth", "setupCalender: $currentMonth")


            dayBinder = object : MonthDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, day: CalendarDay) {
                    container.textView?.setOnClickListener {
                        val formatter =
                            DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.getDefault())
                        val formattedDate = day.date.format(formatter)
                        binding?.tvOnGoingItemLabel1?.text = formattedDate
                    }

                    val today = LocalDate.now()
                    val formatter = DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.getDefault())
                    binding?.tvOnGoingItemLabel1?.text = today.format(formatter)

                    container.textView?.text = day.date.dayOfMonth.toString()
                    if (day.date == LocalDate.now()) {
                        //   container.textView.text = "\uD83C\uDF1F"
                        container.textView?.visibility = View.GONE
                        container.imageView?.visibility = View.VISIBLE

                        container.textView?.apply {
//                            setBackgroundResource(R.drawable.bg_rounded_day)
//                            setRoundedBgColors(
//                                solidColor = ContextCompat.getColor(
//                                    context,
//                                    R.color.app_primary_color
//                                ),
//                                strokeColor = ContextCompat.getColor(context, R.color.black),
//                                strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
//                            )
//                            text = ""
//
//                            val drawable = ContextCompat.getDrawable(context, R.drawable.emooji_smilie)
//                            drawable?.setBounds(0, 0, 60, 60) // width, height in px
//                            container.textView.setCompoundDrawables(drawable, null, null, null)

                        }

                        // container.textView.setBackgroundResource(R.drawable.bg_rounded_current_day)
                    } else {
                        container.textView?.setBackgroundResource(R.drawable.bg_rounded_day)
                    }

                }
            }
            monthScrollListener = { month ->
                val yearMonth = month.yearMonth
                val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                val year = yearMonth.year
                binding?.tvMonth?.text = monthName
            }
            val rowHeight = resources.getDimensionPixelSize(R.dimen.activity_day_height)
            binding?.calendarView?.layoutParams?.height = rowHeight * 4
            scrollToMonth(currentMonth)
        }

    }
}


