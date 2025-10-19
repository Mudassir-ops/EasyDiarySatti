package com.example.easydiarysatti.ui.remainder

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentRemainderBinding
import com.example.easydiarysatti.setReminderEasyDiary
import com.example.easydiarysatti.showDatePickerWithTime
import com.example.easydiarysatti.showToast
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
                        if (selectedCalendar.timeInMillis <= System.currentTimeMillis()) {
                            selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                        }

                        activity.setReminderEasyDiary(
                            calendar = selectedCalendar, text = "Mudassir Here", uniqueId = uniqueId
                        )
                        val formattedDate =
                            selectedCalendar.time.toFormattedString("dd/MM/yyyy hh:mm a")
                        showToast(requireContext(), "Reminder set for $formattedDate")
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