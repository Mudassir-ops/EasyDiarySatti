package com.example.easydiarysatti.ui.uimodels

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnGoingScreenUiModel(val labelOne: String?, val labelTwo: String?, val imageRes: Int) :
    Parcelable