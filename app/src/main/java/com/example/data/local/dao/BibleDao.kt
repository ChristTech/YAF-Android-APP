package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BibleVerseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {

    @Query("SELECT * FROM bible_verses ORDER BY bookNumber ASC, chapter ASC, verse ASC")
    fun getAllVersesFlow(): Flow<List<BibleVerseEntity>>

    @Query("""
        SELECT * FROM bible_verses 
        WHERE (verseText LIKE '%' || :query || '%' 
           OR reference LIKE '%' || :query || '%' 
           OR book LIKE '%' || :query || '%'
           OR theme LIKE '%' || :query || '%')
          AND (:translation IS NULL OR translation = :translation)
        ORDER BY bookNumber ASC, chapter ASC, verse ASC
        LIMIT 100
    """)
    fun searchVersesFlow(query: String, translation: String? = null): Flow<List<BibleVerseEntity>>

    @Query("""
        SELECT * FROM bible_verses 
        WHERE (verseText LIKE '%' || :query || '%' 
           OR reference LIKE '%' || :query || '%' 
           OR book LIKE '%' || :query || '%'
           OR theme LIKE '%' || :query || '%')
          AND (:translation IS NULL OR translation = :translation)
        ORDER BY bookNumber ASC, chapter ASC, verse ASC
        LIMIT 100
    """)
    suspend fun searchVerses(query: String, translation: String? = null): List<BibleVerseEntity>

    @Query("SELECT * FROM bible_verses WHERE book = :book AND chapter = :chapter AND (:translation IS NULL OR translation = :translation) ORDER BY verse ASC")
    fun getVersesByBookAndChapterFlow(book: String, chapter: Int, translation: String? = null): Flow<List<BibleVerseEntity>>

    @Query("SELECT DISTINCT book FROM bible_verses ORDER BY bookNumber ASC")
    fun getDistinctBooksFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT book FROM bible_verses ORDER BY bookNumber ASC")
    suspend fun getDistinctBooks(): List<String>

    @Query("SELECT DISTINCT translation FROM bible_verses ORDER BY translation ASC")
    fun getDistinctTranslationsFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT theme FROM bible_verses ORDER BY theme ASC")
    fun getDistinctThemesFlow(): Flow<List<String>>

    @Query("SELECT * FROM bible_verses WHERE reference = :reference AND (:translation IS NULL OR translation = :translation) LIMIT 1")
    suspend fun getVerseByReference(reference: String, translation: String? = null): BibleVerseEntity?

    @Query("SELECT COUNT(*) FROM bible_verses")
    suspend fun getVerseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerse(verse: BibleVerseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<BibleVerseEntity>)

    @Query("DELETE FROM bible_verses WHERE id = :id")
    suspend fun deleteVerse(id: Long)

    @Query("DELETE FROM bible_verses")
    suspend fun clearAllVerses()
}
