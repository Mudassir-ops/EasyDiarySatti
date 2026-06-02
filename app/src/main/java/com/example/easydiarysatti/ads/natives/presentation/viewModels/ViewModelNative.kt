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

    // 1. Change to a Map to store multiple preloaded ads
    private val adMap = HashMap<NativeAdKey, NativeAd>()

    private val _adMapLiveData = MutableLiveData<Map<NativeAdKey, NativeAd>>()
    val adMapLiveData: LiveData<Map<NativeAdKey, NativeAd>> get() = _adMapLiveData

    private val _loadFailedLiveData = MutableLiveData<NativeAdKey>()
    val loadFailedLiveData: LiveData<NativeAdKey> get() = _loadFailedLiveData

    fun loadNativeAd(nativeAdKey: NativeAdKey) = viewModelScope.launch {
        useCaseNative.loadNativeAd(nativeAdKey) { itemNativeAd ->
            itemNativeAd?.let {
                // 2. Store ad with its specific key
                adMap[nativeAdKey] = it.nativeAd
                _adMapLiveData.postValue(adMap)
            } ?: run {
                _loadFailedLiveData.postValue(nativeAdKey)
            }
        }
    }


}