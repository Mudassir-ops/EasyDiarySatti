package com.example.easydiarysatti.ui.remainder

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.easydiarysatti.R
import com.example.easydiarysatti.cancelAlarm
import com.example.easydiarysatti.data.local.ReminderEntity
import com.example.easydiarysatti.databinding.FragmentRemainderBinding
import com.example.easydiarysatti.setReminderEasyDiary
import com.example.easydiarysatti.showDatePickerWithTime
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.toFormattedString
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

@AndroidEntryPoint
class RemainderFragment : Fragment(R.layout.fragment_remainder) {
    private val binding by viewBinding(FragmentRemainderBinding::bind)
    private val viewModel by viewModels<RemainderViewModel>()

    private val reminderAdapter by lazy {
        ReminderAdapter { reminder ->
            viewModel.deleteReminder(reminder)
            activity.cancelAlarm(uniqueId = reminder.id)
        }
    }

    private lateinit var calendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        calendar = Calendar.getInstance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupReminderRv()
        setupClickListeners()
        viewModel.observeReminders()
        observeReminders()
    }

    private fun setupClickListeners() {
        binding?.txtAddNew?.setOnClickListener {
            showDatePickerWithTime { selectedCalendar ->
                val uniqueId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                val now = System.currentTimeMillis()
                val futureCalendar = if (selectedCalendar.timeInMillis > now) {
                    selectedCalendar
                } else {
                    selectedCalendar.apply { add(Calendar.DAY_OF_YEAR, 1) }
                }

                val formattedDate = futureCalendar.time.toFormattedString("dd-MM-yy | h:mm a")

                viewModel.insertReminder(
                    ReminderEntity(
                        id = uniqueId,
                        description = "",
                        formattedDate = formattedDate,
                        scheduleAt = futureCalendar.timeInMillis,
                        shouldPlay = true,
                        noteReminder = false
                    )
                )

                binding?.parentLayout?.showSnackbar(
                    getString(R.string.reminder_set_for_time, formattedDate)
                )

                activity.setReminderEasyDiary(
                    calendar = futureCalendar,
                    text = getString(R.string.its_time_to_log_your_day_diary),
                    uniqueId = uniqueId,
                    contentTitle = getString(R.string.daily_reminder)
                )
            }
        }
    }

    private fun setupReminderRv() {
        binding?.reminderRecyclerView?.run {
            adapter = reminderAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reminderState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    state?.let {
                        val combinedList = (it.dailyReminders + it.noteReminders)
                        if (combinedList.isEmpty()) {
                            binding?.tvNoData?.visibility = View.VISIBLE
                            binding?.reminderRecyclerView?.visibility = View.GONE
                        } else {
                            binding?.tvNoData?.visibility = View.GONE
                            binding?.reminderRecyclerView?.visibility = View.VISIBLE
                            reminderAdapter.submitList(combinedList.reversed())
                        }
                    }
                }
        }
    }
}
