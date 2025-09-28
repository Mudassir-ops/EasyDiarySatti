package com.example.easydiarysatti.domain.repo

interface SessionManagerRepo {

    fun isLanguageSettled(): Boolean?
    fun setLanguageSettled(languageSettledIn: Boolean)

}