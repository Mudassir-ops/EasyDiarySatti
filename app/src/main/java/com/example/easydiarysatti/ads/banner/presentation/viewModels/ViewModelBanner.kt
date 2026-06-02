package com.example.easydiarysatti.ads.banner.presentation.viewModels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.ads.banner.domain.useCases.UseCaseBanner
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.google.android.gms.ads.AdView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModelBanner @Inject constructor(
    private val useCaseBanner: UseCaseBanner
) : ViewModel() {

    // Store multiple ads: Key -> AdView
    private val adMap = mutableMapOf<BannerAdKey, AdView>()

    // LiveData now emits the Map so fragments can find their specific ad
    private val _adMapLiveData = MutableLiveData<Map<BannerAdKey, AdView>>()
    val adMapLiveData: LiveData<Map<BannerAdKey, AdView>> get() = _adMapLiveData

    private val _loadFailedLiveData = MutableLiveData<Unit>()
    val loadFailedLiveData: LiveData<Unit> get() = _loadFailedLiveData

    fun loadBannerAd(adView: AdView, bannerAdKey: BannerAdKey, context: Context) =
        viewModelScope.launch {
            useCaseBanner.loadBannerAd(adView, context, bannerAdKey) { itemBannerAd ->
                itemBannerAd?.let {
                    // Save to map using the key as a unique identifier
                    adMap[bannerAdKey] = it.adView
                    _adMapLiveData.postValue(adMap)
                } ?: run {
                    _loadFailedLiveData.value = Unit
                }
            }
        }

    fun destroyBanner(bannerAdKey: BannerAdKey) = viewModelScope.launch {
        if (useCaseBanner.destroyBanner(bannerAdKey)) {
            adMap.remove(bannerAdKey) // Remove specifically this key
            _adMapLiveData.postValue(adMap)
        }
    }
}