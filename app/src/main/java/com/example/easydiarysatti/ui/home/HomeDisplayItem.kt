package com.example.easydiarysatti.ui.home

import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.google.android.gms.ads.nativead.NativeAd

sealed class HomeDisplayItem {
    data class NoteItem(val note: CreateNoteEntity) : HomeDisplayItem()
    data class AdItem(val nativeAd: NativeAd) : HomeDisplayItem()
}