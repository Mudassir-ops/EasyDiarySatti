package com.example.easydiarysatti.data.repo

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.easydiarysatti.BG_THEME_ID
import com.example.easydiarysatti.LANGUAGE_SETTLED_IN
import com.example.easydiarysatti.PROFILE_PIC
import com.example.easydiarysatti.R
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

    override fun setBgTheme(themeResId: Int) {
        preferences.edit {
            this.putInt(BG_THEME_ID, themeResId)
        }
    }

    override fun getBgTheme(): Int? {
        return preferences.getInt(
            BG_THEME_ID,
            R.drawable.theme_1
        )
    }

    override fun setProfilePic(profilePic: String) {
        preferences.edit {
            this.putString(PROFILE_PIC, profilePic)
        }
    }

    override fun getprofilePic(): String? {
        return preferences.getString(
            PROFILE_PIC,
            ""
        )
    }

}