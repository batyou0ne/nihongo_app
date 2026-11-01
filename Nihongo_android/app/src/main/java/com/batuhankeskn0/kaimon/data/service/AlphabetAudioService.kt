package com.batuhankeskn0.kaimon.data.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hiragana ve Katakana alfabe tablosundaki karakterlerin sesli telaffuzunu yöneten servis.
 * Dokunulduğunda hafif dokunsal geri bildirim (haptic) üretir, `AudioService` üzerinden
 * karakterin sesini çalar ve 700ms boyunca `currentCharacter` durumunu güncel tutar.
 * iOS `AlphabetAudioService.swift` karşılığıdır.
 */
@Singleton
class AlphabetAudioService {

    private val audioService: AudioService
    private val feedbackManager: FeedbackManager
    private val scope: CoroutineScope

    private var resetJob: Job? = null

    private var _currentCharacterCompose by mutableStateOf<String?>(null)
    private val _currentCharacterFlow = MutableStateFlow<String?>(null)

    /** O an çalınmakta olan karakterin kimliği (`StateFlow`). */
    val currentCharacterFlow: StateFlow<String?> = _currentCharacterFlow.asStateFlow()

    /** O an çalınmakta olan karakterin kimliği (görsel vurgulama ve animasyon için Compose state). */
    var currentCharacter: String?
        get() = _currentCharacterCompose
        private set(value) {
            _currentCharacterCompose = value
            _currentCharacterFlow.value = value
        }

    @Inject
    constructor(
        audioService: AudioService,
        feedbackManager: FeedbackManager
    ) {
        this.audioService = audioService
        this.feedbackManager = feedbackManager
        this.scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        sharedInstance = this
    }

    constructor(
        audioService: AudioService = AudioService.shared,
        feedbackManager: FeedbackManager = FeedbackManager.shared,
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ) {
        this.audioService = audioService
        this.feedbackManager = feedbackManager
        this.scope = CoroutineScope(SupervisorJob() + dispatcher)
    }

    /**
     * Verilen karakterin sesini çalar, hafif haptic üretir ve 700ms boyunca görsel vurguyu korur.
     */
    fun play(character: String) {
        feedbackManager.playSelection()
        audioService.speak(character)
        currentCharacter = character

        resetJob?.cancel()
        resetJob = scope.launch {
            delay(HIGHLIGHT_DURATION_MS)
            currentCharacter = null
        }
    }

    /**
     * Ses çalmayı ve görsel vurguyu durdurur.
     */
    fun stop() {
        resetJob?.cancel()
        resetJob = null
        currentCharacter = null
        audioService.stop()
    }

    companion object {
        const val HIGHLIGHT_DURATION_MS = 700L

        @Volatile
        private var sharedInstance: AlphabetAudioService = AlphabetAudioService()

        val shared: AlphabetAudioService
            get() = sharedInstance
    }
}
