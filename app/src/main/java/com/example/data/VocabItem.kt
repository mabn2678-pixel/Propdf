package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vocab_items")
data class VocabItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val display: String = word,
    val translationAr: String,
    val ipa: String? = null,
    val level: String? = null, // "A1", "A2", "B1", "B2", "C1", "C2"
    val urlWeb: String? = null,
    val urlAudio: String? = null,
    val filePath: String? = null, // Which PDF it was found in context
    val dateAdded: Long = System.currentTimeMillis(),
    
    // SM-2 Spaced Repetition Fields
    val intervalDays: Int = 1,
    val repetitionCount: Int = 0,
    val easinessFactor: Float = 2.5f,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0
)

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocab_items ORDER BY dateAdded DESC")
    fun getAllVocabItems(): Flow<List<VocabItem>>

    @Query("SELECT * FROM vocab_items WHERE nextReviewDate <= :now ORDER BY nextReviewDate ASC")
    fun getItemsDueForReview(now: Long): Flow<List<VocabItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocab(item: VocabItem): Long

    @Update
    suspend fun updateVocab(item: VocabItem)

    @Delete
    suspend fun deleteVocab(item: VocabItem)

    @Query("SELECT * FROM vocab_items WHERE word = :word LIMIT 1")
    suspend fun getVocabByWord(word: String): VocabItem?
}
