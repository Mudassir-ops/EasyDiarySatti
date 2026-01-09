package com.example.easydiarysatti.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.NOTE_ID
import com.example.easydiarysatti.R
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.FragmentCalenderBinding
import com.example.easydiarysatti.dateFormatter
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.getShortDisplayNameCompat
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setCustomDayEmojiBackground
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CalenderFragment : Fragment(R.layout.fragment_calender) {
    private val binding by viewBinding(FragmentCalenderBinding::bind)
    private val viewModel by viewModels<CalenderViewModel>()
    private var shimmerAdapter: ShimmerCalenderAdapter? = null
    private var shimmerAdapterNotes: ShimmerAdapter? = null

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    // Inside CalenderFragment.kt
    private val calenderItemAdapter: CalenderItemAdapter by lazy {
        CalenderItemAdapter(onNoteItemClick = { note ->
            findNavController().navigate(
                R.id.previewFragment2,
                Bundle().apply {
                    putLong(NOTE_ID, note.noteId)
                }
            )
        })
    }

    private val formatter by lazy { dateFormatter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialCalenderPageSetup()
        setupCurrentDate()
        setupBgTheme()
    }
    private fun applyDynamicTheme(themeResId: Int?) {
        val themeColor = when (themeResId) {
            R.drawable.theme_1 -> ContextCompat.getColor(requireContext(), R.color.theme1_color)
            R.drawable.theme_2 -> ContextCompat.getColor(requireContext(), R.color.theme2_color)
            R.drawable.theme_3 -> ContextCompat.getColor(requireContext(), R.color.theme3_color)
            R.drawable.theme_4 -> ContextCompat.getColor(requireContext(), R.color.theme4_color)
            R.drawable.theme_5 -> ContextCompat.getColor(requireContext(), R.color.theme5_color)
            else -> ContextCompat.getColor(requireContext(), R.color.app_primary_color)
        }

        binding?.apply {

            // 1. Update the Icon Tint
            ivCalender.imageTintList = android.content.res.ColorStateList.valueOf(themeColor)

            // 2. Update the Text Color
            tvMonth.setTextColor(themeColor)
        }
    }
    private fun setupBgTheme() {
        val currentTheme = sessionManagerRepo.getBgTheme()

        // Apply colors to FAB and Bottom Nav
        applyDynamicTheme(currentTheme)
    }
    private fun setupCalender() {
        setupCustomCalenderMonth()
        customCalenderDaySetup()
    }

    private fun setupRecyclerView() {
        binding?.rvCalenderNotes?.run {
            adapter = calenderItemAdapter
            hasFixedSize()
        }
    }

    private fun observeAllNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { state ->
                if (!state.isLoading) {
                    if (state.notesByDate[state.selectedDay]?.isEmpty() == false) {
                        binding?.tvNoData?.visibility = View.GONE
                        binding?.rvCalenderNotes?.visibility = View.VISIBLE
                        setupRecyclerView()
                        calenderItemAdapter.submitList(
                            state.notesByDate[state.selectedDay] ?: emptyList()
                        )
                    } else {
                        binding?.tvNoData?.visibility = View.VISIBLE
                        binding?.rvCalenderNotes?.visibility = View.GONE
                    }
                    binding?.calendarView?.notifyCalendarChanged()
                }
            }
        }
    }

    private fun initialCalenderPageSetup() {
        shimmerAdapter = ShimmerCalenderAdapter(30)
        binding?.rvCalendarShimmer?.adapter = shimmerAdapter
        binding?.shimmerLayout?.startShimmer()
        setupCalender()
        val currentMonth = YearMonth.now()
        viewModel.loadMonth(currentMonth)
        viewModel.currentDayNotes()
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

    private fun setupCustomCalenderMonth() {
        binding?.calendarView?.apply {
            monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    if (container.legendLayout.tag == null) {
                        container.legendLayout.tag = data.yearMonth
                        val daysOfWeek = daysOfWeek()
                        container.legendLayout.children.forEachIndexed { index, view ->
                            (view as TextView).text = daysOfWeek[index].getShortDisplayNameCompat()
                        }
                    }
                }
            }
        }
    }

    private fun customCalenderDaySetup() {
        binding?.calendarView?.apply {
            val currentMonth = YearMonth.now()
            val firstMonth = currentMonth.minusMonths(12)
            val lastMonth = currentMonth.plusMonths(12)
            val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            setup(firstMonth, lastMonth, firstDayOfWeek)
            dayBinder = object : MonthDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, data: CalendarDay) {
                    val notesForDay = viewModel.uiState.value.notesByDate[data.date]
                    val noteEntity = notesForDay?.firstOrNull()
                    val isToday = data.date == LocalDate.now()
                    container.styleCalenderCurrentDay(
                        noteEntity = noteEntity,
                        isToday = isToday
                    )
                    container.textView?.text = data.date.dayOfMonth.toString()
                    container.parentLayout?.setOnClickListener {
                        data.onDayClick()
                    }
                }
            }
            monthScrollListener = { month ->
                month.monthScrollListener()
            }
            currentMonth.setupDynamicCalenderView()
        }
    }

    private fun CalendarMonth.monthScrollListener() {
        val yearMonth = this.yearMonth
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        binding?.tvMonth?.text = monthName

        viewModel.loadMonth(yearMonth)
    }

    private fun YearMonth.setupDynamicCalenderView() {
        val rowHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._57sdp)
        binding?.calendarView?.apply {
            layoutParams.height = rowHeight * 4
            scrollToMonth(this@setupDynamicCalenderView)
        }
    }

    private fun DayViewContainer.styleCalenderCurrentDay(
        noteEntity: CreateNoteEntity?,
        isToday: Boolean
    ) {
        if (noteEntity?.feelingEmojiRes != null) {
            textView?.visibility = View.GONE
            imageView?.visibility = View.VISIBLE
            imageView?.setImageResource(noteEntity.feelingEmojiRes)

            imageView?.setCustomDayEmojiBackground(
                fillColor = noteEntity.selectedEmojiColor,
                strokeColor = noteEntity.selectedEmojiColor,
                dayNow = isToday
            )
        } else {
            imageView?.visibility = View.GONE
            textView?.visibility = View.VISIBLE
        }
    }

    private fun setupCurrentDate() {
        val today = LocalDate.now()
        val formatter = dateFormatter()
        val formattedDate = today.format(formatter)
        binding?.tvOnGoingItemLabel1?.text = formattedDate
        val monthName = today.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
        binding?.tvMonth?.text = monthName
    }

    private fun CalendarDay.onDayClick() {
        val formattedDate = date.format(formatter)
        binding?.tvOnGoingItemLabel1?.text = formattedDate
        Log.e("currentMonth", "setupCalender: $formattedDate")
        viewModel.selectDay(date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.shimmerLayout?.stopShimmer()
        shimmerAdapterNotes = null
        shimmerAdapter = null
    }

}


