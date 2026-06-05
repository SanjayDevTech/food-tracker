package com.example.foodtracker.data.db

import androidx.room.TypeConverter
import com.example.foodtracker.data.ServingVariant
import kotlinx.serialization.json.Json

object Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    @JvmStatic
    fun servingVariantsToJson(variants: List<ServingVariant>): String =
        json.encodeToString(variants)

    @TypeConverter
    @JvmStatic
    fun jsonToServingVariants(value: String): List<ServingVariant> =
        if (value.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<ServingVariant>>(value) }.getOrDefault(emptyList())
}
