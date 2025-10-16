package com.example.easydiarysatti.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListStringConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }


    @TypeConverter
    fun fromCustomTagList(value: List<CustomTagEntity>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCustomTagList(value: String): List<CustomTagEntity>? {
        val listType = object : TypeToken<List<CustomTagEntity>>() {}.type
        return gson.fromJson(value, listType)
    }

}
