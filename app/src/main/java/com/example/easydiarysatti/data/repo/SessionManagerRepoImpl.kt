package com.example.easydiarysatti.data.repo

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.easydiarysatti.BG_THEME_ID
import com.example.easydiarysatti.LANGUAGE_SETTLED_IN
import com.example.easydiarysatti.PIN
import com.example.easydiarysatti.PROFILE_EMAIL
import com.example.easydiarysatti.PROFILE_NAME
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

    override fun setProfileName(profilePic: String) {
        preferences.edit {
            this.putString(PROFILE_NAME, profilePic)
        }
    }

    override fun getprofileName(): String? {
        return preferences.getString(
            PROFILE_NAME,
            ""
        )
    }

    override fun setProfileEmail(email: String) {
        preferences.edit {
            this.putString(PROFILE_EMAIL, email)
        }
    }

    override fun getprofileEmail(): String? {
        return preferences.getString(
            PROFILE_EMAIL,
            ""
        )
    }

    override fun setPin(pin: String) {
        preferences.edit {
            this.putString(PIN, pin)
        }
    }

    override fun getPin(): String? {
        return preferences.getString(
            PIN,
            ""
        )
    }

}