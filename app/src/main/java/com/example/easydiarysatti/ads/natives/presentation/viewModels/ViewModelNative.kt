package com.example.easydiarysatti.ads.natives.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.ads.natives.domain.useCases.UseCaseNative
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject // MUST BE javax, NOT jakarta

@HiltViewModel
class ViewModelNative @Inject constructor(
    private val useCaseNative: UseCaseNative
) : ViewModel() {

    private val _adViewLiveData = MutableLiveData<NativeAd?>()
    val adViewLiveData: LiveData<NativeAd?> get() = _adViewLiveData

    private val _loadFailedLiveData = MutableLiveData<Unit>()
    val loadFailedLiveData: LiveData<Unit> get() = _loadFailedLiveData

    private val _clearViewLiveData = MutableLiveData<Unit>()
    val clearViewLiveData: LiveData<Unit> get() = _clearViewLiveData

    fun loadNativeAd(nativeAdKey: NativeAdKey) = viewModelScope.launch {
        useCaseNative.loadNativeAd(nativeAdKey) { itemNativeAd ->
            itemNativeAd?.let {
                _adViewLiveData.postValue(it.nativeAd)
            } ?: run {
                _loadFailedLiveData.postValue(Unit)
            }
        }
    }

    fun destroyNative(nativeAdKey: NativeAdKey) = viewModelScope.launch(Dispatchers.Default) {
        if (useCaseNative.destroyNative(nativeAdKey)) {
            _clearViewLiveData.postValue(Unit)
        }
    }
}