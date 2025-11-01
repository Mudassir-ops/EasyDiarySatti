package com.example.easydiarysatti.ads.banner.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.ads.banner.domain.useCases.UseCaseBanner
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch

class ViewModelBanner(private val useCaseBanner: UseCaseBanner) : ViewModel() {

    private val _adViewLiveData = MutableLiveData<AdView>()
    val adViewLiveData: LiveData<AdView> get() = _adViewLiveData

    private val _loadFailedLiveData = MutableLiveData<Unit>()
    val loadFailedLiveData: LiveData<Unit> get() = _loadFailedLiveData

    private val _clearViewLiveData = MutableLiveData<Unit>()
    val clearViewLiveData: LiveData<Unit> get() = _clearViewLiveData

    fun loadBannerAd(adView: AdView, bannerAdKey: BannerAdKey) = viewModelScope.launch {
        useCaseBanner.loadBannerAd(adView, bannerAdKey) { itemBannerAd ->
            itemBannerAd?.let {
                _adViewLiveData.value = it.adView
            } ?: run {
                _loadFailedLiveData.value = Unit
            }
        }
    }

    fun destroyBanner(bannerAdKey: BannerAdKey) = viewModelScope.launch {
        if (useCaseBanner.destroyBanner(bannerAdKey)) {
            _clearViewLiveData.postValue(Unit)
        }
    }
}