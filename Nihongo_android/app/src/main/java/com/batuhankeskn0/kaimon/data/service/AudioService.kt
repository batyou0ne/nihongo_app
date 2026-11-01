package com.batuhankeskn0.kaimon.data.service

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metin okuma (Text-to-Speech) ve önceden üretilmiş MP3 ses oynatma işlemlerini yöneten servis.
 * iOS `AudioService.swift` karşılığıdır.
 *
 * **Ses Öncelik Sırası:**
 * 1. Önceden üretilmiş ses dosyası (`assets/Audio/audio_manifest.json` -> `assets/Audio/<filename>.mp3`)
 * 2. Android yerleşik `TextToSpeech` (`Locale.JAPAN`) fallback
 */
@Singleton
class AudioService private constructor() {

    private var appContext: Context? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var audioManifest: Map<String, String> = emptyMap()

    private var mediaPlayer: MediaPlayer? = null
    private var effectPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null

    @Volatile
    private var isTtsReady: Boolean = false

    @Volatile
    private var pendingTtsRequest: PendingTtsRequest? = null

    private data class PendingTtsRequest(
        val text: String,
        val languageCode: String,
        val rate: Float
    )

    private var _isSpeakingCompose by mutableStateOf(false)
    private val _isSpeakingFlow = MutableStateFlow(false)

    /** Ses çalma durumu (Coroutines / ViewModel için `StateFlow`). */
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    /** Ses çalma durumu (Compose UI için reaktif state). */
    var isSpeaking: Boolean
        get() = _isSpeakingCompose
        private set(value) {
            _isSpeakingCompose = value
            _isSpeakingFlow.value = value
        }

    @Inject
    constructor(@ApplicationContext context: Context) : this() {
        attachContext(context)
        sharedInstance = this
    }

    internal fun attachContext(context: Context) {
        val appCtx = context.applicationContext
        this.appContext = appCtx
        if (audioManifest.isEmpty()) {
            loadAudioManifestSync(appCtx)
        }
    }

    // MARK: - Manifest Yükleme

    /**
     * `assets/Audio/audio_manifest.json` (veya `assets/audio_manifest.json`) dosyasından
     * Japonca metin -> `.mp3` dosya adı eşlemesini yükler.
     */
    internal fun loadAudioManifestSync(context: Context? = appContext) {
        val ctx = context ?: return
        val rawJson = runCatching {
            ctx.assets.open("Audio/audio_manifest.json").bufferedReader().use { it.readText() }
        }.recoverCatching {
            ctx.assets.open("audio_manifest.json").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return

        runCatching {
            val parsed = json.decodeFromString<Map<String, String>>(rawJson)
            val normalizedMap = LinkedHashMap<String, String>(parsed.size * 2)
            for ((k, v) in parsed) {
                normalizedMap[k] = v
                normalizedMap[normalizeKey(k)] = v
            }
            audioManifest = normalizedMap
        }
    }

    /** Yüklü ses manifest kayıt sayısı (test ve doğrulama için). */
    val manifestSize: Int
        get() = audioManifest.size

    /** Verilen metin için manifest'te eşleşen `.mp3` dosya adını döner. */
    fun resolveAudioFilename(text: String): String? {
        if (text.isBlank()) return null
        audioManifest[text]?.let { return it }
        val normalized = normalizeKey(text)
        audioManifest[normalized]?.let { return it }
        val strippedPunctuation = normalized.trimEnd('。', '！', '？', '.', '!', '?', '、', ',', ' ', '　')
        return audioManifest[strippedPunctuation]
    }

    // MARK: - Ses Çalma

    /**
     * Verilen metni okur. Önce `audio_manifest.json` üzerinden önceden üretilmiş `.mp3` ses dosyasını arar,
     * bulamazsa Android yerleşik `TextToSpeech` (`Locale.JAPAN`) motoruna düşer.
     */
    fun speak(text: String, languageCode: String = "ja-JP", rate: Float = 0.85f) {
        if (text.isBlank()) return
        stop()

        if (playPrerecordedAudio(text)) {
            return
        }

        speakWithTTS(normalizeKey(text), languageCode = languageCode, rate = rate)
    }

    /**
     * Önceden üretilmiş `.mp3` ses dosyasını `AssetFileDescriptor` + `MediaPlayer` ile çalar.
     */
    private fun playPrerecordedAudio(text: String): Boolean {
        val ctx = appContext ?: sharedInstance.appContext ?: return false
        if (audioManifest.isEmpty()) {
            loadAudioManifestSync(ctx)
        }
        val filename = resolveAudioFilename(text) ?: return false

        val afd: AssetFileDescriptor = runCatching {
            ctx.assets.openFd("Audio/$filename")
        }.recoverCatching {
            ctx.assets.openFd(filename)
        }.getOrNull() ?: return false

        return runCatching {
            releaseMediaPlayer()
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            afd.use { descriptor ->
                player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            player.setOnCompletionListener {
                isSpeaking = false
                releaseMediaPlayer()
            }
            player.setOnErrorListener { _, _, _ ->
                isSpeaking = false
                releaseMediaPlayer()
                true
            }
            player.prepare()
            player.start()
            isSpeaking = true
            true
        }.getOrElse {
            isSpeaking = false
            releaseMediaPlayer()
            false
        }
    }

    /**
     * Android yerleşik `TextToSpeech` (`Locale.JAPAN`) ile metni okur (fallback).
     */
    private fun speakWithTTS(text: String, languageCode: String, rate: Float) {
        val ctx = appContext ?: sharedInstance.appContext ?: return
        val request = PendingTtsRequest(text = text, languageCode = languageCode, rate = rate)

        val currentTts = tts
        if (currentTts != null && isTtsReady) {
            executeTtsRequest(currentTts, request)
            return
        }

        pendingTtsRequest = request
        if (currentTts == null) {
            runCatching {
                tts = TextToSpeech(ctx) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        isTtsReady = true
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                isSpeaking = true
                            }

                            override fun onDone(utteranceId: String?) {
                                isSpeaking = false
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                isSpeaking = false
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                isSpeaking = false
                            }
                        })
                        val pending = pendingTtsRequest
                        val activeTts = tts
                        if (pending != null && activeTts != null) {
                            pendingTtsRequest = null
                            executeTtsRequest(activeTts, pending)
                        }
                    } else {
                        isTtsReady = false
                        isSpeaking = false
                    }
                }
            }
        }
    }

    private fun executeTtsRequest(engine: TextToSpeech, request: PendingTtsRequest) {
        runCatching {
            val locale = if (request.languageCode.startsWith("ja", ignoreCase = true)) {
                Locale.JAPAN
            } else {
                Locale.forLanguageTag(request.languageCode)
            }
            engine.language = locale
            // iOS AVSpeechUtterance rate (0.42) -> Android TTS speechRate (~0.85f)
            val normalizedRate = if (request.rate <= 0.55f) 0.85f else request.rate
            engine.setSpeechRate(normalizedRate)
            val utteranceId = UUID.randomUUID().toString()
            isSpeaking = true
            engine.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }.onFailure {
            isSpeaking = false
        }
    }

    // MARK: - Ses Efektleri

    /** Doğru cevap ses efektini çalar. */
    fun playCorrectSound() {
        playSoundEffect("correct-audio.wav")
    }

    /** Yanlış cevap ses efektini çalar. */
    fun playWrongSound() {
        playSoundEffect("wrong-audio.wav")
    }

    private fun playSoundEffect(filename: String) {
        val ctx = appContext ?: sharedInstance.appContext ?: return
        val afd = runCatching {
            ctx.assets.openFd("Audio/$filename")
        }.recoverCatching {
            ctx.assets.openFd(filename)
        }.getOrNull() ?: return

        runCatching {
            releaseEffectPlayer()
            val player = MediaPlayer()
            effectPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            afd.use { descriptor ->
                player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            player.setVolume(0.6f, 0.6f)
            player.setOnCompletionListener { releaseEffectPlayer() }
            player.setOnErrorListener { _, _, _ ->
                releaseEffectPlayer()
                true
            }
            player.prepare()
            player.start()
        }.onFailure {
            releaseEffectPlayer()
        }
    }

    // MARK: - Durdurma ve Kaynak Temizliği

    /** Devam eden tüm okumaları ve ses çalmaları durdurur. */
    fun stop() {
        pendingTtsRequest = null
        runCatching {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        }
        releaseMediaPlayer()
        isSpeaking = false
    }

    /** Tüm MediaPlayer ve TextToSpeech kaynaklarını serbest bırakır. */
    fun release() {
        stop()
        releaseEffectPlayer()
        runCatching {
            tts?.shutdown()
        }
        tts = null
        isTtsReady = false
    }

    private fun releaseMediaPlayer() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        runCatching {
            if (player.isPlaying) {
                player.stop()
            }
        }
        runCatching {
            player.reset()
            player.release()
        }
    }

    private fun releaseEffectPlayer() {
        val player = effectPlayer ?: return
        effectPlayer = null
        runCatching {
            if (player.isPlaying) {
                player.stop()
            }
        }
        runCatching {
            player.reset()
            player.release()
        }
    }

    companion object {
        private val furiganaBracketRegex = Regex("\\[[ぁ-んァ-ンー]+\\]")

        @Volatile
        private var sharedInstance: AudioService = AudioService()

        val shared: AudioService
            get() = sharedInstance

        /**
         * Manifest anahtarı araması için metni normalize eder:
         * 1) Unicode NFC normalizasyonu (ayrık dakuten karakterlerini birleştirir)
         * 2) Varsa `漢字[かんじ]` şeklindeki furigana köşeli parantez etiketlerini temizler
         * 3) Baş ve sondaki boşlukları kırpar.
         */
        fun normalizeKey(text: String): String {
            if (text.isEmpty()) return ""
            val nfc = Normalizer.normalize(text, Normalizer.Form.NFC)
            return furiganaBracketRegex.replace(nfc, "").trim()
        }

        fun init(context: Context) {
            val appCtx = context.applicationContext
            sharedInstance.appContext = appCtx
            sharedInstance.serviceScope.launch {
                sharedInstance.loadAudioManifestSync(appCtx)
            }
        }

        fun speak(text: String, languageCode: String = "ja-JP", rate: Float = 0.85f) {
            sharedInstance.speak(text, languageCode, rate)
        }

        fun stop() {
            sharedInstance.stop()
        }
    }
}
