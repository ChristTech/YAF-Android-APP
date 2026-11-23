package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a Bible verse stored in the local SQLite database.
 * Supports quick search, book/chapter/verse browsing, and translation filtering
 * for game creation (Scripture Scramble, Trivia, and Who Am I context).
 */
@Entity(
    tableName = "bible_verses",
    indices = [
        Index(value = ["reference"]),
        Index(value = ["book", "chapter", "verse"]),
        Index(value = ["translation"]),
        Index(value = ["language"]),
        Index(value = ["theme"])
    ]
)
data class BibleVerseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val book: String,
    val bookNumber: Int = 1, // 1 for Genesis to 66 for Revelation
    val testament: String = "NEW", // "OLD" or "NEW"
    val chapter: Int,
    val verse: Int,
    val verseText: String,
    val reference: String, // e.g. "Daniel 11:32"
    val translation: String = "KJV", // e.g. "KJV", "NKJV", "Bibeli Mimo (YO)", "Littafi Mai Tsarki (HA)", "Baịbụl Nsọ (IG)"
    val language: com.example.core.localization.AppLanguage = com.example.core.localization.AppLanguage.ENGLISH,
    val theme: String = "Exploits & Strength" // Fellowship thematic classification
)
