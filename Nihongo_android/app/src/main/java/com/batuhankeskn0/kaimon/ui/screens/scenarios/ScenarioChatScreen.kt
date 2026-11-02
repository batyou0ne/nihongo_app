package com.batuhankeskn0.kaimon.ui.screens.scenarios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.ConversationScenario
import com.batuhankeskn0.kaimon.data.model.ScenarioOption
import com.batuhankeskn0.kaimon.data.model.ScenarioStep
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Senaryo sohbet geçmişindeki tek bir mesaj balonunu temsil eder.
 */
data class ScenarioChatMessageItem(
    val id: String = UUID.randomUUID().toString(),
    val isBot: Boolean,
    val japanese: String,
    val romaji: String? = null,
    val translation: String? = null,
    val isSuccessFeedback: Boolean? = null
)

/**
 * İnteraktif diyalog senaryosunun adım ilerlemesini ve yanıt doğrulamasını yöneten durum denetleyicisi.
 * Hem `ScenarioChatScreen` tarafından hem de birim testleri tarafından doğrudan kullanılır.
 */
class ScenarioChatController(
    val scenario: ConversationScenario
) {
    var currentStepIndex: Int by mutableIntStateOf(0)
        private set

    val chatHistory = mutableStateListOf<ScenarioChatMessageItem>()

    var selectedOptionId: String? by mutableStateOf(null)
        private set

    var showFeedback: Boolean by mutableStateOf(false)
        private set

    var isBotTyping: Boolean by mutableStateOf(false)
        private set

    val isCompleted: Boolean
        get() = currentStepIndex >= scenario.steps.size

    val currentStep: ScenarioStep?
        get() = scenario.steps.getOrNull(currentStepIndex)

    /**
     * Belirtilen adım için bot yazıyor (`isBotTyping = true`) durumunu başlatır.
     */
    fun beginStepTyping(index: Int = currentStepIndex) {
        if (index >= scenario.steps.size) return
        isBotTyping = true
    }

    /**
     * Yazma gecikmesi tamamlandığında bot mesajını geçmişe ekler ve şıkları açar.
     */
    fun revealBotMessage(index: Int = currentStepIndex) {
        val step = scenario.steps.getOrNull(index) ?: run {
            isBotTyping = false
            return
        }
        chatHistory.add(
            ScenarioChatMessageItem(
                isBot = true,
                japanese = step.botMessage.japanese,
                romaji = step.botMessage.romaji,
                translation = step.botMessage.turkish,
                isSuccessFeedback = null
            )
        )
        isBotTyping = false
    }

    /**
     * Kullanıcının seçtiği şıkkı değerlendirir.
     * Doğruysa kullanıcı mesajını geçmişe ekler ve `true` döner; yanlışsa `false` döner.
     */
    fun evaluateOption(option: ScenarioOption): Boolean {
        selectedOptionId = option.id
        showFeedback = true

        return if (option.isCorrect) {
            FeedbackManager.shared.playSuccess()
            chatHistory.add(
                ScenarioChatMessageItem(
                    isBot = false,
                    japanese = option.japanese,
                    romaji = option.romaji,
                    translation = option.turkish,
                    isSuccessFeedback = true
                )
            )
            true
        } else {
            FeedbackManager.shared.playError()
            false
        }
    }

    /**
     * Doğru yanıt sonrası bir sonraki adıma ilerler.
     */
    fun advanceToNextStep() {
        selectedOptionId = null
        showFeedback = false
        currentStepIndex += 1
    }

    /**
     * Yanlış yanıt geri bildirimi sonrasında seçimi temizler ki kullanıcı tekrar deneyebilsin.
     */
    fun clearWrongSelection() {
        selectedOptionId = null
        showFeedback = false
    }
}

/**
 * İnteraktif Diyalog Senaryosu Sohbet Ekranı (`Views/Scenarios/ScenarioChatView.swift` karşılığı).
 *
 * - Karşı tarafın Japonca mesajını (`FuriganaText` + Romaji + Türkçe çeviri + sesli okuma butonu) gösterir.
 * - Kullanıcının 3-4 Japonca yanıttan doğru olanı seçerek diyaloğu ilerletmesini sağlar.
 */
@Composable
fun ScenarioChatScreen(
    scenario: ConversationScenario,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabBarManager = LocalTabBarManager.current
    val controller = remember(scenario.id) { ScenarioChatController(scenario) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(tabBarManager) {
        val wasHidden = tabBarManager.isHidden
        tabBarManager.hide()
        onDispose {
            if (!wasHidden) {
                tabBarManager.show()
            }
        }
    }

    BackHandler(onBack = onDismiss)

    // İlk adımı başlat
    LaunchedEffect(scenario.id) {
        if (controller.chatHistory.isEmpty() && scenario.steps.isNotEmpty()) {
            controller.beginStepTyping(0)
            delay(1_200L)
            controller.revealBotMessage(0)
        }
    }

    // Yeni mesaj eklendiğinde en alta kaydır
    LaunchedEffect(controller.chatHistory.size, controller.isBotTyping) {
        if (controller.chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(controller.chatHistory.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        // Üst Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                FeedbackManager.shared.playSelection()
                onDismiss()
            }) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = L10n.closeButton,
                    tint = Theme.secondaryInk,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = scenario.title,
                style = Theme.heading(19.sp),
                color = Theme.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(48.dp))
        }

        HorizontalDivider(color = Theme.ink.copy(alpha = 0.12f))

        // Sohbet Geçmişi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Theme.paper),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = controller.chatHistory,
                key = { it.id }
            ) { msg ->
                ScenarioChatBubble(
                    message = msg,
                    onSpeak = { text -> AudioService.shared.speak(text) }
                )
            }
        }

        // Alt Yanıt / Yazıyor / Tamamlama Alanı
        val activeStep = controller.currentStep
        if (activeStep != null) {
            if (controller.isBotTyping) {
                ScenarioTypingIndicator()
            } else {
                ScenarioOptionsPanel(
                    step = activeStep,
                    selectedOptionId = controller.selectedOptionId,
                    showFeedback = controller.showFeedback,
                    onSelectOption = { option ->
                        if (controller.showFeedback) return@ScenarioOptionsPanel
                        val isCorrect = controller.evaluateOption(option)
                        coroutineScope.launch {
                            if (isCorrect) {
                                delay(800L)
                                controller.advanceToNextStep()
                                if (!controller.isCompleted) {
                                    controller.beginStepTyping()
                                    delay(1_200L)
                                    controller.revealBotMessage()
                                }
                            } else {
                                delay(1_000L)
                                controller.clearWrongSelection()
                            }
                        }
                    }
                )
            }
        } else {
            ScenarioCompletionPanel(onFinish = onDismiss)
        }
    }
}

@Composable
private fun ScenarioChatBubble(
    message: ScenarioChatMessageItem,
    onSpeak: (String) -> Unit
) {
    val bubbleShape = RoundedCornerShape(16.dp)
    val isBot = message.isBot
    val primaryColor = if (isBot) Theme.ink else Theme.paper
    val secondaryColor = if (isBot) Theme.secondaryInk else Theme.paper.copy(alpha = 0.8f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
    ) {
        if (isBot) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Theme.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(bubbleShape)
                .background(
                    color = if (isBot) Theme.paper else Theme.ink,
                    shape = bubbleShape
                )
                .then(
                    if (isBot) {
                        Modifier.border(
                            width = 1.dp,
                            color = Theme.ink.copy(alpha = 0.25f),
                            shape = bubbleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = if (isBot) Alignment.Start else Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FuriganaText(
                    text = message.japanese,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    readingStyle = TextStyle(fontSize = 10.sp),
                    color = primaryColor,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isBot) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Theme.accent.copy(alpha = 0.12f))
                            .clickable { onSpeak(message.japanese) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = L10n.listenButton,
                            tint = Theme.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!message.romaji.isNullOrBlank()) {
                Text(
                    text = message.romaji,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = secondaryColor
                )
            }

            if (!message.translation.isNullOrBlank()) {
                HorizontalDivider(
                    color = if (isBot) Theme.ink.copy(alpha = 0.12f) else Theme.paper.copy(alpha = 0.2f)
                )
                Text(
                    text = message.translation,
                    fontSize = 13.sp,
                    color = secondaryColor
                )
            }

            message.isSuccessFeedback?.let { isSuccess ->
                Text(
                    text = if (isSuccess) L10n.correctFeedback else L10n.wrongFeedback,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) Theme.accent else Theme.secondaryInk
                )
            }
        }

        if (isBot) {
            Spacer(modifier = Modifier.width(40.dp))
        } else {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Theme.ink.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Theme.ink,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ScenarioTypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Theme.secondaryInk)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Theme.secondaryInk.copy(alpha = 0.7f))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Theme.secondaryInk.copy(alpha = 0.4f))
        )
        Text(
            text = L10n.typingIndicator,
            fontSize = 12.sp,
            color = Theme.secondaryInk
        )
    }
}

@Composable
private fun ScenarioOptionsPanel(
    step: ScenarioStep,
    selectedOptionId: String?,
    showFeedback: Boolean,
    onSelectOption: (ScenarioOption) -> Unit
) {
    val panelShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = panelShape,
                ambientColor = Theme.ink.copy(alpha = 0.12f),
                spotColor = Theme.ink.copy(alpha = 0.12f)
            )
            .clip(panelShape)
            .background(color = Theme.paper, shape = panelShape)
            .border(width = 1.5.dp, color = Theme.ink.copy(alpha = 0.18f), shape = panelShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = L10n.howToRespond,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Theme.secondaryInk
        )

        step.options.forEach { option ->
            val isSelected = selectedOptionId == option.id
            val isWrong = isSelected && showFeedback && !option.isCorrect
            val isRight = isSelected && showFeedback && option.isCorrect

            val optionShape = RoundedCornerShape(12.dp)
            val backgroundColor = when {
                isWrong -> Theme.accent
                isRight -> Theme.ink
                isSelected -> Theme.accent
                else -> Theme.paper
            }
            val borderColor = when {
                isWrong -> Theme.accent
                isRight -> Theme.ink
                isSelected -> Theme.accent
                else -> Theme.ink
            }
            val contentColor = if (isSelected) Theme.paper else Theme.ink
            val subColor = if (isSelected) Theme.paper.copy(alpha = 0.85f) else Theme.secondaryInk

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(optionShape)
                    .background(color = backgroundColor, shape = optionShape)
                    .border(width = 1.5.dp, color = borderColor, shape = optionShape)
                    .clickable(enabled = !(showFeedback && isSelected)) {
                        onSelectOption(option)
                    }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FuriganaText(
                    text = option.japanese,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    readingStyle = TextStyle(fontSize = 10.sp),
                    color = contentColor
                )

                if (option.romaji.isNotBlank()) {
                    Text(
                        text = option.romaji,
                        fontSize = 12.sp,
                        color = subColor
                    )
                }

                if (isWrong) {
                    Text(
                        text = L10n.wrongFeedback,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.paper
                    )
                } else if (isRight) {
                    Text(
                        text = L10n.correctFeedback,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.paper
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenarioCompletionPanel(
    onFinish: () -> Unit
) {
    val panelShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = panelShape,
                ambientColor = Theme.ink.copy(alpha = 0.12f),
                spotColor = Theme.ink.copy(alpha = 0.12f)
            )
            .clip(panelShape)
            .background(color = Theme.paper, shape = panelShape)
            .border(width = 1.5.dp, color = Theme.ink.copy(alpha = 0.18f), shape = panelShape)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🎉",
            fontSize = 44.sp
        )

        Text(
            text = L10n.congratulations,
            style = Theme.display(24.sp),
            color = Theme.ink
        )

        Text(
            text = L10n.scenarioComplete,
            fontSize = 15.sp,
            color = Theme.secondaryInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Theme.ink)
                .clickable {
                    FeedbackManager.shared.playSelection()
                    onFinish()
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = L10n.finishButton,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.paper
            )
        }
    }
}
