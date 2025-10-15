package com.example.easydiarysatti.ui.remainder

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentRemainderBinding
import com.example.easydiarysatti.setReminderEasyDiary
import com.example.easydiarysatti.showDatePickerWithTime
import com.example.easydiarysatti.showToast
import com.example.easydiarysatti.toFormattedString
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.UUID

@AndroidEntryPoint
class RemainderFragment : Fragment(R.layout.fragment_remainder) {
    private val binding by viewBinding(FragmentRemainderBinding::bind)
    private val viewModel by viewModels<RemainderViewModel>()
    private var adapter: ReminderAdapter? = null
    lateinit var calendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        calendar = Calendar.getInstance()
        adapter = ReminderAdapter(list = listOf(), context = context ?: return, onItemClick = {

        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRemainderRv()
        clickListener()
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
                            calendar = selectedCalendar,
                            text = "Mudassir Here",
                            uniqueId = uniqueId
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
                adapter = adapter
                hasFixedSize()
            }
        }
    }

}