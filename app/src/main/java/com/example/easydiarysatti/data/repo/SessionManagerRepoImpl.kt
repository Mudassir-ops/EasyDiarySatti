package com.example.easydiarysatti.data.repo

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.easydiarysatti.LANGUAGE_SETTLED_IN
import com.example.easydiarysatti.domain.repo.SessionManagerRepo

class SessionManagerRepoImpl(
    private val preferences: SharedPreferences
) : SessionManagerRepo {

    override fun isLanguageSettled(): Boolean? {
        return preferences.getBoolean(
            LANGUAGE_SETTLED_IN,
            false
        )
    }

    override fun setLanguageSettled(languageSettledIn: Boolean) {
        preferences.edit {
            this.putBoolean(LANGUAGE_SETTLED_IN, languageSettledIn)
        }
    }

}