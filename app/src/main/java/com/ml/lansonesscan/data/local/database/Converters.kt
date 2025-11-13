package com.ml.lansonesscan.data.local.database

import androidx.room.TypeConverter
import com.ml.lansonesscan.domain.model.AnalysisType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for complex data types
 */
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return if (value == null) {
            ""
        } else {
            gson.toJson(value)
        }
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                gson.fromJson(value, listType) ?: emptyList()
            } catch (e: Exception) {
                // Fallback for simple JSON parsing
                value.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
            }
        }
    }

    @TypeConverter
    fun fromAnalysisType(analysisType: AnalysisType): String {
        return analysisType.name
    }

    @TypeConverter
    fun toAnalysisType(analysisType: String): AnalysisType {
        return try {
            AnalysisType.valueOf(analysisType)
        } catch (e: IllegalArgumentException) {
            // Default fallback
            AnalysisType.FRUIT
        }
    }
}