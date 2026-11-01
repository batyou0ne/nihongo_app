package com.batuhankeskn0.kaimon.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

/**
 * Room veritabanında saklanan karmaşık türlerin (`LearnableItemKind`, `JLPTLevel`,
 * `List<String>`, `Map<String, Int>`, `Date`) SQLite ilkel tiplerine dönüşümünü sağlar.
 */
class RoomConverters {

    @TypeConverter
    fun fromLearnableItemKind(kind: LearnableItemKind): String = kind.rawValue

    @TypeConverter
    fun toLearnableItemKind(rawValue: String): LearnableItemKind =
        LearnableItemKind.fromRawValue(rawValue) ?: LearnableItemKind.HIRAGANA_CHARACTER

    @TypeConverter
    fun fromJLPTLevel(level: JLPTLevel): String = level.rawValue

    @TypeConverter
    fun toJLPTLevel(rawValue: String): JLPTLevel =
        JLPTLevel.fromRawValue(rawValue) ?: JLPTLevel.N5

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list.isNullOrEmpty()) return "[]"
        return runCatching { json.encodeToString(list) }.getOrDefault("[]")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromStringIntMap(map: Map<String, Int>?): String {
        if (map.isNullOrEmpty()) return "{}"
        return runCatching { json.encodeToString(map) }.getOrDefault("{}")
    }

    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int> {
        if (value.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Int>>(value) }.getOrDefault(emptyMap())
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(millis: Long?): Date? = millis?.let(::Date)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
