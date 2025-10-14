package com.example.easydiarysatti

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp

class MyApp : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    private var isInBackground = false

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        isInBackground = true
        sessionManagerRepo.setRequireLogin(true)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isInBackground) {
            isInBackground = false
        }
    }
}

