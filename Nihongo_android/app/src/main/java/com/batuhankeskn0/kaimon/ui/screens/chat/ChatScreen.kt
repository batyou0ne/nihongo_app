package com.batuhankeskn0.kaimon.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batuhankeskn0.kaimon.data.model.ChatMessage
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.components.AIQuotaBanner
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered

/**
 * AI Dil Partneri Sohbet Ekranı (`Views/Chat/ChatView.swift` karşılığı).
 *
 * - Mesaj balonları (`UserBubble`, `BotBubble`, `ErrorBubble`).
 * - Gelen Japonca yanıtlarda `FuriganaText`, Türkçe çeviri ayrıştırması ve sesli dinleme (`AudioService.speak`) butonu.
 * - Yazıyor (`TypingIndicator`) animasyonu.
 * - `FeatureGate` günlük ücretsiz mesaj kotası (`remainingAIMessages`) göstergesi ve kota dolduğunda Pro Paywall tetikleyicisi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    featureGate: FeatureGate = FeatureGate.shared,
    onDismiss: () -> Unit = {},
    onOpenPaywall: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabBarManager = LocalTabBarManager.current
    val listState = rememberLazyListState()

    var inputText by rememberSaveable { mutableStateOf("") }
    var showPaywall by rememberSaveable { mutableStateOf(false) }

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

    // Yeni mesaj geldiğinde veya yazıyor göstergesi açıldığında en alta kaydır
    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        val totalItems = uiState.messages.size + if (uiState.isTyping) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val handleSend = {
        val trimmed = inputText.trim()
        if (trimmed.isNotEmpty()) {
            if (!featureGate.canSendAIMessage) {
                FeedbackManager.shared.playSelection()
                showPaywall = true
                onOpenPaywall?.invoke()
            } else {
                FeedbackManager.shared.playSelection()
                viewModel.sendMessage(trimmed)
                inputText = ""
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .imePadding()
    ) {
        // Üst Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.paper)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = L10n.closeButton,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme.accent,
                modifier = Modifier.clickable {
                    FeedbackManager.shared.playSelection()
                    onDismiss()
                }
            )

            Text(
                text = L10n.aiPracticeTitle,
                style = Theme.heading(19.sp),
                color = Theme.ink,
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = {
                    FeedbackManager.shared.playSelection()
                    viewModel.clearMessages()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = Theme.secondaryInk,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = Theme.ink.copy(alpha = 0.15f))

        // Mesaj Listesi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Theme.paper.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = uiState.messages,
                key = { it.id }
            ) { message ->
                MessageBubble(
                    message = message,
                    onRetry = {
                        FeedbackManager.shared.playSelection()
                        viewModel.retryLastMessage()
                    }
                )
            }

            if (uiState.isTyping) {
                item(key = "TypingIndicator") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        val indicatorShape = RoundedCornerShape(16.dp)
                        Box(
                            modifier = Modifier
                                .clip(indicatorShape)
                                .background(Theme.paper, indicatorShape)
                                .border(1.5.dp, Theme.ink, indicatorShape)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            TypingIndicator()
                        }
                    }
                }
            }
        }

        // Alt Giriş ve Kota Alanı
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.paper)
        ) {
            HorizontalDivider(color = Theme.ink, thickness = 1.dp)

            val remaining = featureGate.remainingAIMessages
            if (remaining != null) {
                AIQuotaBanner(
                    remaining = remaining,
                    onUpgrade = {
                        FeedbackManager.shared.playSelection()
                        showPaywall = true
                        onOpenPaywall?.invoke()
                    }
                )
                HorizontalDivider(color = Theme.ink.copy(alpha = 0.2f), thickness = 1.dp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val inputShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(inputShape)
                        .background(Theme.paper, inputShape)
                        .border(1.5.dp, Theme.ink, inputShape)
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = L10n.chatInputPlaceholder,
                            fontSize = 15.sp,
                            color = Theme.secondaryInk
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = Theme.ink
                        ),
                        cursorBrush = SolidColor(Theme.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { handleSend() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val isSendEnabled = inputText.isNotBlank()
                IconButton(
                    onClick = handleSend,
                    enabled = isSendEnabled,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowCircleUp,
                        contentDescription = null,
                        tint = if (isSendEnabled) Theme.accent else Theme.secondaryInk,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }

    if (showPaywall) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPaywall = false },
            sheetState = sheetState,
            containerColor = Theme.paper
        ) {
            ChatPaywallSheetContent(
                onDismiss = { showPaywall = false }
            )
        }
    }
}

/**
 * Mesaj balonu yönlendiricisi (`Views/Chat/ChatView.swift` -> `MessageBubble` karşılığı).
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    onRetry: () -> Void = {},
    modifier: Modifier = Modifier
) {
    when {
        message.isError -> ErrorBubble(
            message = message,
            onRetry = onRetry,
            modifier = modifier
        )
        message.isUser -> UserBubble(
            message = message,
            modifier = modifier
        )
        else -> BotBubble(
            message = message,
            modifier = modifier
        )
    }
}

/**
 * Kullanıcı mesaj balonu (`UserBubble` karşılığı).
 */
@Composable
fun UserBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Spacer(modifier = Modifier.width(40.dp))
        Box(
            modifier = Modifier
                .clip(shape)
                .background(Theme.accent, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 16.sp,
                color = Theme.paper
            )
        }
    }
}

/**
 * AI Dil Partneri mesaj balonu (`BotBubble` karşılığı).
 * Japonca metni `FuriganaText` ile gösterir, yanında sesli dinleme (`AudioService.speak`) butonu
 * ve altında parantez içinden ayrıştırılan Türkçe çeviriyi sunar.
 */
@Composable
fun BotBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val parsed = remember(message.text) { ChatViewModel.parseBotMessage(message.text) }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(shape)
                .background(Theme.paper, shape)
                .border(1.5.dp, Theme.ink, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FuriganaText(
                    text = parsed.japanese,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
                    readingStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                    color = Theme.ink,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Theme.accent.copy(alpha = 0.12f))
                        .clickable {
                            AudioService.shared.speak(parsed.japanese)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = L10n.listenButton,
                        tint = Theme.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (parsed.translation != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = Theme.ink.copy(alpha = 0.12f))
                    Text(
                        text = parsed.translation,
                        fontSize = 14.sp,
                        color = Theme.secondaryInk
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(40.dp))
    }
}

/**
 * Hata mesajı balonu ve "Tekrar Dene" aksiyonu (`ErrorBubble` karşılığı).
 */
@Composable
fun ErrorBubble(
    message: ChatMessage,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(shape)
                .background(Theme.paper, shape)
                .border(1.5.dp, Theme.accent.copy(alpha = 0.7f), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = Theme.ink
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Theme.accent.copy(alpha = 0.12f))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = L10n.retry,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.accent
                )
            }
        }

        Spacer(modifier = Modifier.width(40.dp))
    }
}

/**
 * 3 noktalı zıplayan yazıyor animasyonu (`Views/Chat/ChatView.swift` -> `TypingIndicator` karşılığı).
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "TypingTransition")

    val offset1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Dot1"
    )
    val offset2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(150)
        ),
        label = "Dot2"
    )
    val offset3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(300)
        ),
        label = "Dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = offset1.dp)
                .clip(CircleShape)
                .background(Theme.secondaryInk)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = offset2.dp)
                .clip(CircleShape)
                .background(Theme.secondaryInk)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = offset3.dp)
                .clip(CircleShape)
                .background(Theme.secondaryInk)
        )
    }
}

/**
 * Günlük ücretsiz mesaj kotası dolduğunda veya `AIQuotaBanner` üzerindeki `PRO` rozetine
 * dokunulduğunda gösterilen Kaimon Pro Paywall önizleme sayfası (`PaywallView.swift` uyumlu).
 */
@Composable
private fun ChatPaywallSheetContent(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.paper)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "開門",
                    style = Theme.display(44.sp),
                    color = Theme.accent
                )
                Text(
                    text = L10n.proName.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Theme.secondaryInk
                )
                Text(
                    text = L10n.paywallHeadline,
                    style = Theme.display(30.sp),
                    color = Theme.ink
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Theme.paper, CircleShape)
                    .border(2.dp, Theme.ink, CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = L10n.closeButton,
                    tint = Theme.ink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PaywallBenefitRow(
                icon = Icons.Filled.LibraryBooks,
                text = L10n.paywallBenefitContent
            )
            PaywallBenefitRow(
                icon = Icons.Filled.Forum,
                text = L10n.paywallBenefitAI
            )
            PaywallBenefitRow(
                icon = Icons.Filled.Block,
                text = L10n.paywallBenefitNoAds
            )
        }

        PrimaryButton(
            text = L10n.subscribeButton,
            onClick = onDismiss
        )
    }
}

@Composable
private fun PaywallBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Theme.accent,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Theme.ink
        )
    }
}

private typealias Void = Unit
