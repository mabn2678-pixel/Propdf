package com.example.ui

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DatabaseProvider
import com.example.data.PdfFile
import com.example.data.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class PdfViewModel(context: Context) : ViewModel() {
    private val repository: PdfRepository = DatabaseProvider.getRepository(context)
    
    val ttsManager = TtsManager(context)

    // Study & Word Interaction system configurations
    private val _studyModeEnabled = MutableStateFlow(true)
    val studyModeEnabled: StateFlow<Boolean> = _studyModeEnabled.asStateFlow()

    private val _highlightType = MutableStateFlow("luminous") // "luminous", "hidden", "test"
    val highlightType: StateFlow<String> = _highlightType.asStateFlow()

    private val _pronunciationSpeed = MutableStateFlow(1.0f) // 0.5f, 1.0f, 1.5f
    val pronunciationSpeed: StateFlow<Float> = _pronunciationSpeed.asStateFlow()

    private val _preferredWebDictionary = MutableStateFlow("DWDS") // "DWDS", "Arabdict", "Linguee", "Google Translate", "Forvo"
    val preferredWebDictionary: StateFlow<String> = _preferredWebDictionary.asStateFlow()

    private val _showSpeakerIconBesideWord = MutableStateFlow(true)
    val showSpeakerIconBesideWord: StateFlow<Boolean> = _showSpeakerIconBesideWord.asStateFlow()

    // Room Live Personal Vocab items
    val allVocabItems: StateFlow<List<com.example.data.VocabItem>> = repository.allVocabItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state for bottom navigation tabs (0 = Recent, 1 = Bookmarks, 2 = Search, 3 = Settings, 4 = Vocabulary)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Recent PDFs from DB
    val recentFiles: StateFlow<List<PdfFile>> = repository.recentFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarks from DB
    val bookmarkedFiles: StateFlow<List<PdfFile>> = repository.bookmarkedFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Folders list
    val folders: StateFlow<List<String>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active reading file
    private val _selectedPdf = MutableStateFlow<PdfFile?>(null)
    val selectedPdf: StateFlow<PdfFile?> = _selectedPdf.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<PdfFile>> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.recentFiles
            } else {
                repository.searchFiles(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customize Reading variables
    private val _continuousScroll = MutableStateFlow(true)
    val continuousScroll: StateFlow<Boolean> = _continuousScroll.asStateFlow()

    private val _screenBrightness = MutableStateFlow(85f) // Simulated brightness percentage (1 - 100)
    val screenBrightness: StateFlow<Float> = _screenBrightness.asStateFlow()

    // Night Mode Theme Setting ("system", "dark", "light"). Mode starts at "dark" default as requested!
    private val _themeSetting = MutableStateFlow("dark")
    val themeSetting: StateFlow<String> = _themeSetting.asStateFlow()

    // File Permissions Onboarding gate
    private val _hasPermissionGranted = MutableStateFlow(false)
    val hasPermissionGranted: StateFlow<Boolean> = _hasPermissionGranted.asStateFlow()

    private val _showPermissionExplanatoryDialog = MutableStateFlow(false)
    val showPermissionExplanatoryDialog: StateFlow<Boolean> = _showPermissionExplanatoryDialog.asStateFlow()

    // Default language is "ar" (Arabic) so user lands on Arabic first. Supports ar, en, de
    private val _appLanguage = MutableStateFlow("ar")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
    }

    init {
        // Quick check of files. Auto-populate mock PDF guides to provide instant interactivity
        viewModelScope.launch {
            val current = repository.recentFiles.first()
            if (current.isEmpty()) {
                repository.populateMockPdfDocs()
            }
            
            // Populate default high-quality German-Arabic vocab items if empty
            val currentVocab = repository.allVocabItems.first()
            if (currentVocab.isEmpty()) {
                for (item in VocabImporter.defaultVocabList) {
                    repository.insertVocab(item)
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
        // Reset reading file if switching tab
        _selectedPdf.value = null
    }

    fun openPdf(pdfFile: PdfFile) {
        viewModelScope.launch {
            // Update last opened timestamp
            val updated = pdfFile.copy(lastOpened = System.currentTimeMillis())
            repository.updateFile(updated)
            _selectedPdf.value = updated
        }
    }

    fun closePdf() {
        _selectedPdf.value = null
    }

    fun toggleBookmark(pdfFile: PdfFile) {
        viewModelScope.launch {
            val updated = pdfFile.copy(isBookmarked = !pdfFile.isBookmarked)
            repository.updateFile(updated)
            // If the currently reading file is bookmarked/unbookmarked, update selectedPdf too
            if (_selectedPdf.value?.id == pdfFile.id) {
                _selectedPdf.value = updated
            }
        }
    }

    fun updateReadingPage(pdfFile: PdfFile, page: Int) {
        viewModelScope.launch {
            val updated = pdfFile.copy(currentReadingPage = page)
            repository.updateFile(updated)
            if (_selectedPdf.value?.id == pdfFile.id) {
                _selectedPdf.value = updated
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setContinuousScroll(enabled: Boolean) {
        _continuousScroll.value = enabled
    }

    fun setScreenBrightness(brightness: Float) {
        _screenBrightness.value = brightness
    }

    fun setThemeSetting(setting: String) {
        _themeSetting.value = setting
    }

    fun deleteFile(pdfFile: PdfFile) {
        viewModelScope.launch {
            repository.deleteFile(pdfFile)
            if (_selectedPdf.value?.id == pdfFile.id) {
                _selectedPdf.value = null
            }
        }
    }

    fun setPermissionState(granted: Boolean) {
        _hasPermissionGranted.value = granted
    }

    fun setShowPermissionExplanatory(show: Boolean) {
        _showPermissionExplanatoryDialog.value = show
    }

    fun scanForPdfFiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdfs = mutableListOf<PdfFile>()
            
            // Method 1: Query ContentResolver for MIME_TYPE application/pdf
            try {
                val contentResolver = context.contentResolver
                val uri = android.provider.MediaStore.Files.getContentUri("external")
                val projection = arrayOf(
                    android.provider.MediaStore.Files.FileColumns.DATA,
                    android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
                    android.provider.MediaStore.Files.FileColumns.SIZE
                )
                val selection = "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} = ?"
                val selectionArgs = arrayOf("application/pdf")
                
                contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    val dataIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATA)
                    val nameIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.SIZE)
                    
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataIndex)
                        val name = cursor.getString(nameIndex)
                        val size = cursor.getLong(sizeIndex)
                        
                        val f = File(path)
                        val folderName = f.parentFile?.name ?: "Documents"
                        
                        pdfs.add(
                            PdfFile(
                                filePath = path,
                                title = name,
                                author = "Local Document",
                                sizeBytes = size,
                                pageCount = (3..45).random(),
                                folderName = folderName,
                                isBookmarked = false,
                                currentReadingPage = 1
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Method 2: Recursive fallback over standard primary directories
            try {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val dToScan = listOfNotNull(downloadDir, documentsDir)
                for (dir in dToScan) {
                    if (dir.exists() && dir.isDirectory) {
                        scanDirectoryRecursive(dir, pdfs)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Compare and write newly-found files to DB
            val currentList = repository.recentFiles.first()
            for (p in pdfs) {
                val exists = currentList.any { it.filePath == p.filePath }
                if (!exists) {
                    repository.insertFile(p)
                }
            }
        }
    }

    private fun scanDirectoryRecursive(dir: File, list: MutableList<PdfFile>) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                scanDirectoryRecursive(f, list)
            } else if (f.name.endsWith(".pdf", ignoreCase = true)) {
                val alreadyAdded = list.any { it.filePath == f.absolutePath }
                if (!alreadyAdded) {
                    list.add(
                        PdfFile(
                            filePath = f.absolutePath,
                            title = f.name,
                            author = "Storage Folder",
                            sizeBytes = f.length(),
                            pageCount = (4..60).random(),
                            folderName = f.parentFile?.name ?: "Downloads",
                            isBookmarked = false,
                            currentReadingPage = 1
                        )
                    )
                }
            }
        }
    }

    // Creating folders programmatically in the WPS style
    fun addNewSamplePdf(title: String, folderName: String?) {
        viewModelScope.launch {
            val count = (5..25).random()
            val size = (1000..8000).random() * 1024L
            val mockFile = PdfFile(
                filePath = "user_created_${System.currentTimeMillis()}.pdf",
                title = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf",
                author = "WPS Member",
                sizeBytes = size,
                pageCount = count,
                isBookmarked = false,
                folderName = if (folderName.isNullOrBlank()) null else folderName.trim(),
                currentReadingPage = 1
            )
            repository.insertFile(mockFile)
        }
    }

    // Setters for vocabulary and study settings
    fun setStudyModeEnabled(enabled: Boolean) {
        _studyModeEnabled.value = enabled
    }

    fun setHighlightType(type: String) {
        _highlightType.value = type
    }

    fun setPronunciationSpeed(speed: Float) {
        _pronunciationSpeed.value = speed
    }

    fun setPreferredWebDictionary(dict: String) {
        _preferredWebDictionary.value = dict
    }

    fun setShowSpeakerIcon(show: Boolean) {
        _showSpeakerIconBesideWord.value = show
    }

    // Spaced Repetition (SM-2 Algorithm) Implementation
    // score ranges from 0 to 5:
    // 0: "Total blackout", 1: "Incorrect, but recognize", 2: "Incorrect, with easy recognition",
    // 3: "Correct, with severe effort", 4: "Correct, with slight hesitation", 5: "Perfect active recall"
    fun reviewVocabItem(item: com.example.data.VocabItem, score: Int) {
        viewModelScope.launch {
            val repetitions = if (score < 3) 0 else item.repetitionCount + 1
            val interval = when (repetitions) {
                0 -> 1
                1 -> 1
                2 -> 6
                else -> Math.round(item.intervalDays * item.easinessFactor).toInt()
            }
            // Easiness factor modifier formula
            val newFactor = item.easinessFactor + (0.1f - (5f - score) * (0.08f + (5f - score) * 0.02f))
            val factor = if (newFactor < 1.3f) 1.3f else newFactor

            val updated = item.copy(
                repetitionCount = repetitions,
                intervalDays = interval,
                easinessFactor = factor,
                nextReviewDate = System.currentTimeMillis() + (interval * 24L * 60L * 60L * 1000L),
                reviewCount = item.reviewCount + 1
            )
            repository.updateVocab(updated)
        }
    }

    fun addVocabItemManual(word: String, translation: String, ipa: String? = null, level: String? = null) {
        viewModelScope.launch {
            val formattedWord = word.trim()
            if (formattedWord.isNotEmpty()) {
                val cleanedTranslation = translation.trim()
                val item = com.example.data.VocabItem(
                    word = formattedWord,
                    display = formattedWord,
                    translationAr = if (cleanedTranslation.isEmpty()) "ترجمة افتراضية" else cleanedTranslation,
                    ipa = ipa?.trim()?.takeIf { it.isNotEmpty() },
                    level = level?.trim()?.takeIf { it.isNotEmpty() } ?: "A1",
                    urlWeb = "https://www.dwds.de/wb/${formattedWord}"
                )
                repository.insertVocab(item)
            }
        }
    }

    fun deleteVocabItem(item: com.example.data.VocabItem) {
        viewModelScope.launch {
            repository.deleteVocab(item)
        }
    }

    // Trigger import from custom path
    fun triggerVocabImport(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val imported = VocabImporter.importFromJsonFile(filePath)
            if (imported.isNotEmpty()) {
                for (item in imported) {
                    val existing = repository.getVocabByWord(item.word)
                    if (existing == null) {
                        repository.insertVocab(item)
                    } else {
                        // Merge or update translation
                        val merged = existing.copy(
                            translationAr = item.translationAr,
                            ipa = item.ipa ?: existing.ipa,
                            level = item.level ?: existing.level,
                            urlWeb = item.urlWeb ?: existing.urlWeb,
                            urlAudio = item.urlAudio ?: existing.urlAudio
                        )
                        repository.updateVocab(merged)
                    }
                }
            }
        }
    }

    // Import vocabulary items using the user's custom Python semicolon-separated format
    fun triggerSemicolonVocabImport(rawText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = rawText.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.contains("----")) {
                    continue
                }
                
                var cleanLine = trimmed
                if (cleanLine.startsWith(":")) {
                    cleanLine = cleanLine.substring(1).trim()
                }
                if (cleanLine.endsWith("-") && cleanLine.length > 1) {
                    cleanLine = cleanLine.substring(0, cleanLine.length - 1).trim()
                }
                if (cleanLine.endsWith(";") && cleanLine.length > 1) {
                    cleanLine = cleanLine.substring(0, cleanLine.length - 1).trim()
                }

                val parts = cleanLine.split(";")
                if (parts.size >= 2) {
                    val typeOrGender = parts[0].trim().lowercase()
                    val word = parts[1].trim()
                    if (word.isEmpty()) continue

                    val pluralVal = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() && it != "-" }
                    val translation = parts.getOrNull(3)?.trim() ?: ""
                    val notes = parts.getOrNull(4)?.trim()?.takeIf { it.isNotEmpty() && it != "-" }

                    if (translation.isEmpty()) continue

                    val displayWord = if (!pluralVal.isNullOrEmpty()) "$word ($pluralVal)" else word
                    val levelVal = when (typeOrGender) {
                        "der", "die", "das" -> typeOrGender.uppercase()
                        "verb" -> "VERB"
                        "adj" -> "ADJ"
                        else -> "MISC"
                    }

                    val urlEncoded = try {
                        java.net.URLEncoder.encode(word, "UTF-8")
                    } catch (e: Exception) {
                        word
                    }

                    val item = com.example.data.VocabItem(
                        word = word,
                        display = displayWord,
                        translationAr = translation,
                        ipa = pluralVal ?: notes,
                        level = levelVal,
                        urlWeb = "https://www.dwds.de/wb/${urlEncoded}"
                    )

                    val existing = repository.getVocabByWord(item.word)
                    if (existing == null) {
                        repository.insertVocab(item)
                    } else {
                        val updated = existing.copy(
                            translationAr = item.translationAr,
                            display = item.display,
                            ipa = item.ipa ?: existing.ipa,
                            level = item.level ?: existing.level
                        )
                        repository.updateVocab(updated)
                    }
                }
            }
        }
    }

    // Export current personal database to JSON/CSV string
    fun exportVocabString(format: String): String {
        val list = allVocabItems.value
        return if (format.lowercase() == "json") {
            val sb = java.lang.StringBuilder()
            sb.append("[\n")
            list.forEachIndexed { index, item ->
                sb.append("  {\n")
                sb.append("    \"word\": \"${item.word}\",\n")
                sb.append("    \"display\": \"${item.display}\",\n")
                sb.append("    \"translation_ar\": \"${item.translationAr}\",\n")
                sb.append("    \"ipa\": \"${item.ipa ?: ""}\",\n")
                sb.append("    \"level\": \"${item.level ?: ""}\",\n")
                sb.append("    \"url_web\": \"${item.urlWeb ?: ""}\",\n")
                sb.append("    \"url_audio\": \"${item.urlAudio ?: ""}\"\n")
                sb.append("  }")
                if (index < list.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("]")
            sb.toString()
        } else {
            // CSV
            val sb = java.lang.StringBuilder()
            sb.append("Word,Display,Translation (AR),IPA,Level,Web Url,Audio Url\n")
            list.forEach { item ->
                sb.append("\"${item.word}\",\"${item.display}\",\"${item.translationAr}\",\"${item.ipa ?: ""}\",\"${item.level ?: ""}\",\"${item.urlWeb ?: ""}\",\"${item.urlAudio ?: ""}\"\n")
            }
            sb.toString()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
