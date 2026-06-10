package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(
    private val pdfDao: PdfDao,
    private val vocabDao: VocabDao
) {
    val recentFiles: Flow<List<PdfFile>> = pdfDao.getRecentFiles()
    val bookmarkedFiles: Flow<List<PdfFile>> = pdfDao.getBookmarkedFiles()
    val folders: Flow<List<String>> = pdfDao.getFolders()
    
    // Vocabulary methods
    val allVocabItems: Flow<List<VocabItem>> = vocabDao.getAllVocabItems()
    
    fun getItemsDueForReview(now: Long): Flow<List<VocabItem>> = vocabDao.getItemsDueForReview(now)
    
    suspend fun insertVocab(item: VocabItem) {
        vocabDao.insertVocab(item)
    }
    
    suspend fun updateVocab(item: VocabItem) {
        vocabDao.updateVocab(item)
    }
    
    suspend fun deleteVocab(item: VocabItem) {
        vocabDao.deleteVocab(item)
    }
    
    suspend fun getVocabByWord(word: String): VocabItem? = vocabDao.getVocabByWord(word)

    fun getFilesByFolder(folderName: String): Flow<List<PdfFile>> = pdfDao.getFilesByFolder(folderName)

    fun searchFiles(query: String): Flow<List<PdfFile>> {
        val formattedQuery = "%$query%"
        return pdfDao.searchFiles(formattedQuery)
    }

    suspend fun getFileById(id: Int): PdfFile? = pdfDao.getFileById(id)

    suspend fun insertFile(file: PdfFile) {
        pdfDao.insertFile(file)
    }

    suspend fun updateFile(file: PdfFile) {
        pdfDao.updateFile(file)
    }

    suspend fun deleteFile(file: PdfFile) {
        pdfDao.deleteFile(file)
    }

    // Populate default high-quality WPS PDF docs to make the app interactive and visually complete instantly
    suspend fun populateMockPdfDocs() {
        val docs = listOf(
            PdfFile(
                filePath = "sample_wps_guide.pdf",
                title = "WPS Office PDF Quick Guide.pdf",
                author = "WPS AI Team",
                sizeBytes = 2450000,
                pageCount = 12,
                isBookmarked = true,
                folderName = "WPS Guides",
                currentReadingPage = 1
            ),
            PdfFile(
                filePath = "kotlin_compose_best_practices.pdf",
                title = "Kotlin & Jetpack Compose Best Practices.pdf",
                author = "Google Developers",
                sizeBytes = 4120000,
                pageCount = 34,
                isBookmarked = false,
                folderName = "Android Development",
                currentReadingPage = 4
            ),
            PdfFile(
                filePath = "arabic_gulf_tourism_2026.pdf",
                title = "ترقية تجربة قراءة المستندات الاحترافية ٢٠٢٦.pdf",
                author = "فريق تطوير WPS",
                sizeBytes = 6200000,
                pageCount = 18,
                isBookmarked = false,
                folderName = "تقارير WPS",
                currentReadingPage = 1
            ),
            PdfFile(
                filePath = "pdf_view_sample_de.pdf",
                title = "Einführung in die professionelle PDF-Anzeige.pdf",
                author = "WPS Team Deutschland",
                sizeBytes = 1800000,
                pageCount = 8,
                isBookmarked = true,
                folderName = "Lesezeichen",
                currentReadingPage = 2
            )
        )
        for (doc in docs) {
            val existing = pdfDao.getFileByPath(doc.filePath)
            if (existing == null) {
                pdfDao.insertFile(doc)
            }
        }
    }
}
