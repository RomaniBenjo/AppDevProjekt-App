package com.example.comingsoon.db

import androidx.room.TypeConverter
import com.example.comingsoon.viewmodels.JourneyLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromLocationList(value: List<JourneyLocation>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toLocationList(value: String?): List<JourneyLocation>? {
        if (value == null) return null
        val listType = object : TypeToken<List<JourneyLocation>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
