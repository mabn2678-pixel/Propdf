package com.example.ui

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.VocabItem
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream

object VocabImporter {
    
    // Default interactive German vocabulary list with IPA pronunciations, Arabic meanings, levels, and DWDS URLs
    val defaultVocabList = listOf(
        VocabItem(
            word = "aufgeben",
            display = "aufgeben",
            translationAr = "يستسلم / يتخلى عن",
            ipa = "ˈaʊ̯fˌɡeːbn̩",
            level = "B1",
            urlWeb = "https://www.dwds.de/wb/aufgeben"
        ),
        VocabItem(
            word = "begeistert",
            display = "begeistert",
            translationAr = "متحمس للغاية / معجب شديد",
            ipa = "bəˈɡaɪ̯stɐt",
            level = "B2",
            urlWeb = "https://www.dwds.de/wb/begeistert"
        ),
        VocabItem(
            word = "Erfolg",
            display = "Erfolg",
            translationAr = "النجاح / فوز",
            ipa = "ɛɐ̯ˈfɔlk",
            level = "A2",
            urlWeb = "https://www.dwds.de/wb/Erfolg"
        ),
        VocabItem(
            word = "Geduld",
            display = "Geduld",
            translationAr = "الصبر / التحمّل",
            ipa = "ɡəˈdʊlt",
            level = "B1",
            urlWeb = "https://www.dwds.de/wb/Geduld"
        ),
        VocabItem(
            word = "wunderbar",
            display = "wunderbar",
            translationAr = "رائع / بديع",
            ipa = "ˈvʊndɐbaːɐ̯",
            level = "A1",
            urlWeb = "https://www.dwds.de/wb/wunderbar"
        ),
        VocabItem(
            word = "lernen",
            display = "lernen",
            translationAr = "يتعلم / يدرس",
            ipa = "ˈlɛʁnən",
            level = "A1",
            urlWeb = "https://www.dwds.de/wb/lernen"
        ),
        VocabItem(
            word = "entwickeln",
            display = "entwickeln",
            translationAr = "يطوّر / ينمّي",
            ipa = "ɛntˈvɪkl̩n",
            level = "B2",
            urlWeb = "https://www.dwds.de/wb/entwickeln"
        ),
        VocabItem(
            word = "vorteilhaft",
            display = "vorteilhaft",
            translationAr = "مفيد / ذو فائدة / ملائم",
            ipa = "ˈfoːɐ̯taɪ̯lhaft",
            level = "C1",
            urlWeb = "https://www.dwds.de/wb/vorteilhaft"
        ),
        VocabItem(
            word = "verstehen",
            display = "verstehen",
            translationAr = "يفهم / يدرك",
            ipa = "fɛɐ̯ˈʃteːən",
            level = "A1",
            urlWeb = "https://www.dwds.de/wb/verstehen"
        ),
        VocabItem(
            word = "entscheiden",
            display = "entscheiden",
            translationAr = "يقرر / يحسم أمر",
            ipa = "ɛntˈʃaɪ̯dn̩",
            level = "B1",
            urlWeb = "https://www.dwds.de/wb/entscheiden"
        )
    )

    fun importFromJsonFile(filePath: String): List<VocabItem> {
        val file = File(filePath)
        if (!file.exists()) {
            Log.d("VocabImporter", "File $filePath does not exist")
            return emptyList()
        }

        return try {
            val size = file.length().toInt()
            val bytes = ByteArray(size)
            FileInputStream(file).use { stream ->
                stream.read(bytes)
            }
            val jsonString = String(bytes, Charsets.UTF_8)
            parseJsonString(jsonString)
        } catch (e: Exception) {
            Log.e("VocabImporter", "Error reading vocal JSON file", e)
            emptyList()
        }
    }

    fun parseJsonString(jsonString: String): List<VocabItem> {
        val list = mutableListOf<VocabItem>()
        try {
            val rootArray = JSONArray(jsonString)
            for (i in 0 until rootArray.length()) {
                val obj = rootArray.getJSONObject(i)
                val word = obj.optString("word", "").trim()
                if (word.isNotEmpty()) {
                    list.add(
                        VocabItem(
                            word = word,
                            display = obj.optString("display", word),
                            translationAr = obj.optString("translation_ar", ""),
                            ipa = obj.optString("ipa", null),
                            level = obj.optString("level", null),
                            urlWeb = obj.optString("url_web", null),
                            urlAudio = obj.optString("url_audio", null)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("VocabImporter", "JSON parse error", e)
        }
        return list
    }

    fun getDownloadFolderPath(): String {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "vocab_list.json").absolutePath
    }
}
