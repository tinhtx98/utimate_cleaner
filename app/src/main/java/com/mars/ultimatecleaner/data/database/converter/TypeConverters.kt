package com.mars.ultimatecleaner.data.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class TypeConverters {

    private val gson = Gson()

    // Date converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // List<String> converters
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value == null) {
            null
        } else {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType)
        }
    }

    // List<Long> converters
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return if (value == null) {
            null
        } else {
            val listType = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(value, listType)
        }
    }

    // Map<String, Any> converters
    @TypeConverter
    fun fromStringAnyMap(value: Map<String, Any>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringAnyMap(value: String?): Map<String, Any>? {
        return if (value == null) {
            null
        } else {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(value, mapType)
        }
    }

    // Map<String, String> converters
    @TypeConverter
    fun fromStringStringMap(value: Map<String, String>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringStringMap(value: String?): Map<String, String>? {
        return if (value == null) {
            null
        } else {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(value, mapType)
        }
    }

    // Map<String, Float> converters
    @TypeConverter
    fun fromStringFloatMap(value: Map<String, Float>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringFloatMap(value: String?): Map<String, Float>? {
        return if (value == null) {
            null
        } else {
            val mapType = object : TypeToken<Map<String, Float>>() {}.type
            gson.fromJson(value, mapType)
        }
    }
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            Gson().fromJson<Map<String, String>>(value, object : TypeToken<Map<String, String>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromStringLongMap(value: Map<String, Long>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringLongMap(value: String): Map<String, Long> {
        return try {
            Gson().fromJson<Map<String, Long>>(value, object : TypeToken<Map<String, Long>>() {}.type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}