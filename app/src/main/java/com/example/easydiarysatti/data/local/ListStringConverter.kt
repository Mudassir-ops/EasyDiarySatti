package com.example.easydiarysatti.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListStringConverter {

    private val gson = Gson()

    // ── List<String> ──────────────────────────────────────────────────────────
    // Null input → null stored (NULL column). Empty list → "[]" stored.
    @TypeConverter
    fun fromList(value: List<String>?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return null   // NULL column → null field
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(value, type)
        }.getOrNull()
    }

    // ── List<CustomTagEntity> ─────────────────────────────────────────────────
    // MUST match toList() null contract exactly — both return nullable String?
    // so Room computes the same identity hash every build.
    // fromCustomTagList previously returned non-null String (wrote "null" to DB
    // when value was null) while toList returned null String? — the signature
    // mismatch caused checkIdentity to fail across builds.
    @TypeConverter
    fun fromCustomTagList(value: List<CustomTagEntity>?): String? =
        value?.let { gson.toJson(it) }   // null input → null column (not the string "null")

    @TypeConverter
    fun toCustomTagList(value: String?): List<CustomTagEntity>? {
        if (value.isNullOrEmpty()) return null
        return runCatching {
            val listType = object : TypeToken<List<CustomTagEntity>>() {}.type
            gson.fromJson<List<CustomTagEntity>>(value, listType)
        }.getOrNull()
    }
}