package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pdf_files")
data class PdfFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val title: String,
    val author: String? = null,
    val sizeBytes: Long = 0,
    val pageCount: Int = 0,
    val lastOpened: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val folderName: String? = null, // Custom folder organization like WPS
    val currentReadingPage: Int = 1
)

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_files ORDER BY lastOpened DESC")
    fun getRecentFiles(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE isBookmarked = 1 ORDER BY lastOpened DESC")
    fun getBookmarkedFiles(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE folderName = :folderName ORDER BY title ASC")
    fun getFilesByFolder(folderName: String): Flow<List<PdfFile>>

    @Query("SELECT DISTINCT folderName FROM pdf_files WHERE folderName IS NOT NULL")
    fun getFolders(): Flow<List<String>>

    @Query("SELECT * FROM pdf_files WHERE title LIKE :query OR filePath LIKE :query")
    fun searchFiles(query: String): Flow<List<PdfFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: PdfFile): Long

    @Update
    suspend fun updateFile(file: PdfFile)

    @Delete
    suspend fun deleteFile(file: PdfFile)

    @Query("SELECT * FROM pdf_files WHERE filePath = :path LIMIT 1")
    suspend fun getFileByPath(path: String): PdfFile?
    
    @Query("SELECT * FROM pdf_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): PdfFile?
}

@Database(entities = [PdfFile::class, VocabItem::class], version = 2, exportSchema = false)
abstract class PdfDatabase : RoomDatabase() {
    abstract val pdfDao: PdfDao
    abstract val vocabDao: VocabDao
}
