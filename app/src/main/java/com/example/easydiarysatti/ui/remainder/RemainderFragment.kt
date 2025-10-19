package com.example.easydiarysatti.ui.remainder

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.easydiarysatti.R
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

    private val dailyReminderAdapter by lazy {
        ReminderAdapter({

        })
    }

    private val noteReminderAdapter by lazy {
        ReminderAdapter({

        })
    }

    lateinit var calendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        calendar = Calendar.getInstance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRemainderRv()
        clickListener()
        viewModel.observeReminders()
        observeReminders()
    }

    private fun clickListener() {
        binding?.apply {
            txtAddNew.setOnClickListener {
                txtAddNew.setOnClickListener {
                    showDatePickerWithTime { selectedCalendar ->
                        val uniqueId = UUID.randomUUID().hashCode()
                        val now = System.currentTimeMillis()
                        if (selectedCalendar.timeInMillis > now) {
                            val formattedDate =
                                selectedCalendar.time.toFormattedString("dd-MM-yy | h:mm a")
                            viewModel.insertReminder(
                                ReminderEntity(
                                    id = uniqueId,
                                    description = "",
                                    formattedDate = formattedDate,
                                    scheduleAt = selectedCalendar.timeInMillis,
                                    shouldPlay = true,
                                    noteReminder = false
                                )
                            )
                            binding?.parentLayout?.showSnackbar(
                                getString(
                                    R.string.reminder_set_for_time, formattedDate
                                )
                            )
                        } else {
                            selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                            val formattedDate =
                                selectedCalendar.time.toFormattedString("dd-MM-yy | h:mm a")
                            viewModel.insertReminder(
                                ReminderEntity(
                                    id = uniqueId,
                                    description = "",
                                    formattedDate = formattedDate,
                                    scheduleAt = selectedCalendar.timeInMillis,
                                    shouldPlay = true,
                                    noteReminder = false
                                )
                            )
                            binding?.parentLayout?.showSnackbar(getString(R.string.reminder_set_for_future_date))
                        }
                        activity.setReminderEasyDiary(
                            calendar = selectedCalendar,
                            text = getString(R.string.its_time_to_log_your_day_diary),
                            uniqueId = uniqueId
                        )
                    }
                }
            }
        }
    }

    private fun setupRemainderRv() {
        binding?.apply {
            reminderRecyclerView.run {
                adapter = dailyReminderAdapter
                hasFixedSize()
            }

            reminderNotesRecyclerView.run {
                adapter = noteReminderAdapter
                hasFixedSize()
            }
        }
    }

    fun observeReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reminderState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    state?.let {
                        if (it.noteReminders.isEmpty()) {
                            binding?.tvNoNotesData?.visibility = View.VISIBLE
                            binding?.reminderNotesRecyclerView?.visibility = View.GONE
                        } else {
                            binding?.tvNoNotesData?.visibility = View.GONE
                            binding?.reminderNotesRecyclerView?.visibility = View.VISIBLE
                            noteReminderAdapter.submitList(it.noteReminders)
                        }

                        if (it.dailyReminders.isEmpty()) {
                            binding?.tvNoData?.visibility = View.VISIBLE
                            binding?.reminderRecyclerView?.visibility = View.GONE
                        } else {
                            binding?.tvNoData?.visibility = View.GONE
                            binding?.reminderRecyclerView?.visibility = View.VISIBLE
                            dailyReminderAdapter.submitList(it.dailyReminders)
                        }
                    }
                }
        }
    }

}