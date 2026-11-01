package com.batuhankeskn0.kaimon

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Faz 1 — Single Source of Truth Ortak Asset Doğrulama Testleri.
 * iOS `Nihongo_app/Nihongo_app/Resources/` klasörünün sıfır kopya ile
 * Android modülü tarafından erişilebilir ve geçerli olduğunu doğrular.
 */
class SharedAssetsVerificationTest {

    private val sharedResourcesDir: File by lazy {
        // Test çalışma dizini `Nihongo_android/app` olduğundan `../../Nihongo_app/Nihongo_app/Resources`
        val candidate = File("../../Nihongo_app/Nihongo_app/Resources").canonicalFile
        if (candidate.exists()) {
            candidate
        } else {
            File("../Nihongo_app/Nihongo_app/Resources").canonicalFile
        }
    }

    @Test
    fun `shared resources directory exists and contains all 28 JLPT and kana JSON files`() {
        assertTrue(
            "Ortak Resources dizini bulunamadı: ${sharedResourcesDir.absolutePath}",
            sharedResourcesDir.exists() && sharedResourcesDir.isDirectory
        )

        val expectedJsonFiles = buildList {
            add("HiraganaData.json")
            add("KatakanaData.json")
            add("ScenariosData.json")
            for (level in listOf("N5", "N4", "N3", "N2", "N1")) {
                add("${level}KanjiData.json")
                add("${level}VocabularyData.json")
                add("${level}GrammarData.json")
                add("${level}GrammarSyllabus.json")
                add("${level}StoriesData.json")
            }
        }

        assertEquals(28, expectedJsonFiles.size)

        expectedJsonFiles.forEach { fileName ->
            val file = File(sharedResourcesDir, fileName)
            assertTrue("Eksik ortak JSON dosyası: $fileName", file.exists() && file.length() > 0L)
        }
    }

    @Test
    fun `hiragana and audio manifest JSON files are parseable`() {
        val hiraganaFile = File(sharedResourcesDir, "HiraganaData.json")
        val hiraganaArray = Json.parseToJsonElement(hiraganaFile.readText()).jsonArray
        assertTrue("HiraganaData.json boş olmamalı", hiraganaArray.isNotEmpty())

        val audioManifestFile = File(sharedResourcesDir, "Audio/audio_manifest.json")
        assertTrue("Audio/audio_manifest.json mevcut olmalı", audioManifestFile.exists())
        val manifestObject = Json.parseToJsonElement(audioManifestFile.readText()).jsonObject
        assertTrue("Audio manifest boş olmamalı", manifestObject.isNotEmpty())
    }
}
