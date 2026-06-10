package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VocabItem
import kotlinx.coroutines.launch
import java.net.URLEncoder

val TechBlue = Color(0xFF3B82F6)
val SoftHighlightGreen = Color(0x2222C55E)
val SoftHighlightBlue = Color(0x153B82F6)
val SoftHighlightOrange = Color(0x15F59E0B)
val LightBorderGray = Color(0xFFE5E7EB)

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GermanArabicInteractiveVocabParagraph(
    text: String,
    scale: Float,
    textBaseColor: Color,
    viewModel: PdfViewModel,
    onWordSelected: (VocabItem) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val vocabList by viewModel.allVocabItems.collectAsState()
    val highlightType by viewModel.highlightType.collectAsState()
    val pronunciationSpeed by viewModel.pronunciationSpeed.collectAsState()
    val showSpeakerIcon by viewModel.showSpeakerIconBesideWord.collectAsState()

    val rawTokens = remember(text) { text.split(" ") }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        rawTokens.forEach { token ->
            val cleanedWord = remember(token) {
                token.replace(Regex("[.,!?;:()\"]"), "").trim()
            }
            
            val matchItem = remember(cleanedWord, vocabList) {
                vocabList.find { it.word.equals(cleanedWord, ignoreCase = true) }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (matchItem != null && highlightType == "luminous") {
                            Modifier.background(SoftHighlightGreen)
                        } else {
                            Modifier
                        }
                    )
                    .combinedClickable(
                        onClick = {
                            if (cleanedWord.isNotEmpty()) {
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } catch (e: Exception) {
                                }
                                
                                viewModel.ttsManager.speak(cleanedWord, speed = pronunciationSpeed)

                                val selectedItem = matchItem ?: VocabItem(
                                    word = cleanedWord,
                                    translationAr = getArabicTranslationFallback(cleanedWord),
                                    ipa = getIpaFallback(cleanedWord),
                                    level = getLevelFallback(cleanedWord),
                                    urlWeb = "https://www.dwds.de/wb/${URLEncoder.encode(cleanedWord, "UTF-8")}"
                                )
                                onWordSelected(selectedItem)
                            }
                        },
                        onLongClick = {
                            val speakRate = pronunciationSpeed
                            viewModel.ttsManager.speak(text, speed = speakRate)
                        }
                    )
                    .then(
                        if (matchItem != null && highlightType == "luminous") {
                            Modifier.border(0.5.dp, TechBlue.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                        }
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = token,
                        fontSize = (12 * scale).sp,
                        color = if (matchItem != null && highlightType == "luminous") TechBlue else textBaseColor,
                        fontWeight = if (matchItem != null && highlightType == "luminous") FontWeight.Bold else FontWeight.Normal,
                        style = if (matchItem != null && highlightType == "luminous") {
                            androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        } else {
                            androidx.compose.ui.text.TextStyle.Default
                        }
                    )
                    
                    if (matchItem != null && highlightType == "luminous" && showSpeakerIcon) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = TechBlue,
                            modifier = Modifier.size((10 * scale).dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WordInteractionBottomSheet(
    vocabItem: VocabItem,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val vocabList by viewModel.allVocabItems.collectAsState()
    val preferredDict by viewModel.preferredWebDictionary.collectAsState()
    
    val isAlreadySaved = remember(vocabItem, vocabList) {
        vocabList.any { it.word.equals(vocabItem.word, ignoreCase = true) }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("word_bottom_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, TechBlue.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.ttsManager.speak(vocabItem.word, speed = 1.0f) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(TechBlue.copy(alpha = 0.1f), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Standard Pronounce",
                            tint = TechBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Text(
                        text = vocabItem.word,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    vocabItem.ipa?.let {
                        Text(
                            text = "[$it]",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(TechBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = vocabItem.level ?: "A1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TechBlue
                        )
                    }
                    
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "الترجمة: ${vocabItem.translationAr}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val query = vocabItem.word
                        val dictionaryUrl = when (preferredDict) {
                            "DWDS" -> "https://www.dwds.de/wb/${URLEncoder.encode(query, "UTF-8")}"
                            "Arabdict" -> "https://www.arabdict.com/de-ar/${URLEncoder.encode(query, "UTF-8")}"
                            "Linguee" -> "https://www.linguee.de/deutsch-arabisch/search?source=auto&query=${URLEncoder.encode(query, "UTF-8")}"
                            "Google Translate" -> "https://translate.google.com/?sl=de&tl=ar&text=${URLEncoder.encode(query, "UTF-8")}"
                            "Forvo" -> "https://forvo.com/word/${URLEncoder.encode(query, "UTF-8")}/#de"
                            else -> "https://www.dwds.de/wb/${URLEncoder.encode(query, "UTF-8")}"
                        }
                        try {
                            uriHandler.openUri(dictionaryUrl)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.weight(1.0f),
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فتح $preferredDict", fontSize = 11.sp, maxLines = 1)
                }
                
                OutlinedButton(
                    onClick = { viewModel.ttsManager.speak(vocabItem.word, speed = 0.5f) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.QuestionMark, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نطق بطيء", fontSize = 11.sp)
                }
                
                OutlinedButton(
                    onClick = {
                        if (isAlreadySaved) {
                            val target = vocabList.find { it.word.equals(vocabItem.word, ignoreCase = true) }
                            if (target != null) viewModel.deleteVocabItem(target)
                        } else {
                            viewModel.addVocabItemManual(
                                word = vocabItem.word,
                                translation = vocabItem.translationAr,
                                ipa = vocabItem.ipa,
                                level = vocabItem.level
                            )
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isAlreadySaved) Color(0xFFEF4444) else TechBlue
                    ),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isAlreadySaved) Color(0xFFFCA5A5) else TechBlue.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isAlreadySaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAlreadySaved) "إزالة" else "إضافة لقائمتي", fontSize = 11.sp, maxLines = 1)
                }
            }
            
            if (isAlreadySaved) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = LightBorderGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "تقييم التذكر في المراجعة (خوارزمية SM-2):",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val saved = vocabList.find { it.word.equals(vocabItem.word, ignoreCase = true) }
                            if (saved != null) {
                                viewModel.reviewVocabItem(saved, 1)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f), contentColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("خطأ 🔴", fontSize = 11.sp)
                    }
                    
                    Button(
                        onClick = {
                            val saved = vocabList.find { it.word.equals(vocabItem.word, ignoreCase = true) }
                            if (saved != null) {
                                viewModel.reviewVocabItem(saved, 3)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f), contentColor = Color(0xFFD97706)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("بصعوبة 🟡", fontSize = 11.sp)
                    }
                    
                    Button(
                        onClick = {
                            val saved = vocabList.find { it.word.equals(vocabItem.word, ignoreCase = true) }
                            if (saved != null) {
                                viewModel.reviewVocabItem(saved, 5)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f), contentColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("سهل 🟢", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun VocabularyManagerScreen(
    viewModel: PdfViewModel,
    modifier: Modifier = Modifier
) {
    val vocabList by viewModel.allVocabItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevelFilter by remember { mutableStateOf<String?>(null) }
    var showFlashcardQuiz by remember { mutableStateOf(false) }
    
    var showAddManualDialog by remember { mutableStateOf(false) }
    var manualWord by remember { mutableStateOf("") }
    var manualTranslation by remember { mutableStateOf("") }
    var manualLevel by remember { mutableStateOf("B1") }
    var manualIpa by remember { mutableStateOf("") }
    
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var batchImportText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    
    val filteredList = remember(vocabList, searchQuery, selectedLevelFilter) {
        vocabList.filter {
            (searchQuery.isEmpty() || it.word.contains(searchQuery, ignoreCase = true) || it.translationAr.contains(searchQuery)) &&
            (selectedLevelFilter == null || it.level == selectedLevelFilter)
        }
    }
    
    val wordsDueCount = remember(vocabList) {
        vocabList.count { it.nextReviewDate <= System.currentTimeMillis() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قاموس المفردات الشخصي",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TechBlue
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddManualDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("إضافة كلمة", fontSize = 11.sp)
                }
                
                Button(
                    onClick = { showBatchImportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue.copy(alpha = 0.15f), contentColor = TechBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("استيراد حزمة", fontSize = 11.sp)
                }
                
                IconButton(
                    onClick = {
                        val jsonStr = viewModel.exportVocabString("json")
                        android.widget.Toast.makeText(context, "تم نسخ الملف بصيغة JSON", android.widget.Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.background(TechBlue.copy(alpha = 0.1f), RoundedCornerShape(50))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export JSON", tint = TechBlue, modifier = Modifier.size(18.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TechBlue.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مراجعة اليوم الذكية (SM-2)",
                        fontWeight = FontWeight.Bold,
                        color = TechBlue,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (wordsDueCount > 0) "لديك $wordsDueCount مفردات جاهزة للمراجعة المجدولة حالياً."
                        else "عمل رائع! لقد أكملت مراجعة جميع كلماتك لليوم.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                if (wordsDueCount > 0) {
                    Button(
                        onClick = { showFlashcardQuiz = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("بدأ المراجعة", fontSize = 11.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في الكلمات أو الترجمات...", fontSize = 12.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            var levelMenuExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { levelMenuExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(selectedLevelFilter ?: "كل المستويات", fontSize = 11.sp)
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                
                DropdownMenu(
                    expanded = levelMenuExpanded,
                    onDismissRequest = { levelMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("كل المستويات") },
                        onClick = {
                            selectedLevelFilter = null
                            levelMenuExpanded = false
                        }
                    )
                    listOf("A1", "A2", "B1", "B2", "C1").forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                selectedLevelFilter = level
                                levelMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Book, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Text(
                        text = "لم نجد أي كلمات تطابق الاختيارات",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = item.word,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TechBlue
                                    )
                                    Text(
                                        text = "[${item.ipa ?: ""}]",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(TechBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(item.level ?: "A1", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TechBlue)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.translationAr,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "تاريخ المراجعة القادم: ${getReadableTime(item.nextReviewDate)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.ttsManager.speak(item.word, speed = 1.0f) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Listen", tint = TechBlue, modifier = Modifier.size(18.dp))
                                }
                                
                                IconButton(
                                    onClick = { viewModel.deleteVocabItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddManualDialog) {
        AlertDialog(
            onDismissRequest = { showAddManualDialog = false },
            title = { Text("أضف كلمة جديدة يدوياً", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = manualWord,
                        onValueChange = { manualWord = it },
                        label = { Text("الكلمة بالألمانية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualTranslation,
                        onValueChange = { manualTranslation = it },
                        label = { Text("الترجمة بالعربية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualIpa,
                        onValueChange = { manualIpa = it },
                        label = { Text("اللفظ الصوتي IPA (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualLevel,
                        onValueChange = { manualLevel = it },
                        label = { Text("المستوى (A1, B1, etc.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualWord.isNotEmpty() && manualTranslation.isNotEmpty()) {
                            viewModel.addVocabItemManual(manualWord, manualTranslation, manualIpa, manualLevel)
                            showAddManualDialog = false
                            manualWord = ""
                            manualTranslation = ""
                            manualIpa = ""
                            manualLevel = "B1"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showBatchImportDialog) {
        AlertDialog(
            onDismissRequest = { showBatchImportDialog = false },
            title = { Text("استيراد حزمة مفردات من بايثون / ملف نصي", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "انسخ والصق الكلمات بالصيغة المخصصة (فصل بنقطتين وفاصلة منقوطة) مثل:\ndie;Babykleidung;Sg.;ملابس أطفال;-\nverb;bestehen;;ينجح;er besteht",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = batchImportText,
                        onValueChange = { batchImportText = it },
                        placeholder = { Text("أدخل الكلمات هنا...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 15
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (batchImportText.isNotBlank()) {
                            viewModel.triggerSemicolonVocabImport(batchImportText)
                            showBatchImportDialog = false
                            batchImportText = ""
                            android.widget.Toast.makeText(context, "جاري استيراد ومعالجة الكلمات بنجاح...", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue)
                ) {
                    Text("بدأ الاستيراد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchImportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
    
    if (showFlashcardQuiz) {
        val reviewList = remember(vocabList) {
            vocabList.filter { it.nextReviewDate <= System.currentTimeMillis() }
        }
        var currentQuizIdx by remember { mutableStateOf(0) }
        var showTranslationInCard by remember { mutableStateOf(false) }
        
        if (reviewList.isNotEmpty() && currentQuizIdx < reviewList.size) {
            val quizItem = reviewList[currentQuizIdx]
            
            AlertDialog(
                onDismissRequest = { showFlashcardQuiz = false },
                title = { Text("مراجعة بطاقات الـ Flashcards", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "مفردة ${currentQuizIdx + 1} من ${reviewList.size}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            text = quizItem.word,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TechBlue
                        )
                        quizItem.ipa?.let {
                            Text(text = "[$it]", fontSize = 12.sp, color = Color.Gray)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (showTranslationInCard) {
                            Text(
                                text = "الترجمة بالعربية: ${quizItem.translationAr}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Button(
                                onClick = { showTranslationInCard = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TechBlue.copy(alpha = 0.1f), contentColor = TechBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("اكشف عن الترجمة")
                            }
                        }
                        
                        if (showTranslationInCard) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "كيف كانت جودة تذكر الكلمة؟",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.reviewVocabItem(quizItem, 1)
                                        if (currentQuizIdx < reviewList.size - 1) {
                                            currentQuizIdx++
                                            showTranslationInCard = false
                                        } else {
                                            showFlashcardQuiz = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("صعب 🚫", fontSize = 10.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.reviewVocabItem(quizItem, 3)
                                        if (currentQuizIdx < reviewList.size - 1) {
                                            currentQuizIdx++
                                            showTranslationInCard = false
                                        } else {
                                            showFlashcardQuiz = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("بجهد 🟡", fontSize = 10.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.reviewVocabItem(quizItem, 5)
                                        if (currentQuizIdx < reviewList.size - 1) {
                                            currentQuizIdx++
                                            showTranslationInCard = false
                                        } else {
                                            showFlashcardQuiz = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("سهل 🟢", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showFlashcardQuiz = false }) {
                        Text("إغلاق الجلسة")
                    }
                }
            )
        } else {
            showFlashcardQuiz = false
        }
    }
}

private fun getArabicTranslationFallback(word: String): String {
    return when (word.lowercase()) {
        "willkommen" -> "مرحباً بكم"
        "professionellen" -> "الاحترافي / المهني"
        "deutsch" -> "ألماني / اللغة الألمانية"
        "sprache" -> "اللغة / لسان"
        "schöne" -> "جميل / رائع"
        "wunderbar" -> "بديع / رائع للغاية"
        "erfolg" -> "النجاح"
        "geduld" -> "الصبر / التأنّي"
        "lernen" -> "يعلم / يدرس"
        "verstehen" -> "يفهم"
        "entscheiden" -> "يقرر / يحسم"
        "begeistert" -> "متحمس للغاية"
        else -> "اضغط مرتين للترجمة الفورية عبر الإنترنت"
    }
}

private fun getIpaFallback(word: String): String {
    return when (word.lowercase()) {
        "willkommen" -> "vɪlˈkɔmən"
        "deutsch" -> "dɔʏtʃ"
        "sprache" -> "ˈʃpʁaːxə"
        "schöne" -> "ˈʃøːnə"
        "lernen" -> "ˈlɛʁnən"
        "erfolg" -> "ɛɐ̯ˈfɔlk"
        "geduld" -> "ɡəˈdʊlt"
        else -> "ˈdeː_ar"
    }
}

private fun getLevelFallback(word: String): String {
    return when (word.lowercase()) {
        "willkommen", "deutsch", "lernen" -> "A1"
        "erfolg", "sprache" -> "A2"
        "geduld", "entscheiden" -> "B1"
        "begeistert", "entwickeln" -> "B2"
        else -> "B1"
    }
}

private fun getReadableTime(millis: Long): String {
    val date = java.util.Date(millis)
    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
    return sdf.format(date)
}
