package com.example.easydiarysatti.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.time.YearMonth
import java.time.DayOfWeek

import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentCalenderBinding
import com.example.easydiarysatti.getShortDisplayNameCompat
import com.example.easydiarysatti.ui.home.HomeNotesState
import com.example.easydiarysatti.ui.model.DayViewContainer
import com.example.easydiarysatti.ui.model.MonthViewContainer
import com.example.easydiarysatti.utills.ShimmerAdapter
import com.example.easydiarysatti.utills.ShimmerCalenderAdapter
import com.example.easydiarysatti.viewBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@AndroidEntryPoint
class CalenderFragment : Fragment(R.layout.fragment_calender) {
    private val binding by viewBinding(FragmentCalenderBinding::bind)
    private val viewModel by viewModels<CalenderViewModel>()
    private var shimmerAdapter: ShimmerCalenderAdapter? = null
    private var shimmerAdapterNotes: ShimmerAdapter? = null
    private val calenderItemAdapter: CalenderItemAdapter by lazy {
        CalenderItemAdapter(onNoteItemClick = { note ->
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            shimmerAdapter = ShimmerCalenderAdapter(30)
            binding?.rvCalendarShimmer?.adapter = shimmerAdapter
            binding?.shimmerLayout?.startShimmer()
            delay(100)
            setupCalender()
            observeAllNotes()
            binding?.apply {
                binding?.shimmerLayout?.visibility = View.GONE
                binding?.calendarView?.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f).setDuration(200).start()
                }
            }

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
                                daysOfWeek[index].getShortDisplayNameCompat()

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
                override fun bind(container: DayViewContainer, data: CalendarDay) {
                    container.textView?.setOnClickListener {
                        val formatter =
                            DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.getDefault())
                        val formattedDate = data.date.format(formatter)
                        binding?.tvOnGoingItemLabel1?.text = formattedDate
                    }

                    val today = LocalDate.now()
                    val formatter = DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.getDefault())
                    binding?.tvOnGoingItemLabel1?.text = today.format(formatter)

                    container.textView?.text = data.date.dayOfMonth.toString()
                    if (data.date == LocalDate.now()) {
                        //   container.textView.text = "\uD83C\uDF1F"
                        container.textView?.visibility = View.GONE
                        container.imageView?.visibility = View.VISIBLE
                    } else {
                        container.textView?.setBackgroundResource(R.drawable.bg_rounded_day)
                    }

                }
            }
            monthScrollListener = { month ->
                val yearMonth = month.yearMonth
                val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                binding?.tvMonth?.text = monthName
            }
            val rowHeight = resources.getDimensionPixelSize(R.dimen.activity_day_height)
            binding?.calendarView?.layoutParams?.height = rowHeight * 4
            scrollToMonth(currentMonth)
        }
    }


    private fun setupRecyclerView() {
        binding?.rvCalenderNotes?.run {
            adapter = calenderItemAdapter
            hasFixedSize()
        }
    }

    private fun observeAllNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allCalenderNotesState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    when (state) {
                        is HomeNotesState.Success -> {
                            setupRecyclerView()
                            calenderItemAdapter.submitList(state.notes)
                        }

                        is HomeNotesState.Error -> {

                        }

                        else -> {
                            shimmerAdapterNotes = ShimmerAdapter(10)
                            binding?.rvCalendarShimmer?.adapter = shimmerAdapterNotes
                        }
                    }
                }
        }
    }
}


