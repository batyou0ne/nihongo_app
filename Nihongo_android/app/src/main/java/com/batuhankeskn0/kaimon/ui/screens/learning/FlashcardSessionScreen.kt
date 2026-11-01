package com.batuhankeskn0.kaimon.ui.screens.learning

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.LearningSessionStateEntity
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.model.FlashcardItem
import com.batuhankeskn0.kaimon.data.service.AlphabetAudioService
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.domain.engine.SpacedRepetitionService
import com.batuhankeskn0.kaimon.domain.engine.XPManager
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.components.LessonCompleteView
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.screens.quiz.QuizScreen
import com.batuhankeskn0.kaimon.ui.screens.quiz.QuizViewModel
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Ortak Flashcard oturum durumunu ve çok turlu tekrar döngüsünü yöneten denetleyici.
 * Hem Compose UI (`FlashcardSessionScreen`) hem de birim testleri tarafından doğrudan kullanılır.
 *
 * iOS `Views/Learning/FlashcardSessionView.swift` içindeki oturum mantığının birebir karşılığıdır:
 * - `remainingItemIDs` ve `wrongItemIDs` takibi
 * - `currentCardID` ile kaldığı karttan devam etme
 * - Tur sonunda `wrongItemIDs` doluysa sadece yanlışlarla yeni tekrar turu başlatma
 * - Tüm kartlar bir turda eksiksiz doğru bilindiğinde `isCompleted = true` ve `SessionSummaryView` geçişi
 */
class FlashcardSessionController<Item : FlashcardItem>(
    val sessionKey: String,
    val itemKind: LearnableItemKind,
    val allItems: List<Item>,
    val distractorPool: List<Item> = allItems,
    private val alternateModeSelector: () -> Boolean = { Random.nextBoolean() },
    private val onSaveSession: (LearningSessionStateEntity) -> Unit = {},
    private val onSaveProgress: (LearningItemProgressEntity) -> Unit = {},
    private val onAwardXp: (XPManager.XPAction) -> Unit = {},
    private val onSessionCompleted: (String) -> Unit = {}
) {
    private val itemsByID: Map<String, Item> = allItems.associateBy { it.id }

    var progressByID: MutableMap<String, LearningItemProgressEntity> = mutableMapOf()
        private set

    var sessionState: LearningSessionStateEntity? by mutableStateOf(null)
        private set

    var quizViewModel: QuizViewModel<Item>? by mutableStateOf(null)
        private set

    var isFlipped: Boolean by mutableStateOf(false)

    var sessionEarnedXP: Int by mutableIntStateOf(0)
        private set

    var roundNumber: Int by mutableIntStateOf(1)
        private set

    /**
     * Toplam öğe sayısı üzerinden "kalıcı olarak ustalaşılan" oran (`0.0..1.0`).
     * `remainingItemIDs + wrongItemIDs` her zaman henüz bitmemiş öğeleri temsil eder.
     */
    val moduleProgress: Double
        get() {
            val session = sessionState ?: return 0.0
            if (allItems.isEmpty()) return 0.0
            val inPlay = session.remainingItemIDs.size + session.wrongItemIDs.size
            return max(0, allItems.size - inPlay).toDouble() / allItems.size.toDouble()
        }

    val masteredCount: Int
        get() {
            val session = sessionState ?: return 0
            val inPlay = session.remainingItemIDs.size + session.wrongItemIDs.size
            return max(0, allItems.size - inPlay)
        }

    val moduleProgressLabel: String
        get() = L10n.learnedCount(masteredCount, allItems.size)

    /**
     * İlerleme kayıtlarını ve mevcut oturum durumunu senkronize ederek turu başlatır.
     */
    fun setup(
        existingSession: LearningSessionStateEntity? = null,
        existingProgress: List<LearningItemProgressEntity> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ) {
        // 1. syncProgress
        val byID = LinkedHashMap<String, LearningItemProgressEntity>()
        for (prog in existingProgress) {
            if (prog.itemKind == itemKind && !byID.containsKey(prog.itemID)) {
                byID[prog.itemID] = prog
            }
        }
        for (item in allItems) {
            if (!byID.containsKey(item.id)) {
                val created = LearningItemProgressEntity(itemID = item.id, itemKind = itemKind)
                byID[item.id] = created
                onSaveProgress(created)
            }
        }
        progressByID = byID

        // 2. setupSession
        val session = existingSession?.copy(
            remainingItemIDs = existingSession.remainingItemIDs.toList(),
            wrongItemIDs = existingSession.wrongItemIDs.toList(),
            wrongAnswerCounts = existingSession.wrongAnswerCounts.toMap()
        ) ?: LearningSessionStateEntity(
            moduleType = sessionKey,
            remainingItemIDs = allItems.map { it.id }.shuffled()
        )

        session.lastOpenedAt = nowMillis

        // Geçerli olmayan ID'leri temizle
        val validIDs = allItems.map { it.id }.toSet()
        session.remainingItemIDs = session.remainingItemIDs.filter { validIDs.contains(it) }
        session.wrongItemIDs = session.wrongItemIDs.filter { validIDs.contains(it) }

        if (session.isCompleted) {
            // Daha önce tamamlanmış oturum yeniden açıldığında sıfırdan başlatılır
            session.isCompleted = false
            session.wrongItemIDs = emptyList()
            session.remainingItemIDs = allItems.map { it.id }.shuffled()
            resetSessionStats(session)
        } else if (session.remainingItemIDs.isEmpty() && session.wrongItemIDs.isNotEmpty()) {
            // Ana tur bitmiş, tekrar turu bekleniyor
            session.remainingItemIDs = session.wrongItemIDs.shuffled()
            session.wrongItemIDs = emptyList()
        } else if (session.remainingItemIDs.isEmpty()) {
            session.remainingItemIDs = allItems.map { it.id }.shuffled()
            resetSessionStats(session)
        }

        sessionState = session
        roundNumber = 1
        onSaveSession(session. snapshotCopy())
        startRound(session.remainingItemIDs)
    }

    /**
     * Verilen ID listesiyle yeni bir çalışma/tekrar turu başlatır.
     * `currentCardID` listede mevcutsa en başa alınır ki kullanıcı tam bıraktığı kartla karşılaşsın.
     */
    fun startRound(ids: List<String>) {
        val orderedIDs = ids.toMutableList()
        val currentId = sessionState?.currentCardID
        if (currentId != null) {
            val idx = orderedIDs.indexOf(currentId)
            if (idx >= 0) {
                orderedIDs.removeAt(idx)
                orderedIDs.shuffle()
                orderedIDs.add(0, currentId)
            } else {
                orderedIDs.shuffle()
            }
        } else {
            orderedIDs.shuffle()
        }

        val items = orderedIDs.mapNotNull { itemsByID[it] }
        val vm = QuizViewModel(
            questions = items,
            itemKind = itemKind,
            progressLookup = { progressByID[it] },
            shuffled = false,
            distractorPool = distractorPool,
            autoUpdateProgress = false,
            triggerFeedback = false,
            alternateModeSelector = alternateModeSelector
        )
        quizViewModel = vm
        isFlipped = false
        syncCurrentCard(vm)
    }

    /**
     * 4 şıktan birine tıklandığında çağrılır.
     */
    fun submitOption(option: String): Boolean {
        val vm = quizViewModel ?: return false
        val item = vm.currentQuestion ?: return false
        if (vm.selectedAnswer != null) return vm.isAnswerCorrect == true

        vm.submitAnswer(option)
        val isCorrect = (vm.isAnswerCorrect == true)
        recordAnswer(forItem = item, correct = isCorrect)
        return isCorrect
    }

    /**
     * "Öğrendim / Biliyorum" butonu veya sağa kaydırma (swipe right) aksiyonu.
     * Henüz cevaplanmadıysa doğru olarak kaydeder; zaten cevaplandıysa sonraki karta geçer.
     */
    fun markKnownOrAdvance(): Boolean {
        val vm = quizViewModel ?: return false
        val item = vm.currentQuestion ?: return false
        if (vm.selectedAnswer == null) {
            val expected = vm.currentExpectedAnswer ?: item.correctAnswer
            vm.submitAnswer(expected)
            recordAnswer(forItem = item, correct = true)
            isFlipped = true
            return true
        } else {
            advanceToNext()
            return true
        }
    }

    /**
     * "Tekrar Et / Bilmiyorum" butonu veya sola kaydırma (swipe left) aksiyonu.
     * Henüz cevaplanmadıysa yanlış olarak kaydedip kartı çevirir; zaten çevriliyse sonraki karta geçer.
     */
    fun markUnknownOrAdvance(): Boolean {
        val vm = quizViewModel ?: return false
        val item = vm.currentQuestion ?: return false
        if (vm.selectedAnswer == null) {
            vm.submitAnswer("__repeat__")
            recordAnswer(forItem = item, correct = false)
            isFlipped = true
            return false
        } else {
            advanceToNext()
            return false
        }
    }

    /**
     * Her cevaptan hemen sonra çağrılır: öğeyi `remainingItemIDs` listesinden çıkarır,
     * yanlışsa `wrongItemIDs` ve `wrongAnswerCounts` içine ekler, ilk denemeyse SRS ve XP günceller.
     */
    fun recordAnswer(forItem: Item, correct: Boolean) {
        val session = sessionState ?: return
        session.totalAnswerCount += 1
        session.remainingItemIDs = session.remainingItemIDs.filter { it != forItem.id }

        val isRetry = session.wrongAnswerCounts.containsKey(forItem.id)
        if (!isRetry) {
            val progress = progressByID[forItem.id]
            if (progress != null) {
                val wasLearned = progress.isLearned
                SpacedRepetitionService.shared.updateProgress(progress = progress, correct = correct)
                onSaveProgress(progress.copy())

                if (correct) {
                    val action = if (wasLearned) {
                        XPManager.XPAction.CARD_REVIEWED
                    } else {
                        XPManager.XPAction.NEW_CARD_LEARNED
                    }
                    sessionEarnedXP += action.points
                    onAwardXp(action)
                }
            }
        }

        if (correct) {
            session.wrongItemIDs = session.wrongItemIDs.filter { it != forItem.id }
        } else {
            if (!session.wrongItemIDs.contains(forItem.id)) {
                session.wrongItemIDs = session.wrongItemIDs + forItem.id
            }
            val updatedCounts = session.wrongAnswerCounts.toMutableMap()
            updatedCounts[forItem.id] = (updatedCounts[forItem.id] ?: 0) + 1
            session.wrongAnswerCounts = updatedCounts
            progressByID[forItem.id]?.let { prog ->
                prog.needsReview = true
                onSaveProgress(prog.copy())
            }
        }

        val snapshot = session.snapshotCopy()
        sessionState = snapshot
        onSaveSession(snapshot)
    }

    /**
     * Sonraki karta ilerler; turdaki son kartsa `handleRoundCompletionIfNeeded` tetikler.
     */
    fun advanceToNext() {
        val vm = quizViewModel ?: return
        if (vm.currentIndex + 1 >= vm.questions.size) {
            handleRoundCompletionIfNeeded(vm)
        } else {
            isFlipped = false
            vm.moveToNext()
            syncCurrentCard(vm)
        }
    }

    /**
     * Bir tur bittiğinde çağrılır: `wrongItemIDs` boşsa oturumu tamamlar,
     * doluysa sadece yanlış yapılan kartlarla yeni bir tekrar turu başlatır.
     */
    fun handleRoundCompletionIfNeeded(vm: QuizViewModel<Item>) {
        val session = sessionState ?: return

        if (session.wrongItemIDs.isEmpty()) {
            isFlipped = false
            vm.moveToNext()
            session.isCompleted = true
            session.currentCardID = null
            val snapshot = session.snapshotCopy()
            sessionState = snapshot
            onSaveSession(snapshot)
            onSessionCompleted(sessionKey)
        } else {
            val retryIDs = session.wrongItemIDs.shuffled()
            session.remainingItemIDs = retryIDs
            session.wrongItemIDs = emptyList()
            roundNumber += 1
            val snapshot = session.snapshotCopy()
            sessionState = snapshot
            onSaveSession(snapshot)
            isFlipped = false
            startRound(retryIDs)
        }
    }

    /**
     * Oturum sonu özet ekranı (`SessionSummaryView`) için yanlış yapılan öğelerin sıralı listesini döner.
     */
    fun buildWrongSummaryItems(): List<SessionSummaryItem> {
        val counts = sessionState?.wrongAnswerCounts ?: emptyMap()
        return counts.mapNotNull { (id, count) ->
            val item = itemsByID[id] ?: return@mapNotNull null
            SessionSummaryItem(
                id = id,
                prompt = item.prompt,
                detail = item.flipRecap,
                wrongCount = count
            )
        }.sortedByDescending { it.wrongCount }
    }

    private fun syncCurrentCard(vm: QuizViewModel<Item>?) {
        val session = sessionState ?: return
        session.currentCardID = vm?.currentQuestion?.id
        val snapshot = session.snapshotCopy()
        sessionState = snapshot
        onSaveSession(snapshot)
    }

    private fun resetSessionStats(session: LearningSessionStateEntity) {
        session.totalAnswerCount = 0
        session.wrongAnswerCounts = emptyMap()
    }

    private fun LearningSessionStateEntity.snapshotCopy(): LearningSessionStateEntity =
        copy(
            remainingItemIDs = remainingItemIDs.toList(),
            wrongItemIDs = wrongItemIDs.toList(),
            wrongAnswerCounts = wrongAnswerCounts.toMap()
        )

    companion object {
        /**
         * `"kanji_N5_part1"` -> `Triple("kanji", JLPTLevel.N5, 0)`
         * `"vocab_N4_part3"` -> `Triple("vocab", JLPTLevel.N4, 2)`
         * iOS `FlashcardSessionView.parseSessionKey` metodunun birebir karşılığıdır.
         */
        fun parseSessionKey(key: String): Triple<String, JLPTLevel, Int>? {
            val prefixes = listOf("kanji_" to "kanji", "vocab_" to "vocab")
            for ((prefix, module) in prefixes) {
                if (key.startsWith(prefix)) {
                    val rest = key.removePrefix(prefix)
                    val pieces = rest.split("_part")
                    if (pieces.size == 2) {
                        val level = JLPTLevel.fromRawValue(pieces[0])
                        val number = pieces[1].toIntOrNull()
                        if (level != null && number != null && number > 0) {
                            return Triple(module, level, number - 1)
                        }
                    }
                }
            }
            return null
        }
    }
}

/**
 * Ortak Flashcard Oturum Ekranı (`Views/Learning/FlashcardSessionView.swift` karşılığı).
 *
 * Hem Hiragana/Katakana (`LearningScreen`), hem Kanji, hem de Kelime modülleri bu generic bileşeni kullanır:
 * - 3B Y-ekseni kart çevirme animasyonu (`graphicsLayer { rotationY = ... }`)
 * - Ön yüzde büyük karakter/kelime, üstte opsiyonel okunuş (`promptReading`) ve ses butonu
 * - Arka yüzde `flipRecap`, anlam, örnek kelimeler (`exampleWords`) ve örnek cümle (`contextHint`)
 * - 4 şıklı çoktan seçmeli ızgara + "Tekrar Et" / "Öğrendim" aksiyon butonları + sağa/sola kaydırma (swipe) jestleri
 * - Her cevaptan sonra `LearningSessionStateEntity` güncellemesi ve tur sonunda yanlışlarla tekrar turu döngüsü
 * - Tümü doğru yapıldığında `LessonCompleteView` + `SessionSummaryView`, `XPManager` ve `UserProgress.recordStudySession()`
 * - Oturum açıkken `TabBarManager` ile alt barı gizleme, çıkarken geri açma
 */
@Composable
fun <Item : FlashcardItem> FlashcardSessionScreen(
    sessionKey: String,
    itemKind: LearnableItemKind,
    allItems: List<Item>,
    modifier: Modifier = Modifier,
    distractorPool: List<Item> = allItems,
    accentColor: Color = Theme.accent,
    title: String,
    onBack: () -> Unit = {}
) {
    val tabBarManager = LocalTabBarManager.current
    val scope = rememberCoroutineScope()

    var isPresentingQuiz by remember { mutableStateOf(false) }
    var hasRecordedStreak by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var showLessonComplete by remember { mutableStateOf(false) }
    var completedStreak by remember { mutableIntStateOf(0) }
    var didStreakIncrease by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var autoAdvanceJob by remember { mutableStateOf<Job?>(null) }

    val controller = remember(sessionKey, itemKind, allItems) {
        FlashcardSessionController(
            sessionKey = sessionKey,
            itemKind = itemKind,
            allItems = allItems,
            distractorPool = distractorPool,
            onSaveSession = { session ->
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        UserDatabaseManager.shared.sessionDao.upsertSession(session)
                    }
                }
            },
            onSaveProgress = { progress ->
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        UserDatabaseManager.shared.progressDao.upsertItemProgress(progress)
                    }
                }
            },
            onAwardXp = { action ->
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        XPManager.shared.addXP(action = action)
                    }
                }
            }
        )
    }

    // TabBar gizleme ve çıkarken geri açma + autoAdvanceJob iptali
    DisposableEffect(tabBarManager) {
        tabBarManager.hide()
        onDispose {
            autoAdvanceJob?.cancel()
            AudioService.shared.stop()
            tabBarManager.show()
        }
    }

    BackHandler {
        if (isPresentingQuiz) {
            isPresentingQuiz = false
        } else {
            autoAdvanceJob?.cancel()
            onBack()
        }
    }

    // Room'dan mevcut ilerleme ve oturum durumunu yükle
    LaunchedEffect(sessionKey, allItems.size) {
        if (allItems.isNotEmpty() && !isReady) {
            val (existingSession, existingProgress) = withContext(Dispatchers.IO) {
                val db = UserDatabaseManager.shared.getDatabase()
                val sess = runCatching { db.sessionDao().getSession(sessionKey) }.getOrNull()
                val prog = runCatching { db.progressDao().getProgressByKind(itemKind) }.getOrDefault(emptyList())
                sess to prog
            }
            controller.setup(
                existingSession = existingSession,
                existingProgress = existingProgress
            )
            isReady = true
        }
    }

    val vm = controller.quizViewModel
    LaunchedEffect(vm?.currentIndex, controller.roundNumber) {
        showHint = false
    }

    // Oturum tamamlandığında seri (streak), XP ve bölüm kilidi (LevelProgressionService) güncellemesi
    val isSessionCompleted = controller.sessionState?.isCompleted == true
    LaunchedEffect(isSessionCompleted) {
        if (isSessionCompleted && !hasRecordedStreak) {
            hasRecordedStreak = true
            withContext(Dispatchers.IO) {
                runCatching {
                    val db = UserDatabaseManager.shared.getDatabase()
                    val userProgress = db.progressDao().getOrCreateUserProgress()
                    val prevStreak = userProgress.activeStreak
                    userProgress.recordStudySession()
                    db.progressDao().upsertUserProgress(userProgress)
                    val newStreak = userProgress.activeStreak

                    completedStreak = newStreak
                    didStreakIncrease = (newStreak > prevStreak)

                    FlashcardSessionController.parseSessionKey(sessionKey)?.let { (module, level, partIdx) ->
                        LevelProgressionService.shared.markPartCompleted(
                            module = module,
                            level = level,
                            partIndex = partIdx,
                            dao = db.levelProgressDao()
                        )
                    }
                }
            }
            showLessonComplete = true
        }
    }

    if (isPresentingQuiz) {
        QuizScreen(
            questions = allItems,
            itemKind = itemKind,
            accentColor = accentColor,
            distractorPool = distractorPool,
            progressLookup = { controller.progressByID[it] },
            onDismiss = { isPresentingQuiz = false },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Üst Bar: Geri Butonu + Başlık + Hızlı Quiz Butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            autoAdvanceJob?.cancel()
                            onBack()
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = L10n.backButton,
                        tint = Theme.ink,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = L10n.backButton,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Theme.ink
                    )
                }

                Text(
                    text = title,
                    style = Theme.heading(18.sp),
                    color = Theme.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = L10n.quickQuiz,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier
                        .clickable {
                            autoAdvanceJob?.cancel()
                            isPresentingQuiz = true
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                )
            }

            if (!isReady || vm == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (isSessionCompleted) {
                val wrongSummaryItems = remember(controller.sessionState?.wrongAnswerCounts) {
                    controller.buildWrongSummaryItems()
                }
                SessionSummaryView(
                    totalAnswers = controller.sessionState?.totalAnswerCount ?: vm.questions.size,
                    wrongItems = wrongSummaryItems,
                    onFinish = onBack,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                FlashcardActiveSessionContent(
                    controller = controller,
                    vm = vm,
                    itemKind = itemKind,
                    accentColor = accentColor,
                    showHint = showHint,
                    onShowHint = { showHint = true },
                    onOptionSelected = { option ->
                        if (vm.selectedAnswer != null) return@FlashcardActiveSessionContent
                        val isCorrect = controller.submitOption(option)
                        if (isCorrect) {
                            FeedbackManager.shared.playSuccess()
                            AudioService.shared.playCorrectSound()
                        } else {
                            FeedbackManager.shared.playError()
                            AudioService.shared.playWrongSound()
                        }

                        autoAdvanceJob?.cancel()
                        autoAdvanceJob = scope.launch {
                            delay(700L)
                            controller.isFlipped = true
                            if (isCorrect) {
                                delay(2800L)
                                controller.advanceToNext()
                            }
                        }
                    },
                    onCardTapped = {
                        if (controller.isFlipped) {
                            autoAdvanceJob?.cancel()
                            controller.advanceToNext()
                        } else {
                            controller.isFlipped = true
                        }
                    },
                    onKnownAction = {
                        autoAdvanceJob?.cancel()
                        val wasUnanswered = vm.selectedAnswer == null
                        controller.markKnownOrAdvance()
                        if (wasUnanswered) {
                            FeedbackManager.shared.playSuccess()
                            AudioService.shared.playCorrectSound()
                            autoAdvanceJob = scope.launch {
                                delay(1200L)
                                controller.advanceToNext()
                            }
                        } else {
                            FeedbackManager.shared.playSelection()
                        }
                    },
                    onRepeatAction = {
                        autoAdvanceJob?.cancel()
                        val wasUnanswered = vm.selectedAnswer == null
                        controller.markUnknownOrAdvance()
                        if (wasUnanswered) {
                            FeedbackManager.shared.playError()
                            AudioService.shared.playWrongSound()
                        } else {
                            FeedbackManager.shared.playSelection()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showLessonComplete) {
            val earnedXp = if (controller.sessionEarnedXP > 0) {
                controller.sessionEarnedXP
            } else {
                max(allItems.size * 2, 10)
            }
            LessonCompleteView(
                xpGained = earnedXp,
                streak = completedStreak,
                streakIncreased = didStreakIncrease,
                ringColor = accentColor,
                onContinue = { showLessonComplete = false }
            )
        }
    }
}

@Composable
private fun <Item : FlashcardItem> FlashcardActiveSessionContent(
    controller: FlashcardSessionController<Item>,
    vm: QuizViewModel<Item>,
    itemKind: LearnableItemKind,
    accentColor: Color,
    showHint: Boolean,
    onShowHint: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onCardTapped: () -> Unit,
    onKnownAction: () -> Unit,
    onRepeatAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = vm.currentQuestion ?: return
    var dragOffsetX by remember(question.id, controller.roundNumber) { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Modül İlerleme Çubuğu + Etiketi
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProgressBar(fraction = controller.moduleProgress)
            Text(
                text = controller.moduleProgressLabel,
                fontSize = 12.sp,
                color = Theme.secondaryInk
            )
        }

        // 3B Çevrilebilir Flashcard + Sağa/Sola Kaydırma (Swipe) Jestleri
        FlashcardFlipCard(
            item = question,
            itemKind = itemKind,
            isFlipped = controller.isFlipped,
            isAlternateMode = vm.currentQuestionModeIsAlternate,
            accentColor = accentColor,
            dragOffsetX = dragOffsetX,
            onDragDelta = { delta -> dragOffsetX += delta },
            onDragEnd = {
                val threshold = 110f
                when {
                    dragOffsetX > threshold -> {
                        dragOffsetX = 0f
                        onKnownAction()
                    }
                    dragOffsetX < -threshold -> {
                        dragOffsetX = 0f
                        onRepeatAction()
                    }
                    else -> {
                        dragOffsetX = 0f
                    }
                }
            },
            onClick = onCardTapped
        )

        // Bağlam İpucu (ContextHint) — Kart Çevrilmemişken
        if (!controller.isFlipped) {
            val hint = question.contextHint
            if (vm.currentQuestionModeIsAlternate && hint != null) {
                Text(
                    text = hint.translation,
                    fontSize = 15.sp,
                    color = Theme.secondaryInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else if (hint != null) {
                if (showHint) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = hint.sentence,
                            style = Theme.heading(18.sp),
                            color = Theme.accent,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = hint.translation,
                            fontSize = 13.sp,
                            color = Theme.secondaryInk,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = L10n.hintButton,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.secondaryInk,
                        modifier = Modifier
                            .clickable(onClick = onShowHint)
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    )
                }
            }
        }

        // 4 Şıklı Seçim Izgarası (2x2 Brutalist Grid - FlashcardSessionView.swift optionsGrid)
        FlashcardOptionsGrid(
            options = vm.options,
            selectedAnswer = vm.selectedAnswer,
            isAnswerCorrect = vm.isAnswerCorrect,
            onSelectOption = onOptionSelected
        )

        // "Tekrar Et / Bilmiyorum" ve "Öğrendim / Biliyorum" Aksiyon Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(color = Theme.paper, shape = RectangleShape)
                    .border(width = 2.dp, color = Theme.ink, shape = RectangleShape)
                    .clickable(onClick = onRepeatAction)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = L10n.flashcardRepeatButton,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.ink
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(color = Theme.paper, shape = RectangleShape)
                    .border(width = 2.dp, color = Theme.ink, shape = RectangleShape)
                    .clickable(onClick = onKnownAction)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Theme.success,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = L10n.flashcardKnownButton,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.ink
                )
            }
        }

        // Yanlış Cevapta Devam Etme İpucu
        if (controller.isFlipped && vm.isAnswerCorrect == false) {
            Text(
                text = L10n.tapToContinue,
                fontSize = 13.sp,
                color = Theme.secondaryInk,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = L10n.swipeHint,
                fontSize = 12.sp,
                color = Theme.secondaryInk.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 3B Y-ekseni çevirme (`graphicsLayer { rotationY = ... }`) ve yatay kaydırma (swipe) destekli Flashcard.
 */
@Composable
private fun <Item : FlashcardItem> FlashcardFlipCard(
    item: Item,
    itemKind: LearnableItemKind,
    isFlipped: Boolean,
    isAlternateMode: Boolean,
    accentColor: Color,
    dragOffsetX: Float,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "FlashcardRotationY"
    )

    val showingBack = rotationY > 90f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount)
                    }
                )
            }
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = 12f * density
            }
            .background(color = Theme.paper, shape = RectangleShape)
            .border(width = 2.dp, color = accentColor, shape = RectangleShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!showingBack) {
            // Ön Yüz
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val reading = item.promptReading
                if (!reading.isNullOrEmpty()) {
                    Text(
                        text = reading,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Theme.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val promptText = if (isAlternateMode) {
                    item.alternatePrompt ?: item.prompt
                } else {
                    item.prompt
                }
                val fontSize = when {
                    isAlternateMode -> 22.sp
                    promptText.length > 4 -> 36.sp
                    else -> 76.sp
                }

                if (itemKind == LearnableItemKind.HIRAGANA_CHARACTER ||
                    itemKind == LearnableItemKind.KATAKANA_CHARACTER
                ) {
                    Text(
                        text = promptText,
                        style = Theme.display(fontSize),
                        color = Theme.ink,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                } else {
                    FuriganaText(
                        text = promptText,
                        style = Theme.display(fontSize),
                        color = Theme.ink
                    )
                }

                // Sesli Okuma Butonu
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                        .clickable {
                            if (itemKind == LearnableItemKind.HIRAGANA_CHARACTER ||
                                itemKind == LearnableItemKind.KATAKANA_CHARACTER
                            ) {
                                AlphabetAudioService.shared.play(item.speechText)
                            } else {
                                AudioService.shared.speak(item.speechText)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = L10n.listenPronunciation,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // Arka Yüz (rotationY = 180f ile düzeltilmiş)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.rotationY = 180f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.flipRecap,
                    style = Theme.heading(17.sp),
                    color = Theme.accent,
                    textAlign = TextAlign.Center
                )

                if (item.exampleWords.isEmpty()) {
                    Text(
                        text = item.correctAnswer,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.ink,
                        textAlign = TextAlign.Center
                    )
                } else {
                    item.exampleWords.take(2).forEach { word ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = word.hiragana,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Theme.ink
                            )
                            Text(
                                text = "${word.romaji} · ${word.turkishMeaning}",
                                fontSize = 13.sp,
                                color = Theme.secondaryInk,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                val hint = item.contextHint
                if (hint != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = hint.sentence,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Theme.ink,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = hint.translation,
                            fontSize = 12.sp,
                            color = Theme.secondaryInk,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2x2 Brutalist 4 şık ızgarası (`FlashcardSessionView.swift` -> `optionsGrid`).
 */
@Composable
private fun FlashcardOptionsGrid(
    options: List<String>,
    selectedAnswer: String?,
    isAnswerCorrect: Boolean?,
    onSelectOption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = options.chunked(2)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEach { option ->
                    val isSelected = selectedAnswer == option
                    val bgColor = when {
                        !isSelected -> Theme.paper
                        isAnswerCorrect == true -> Theme.success
                        else -> Theme.accent
                    }
                    val fgColor = if (isSelected) Color.White else Theme.ink

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .background(color = bgColor, shape = RectangleShape)
                            .border(width = 2.dp, color = Theme.ink, shape = RectangleShape)
                            .clickable(enabled = selectedAnswer == null) {
                                onSelectOption(option)
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = fgColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
