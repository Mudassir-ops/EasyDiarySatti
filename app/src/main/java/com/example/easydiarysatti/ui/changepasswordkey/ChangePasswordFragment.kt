package com.example.easydiarysatti.ui.changepasswordkey

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentChangePasswordBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {
    private val viewModel by viewModels<ChangePasswordViewModel>()
    private val binding by viewBinding(FragmentChangePasswordBinding::bind)

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScreenUi()
        setupBgTheme()
    }

    private fun setupBgTheme() {
        binding?.parentView?.loadBackground(
            resourceId = sessionManagerRepo.getBgTheme(),
            placeholder = R.drawable.theme_1
        )
    }

    private fun setupScreenUi() {
        binding?.apply {
            oldPwdKeyLayout.txtSetPasswordDescription.text = getString(R.string.enter_old_passkey)
            newPwdKeyLayout.txtSetPasswordDescription.text = getString(R.string.enter_new_passkey)
            confirmPwdKeyLayout.txtSetPasswordDescription.text =
                getString(R.string.confirm_new_passkey)
        }
    }
}