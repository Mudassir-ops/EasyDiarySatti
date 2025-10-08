package com.example.easydiarysatti.domain.repo

interface SessionManagerRepo {

    fun isLanguageSettled(): Boolean?
    fun setLanguageSettled(languageSettledIn: Boolean)

    fun setBgTheme(themeResId: Int)
    fun getBgTheme(): Int?


    fun setProfilePic(profilePic: String)
    fun getprofilePic(): String?

}