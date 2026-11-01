package com.batuhankeskn0.kaimon.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alt ekranlara (ör. Flashcard oturumu, Hikaye okuyucu, AI Sohbet) girildiğinde
 * yüzen alt sekme çubuğunu (`FloatingTabBar`) animasyonlu gizleyip gösteren paylaşımlı durum yöneticisi.
 * iOS `TabBarManager.swift` karşılığıdır.
 */
@Singleton
class TabBarManager @Inject constructor() {

    private var _isHiddenCompose by mutableStateOf(false)
    private val _isHiddenFlow = MutableStateFlow(false)

    /** ViewModel ve Coroutine katmanları için reaktif görünürlük akışı. */
    val isHiddenFlow: StateFlow<Boolean> = _isHiddenFlow.asStateFlow()

    /**
     * Sekme çubuğunun gizli olup olmadığını belirtir.
     * `@Composable` içinden okunduğunda otomatik recomposition ve `AnimatedVisibility` tetikler.
     */
    var isHidden: Boolean
        get() = _isHiddenCompose
        set(value) {
            _isHiddenCompose = value
            _isHiddenFlow.value = value
            if (this !== sharedInstance) {
                sharedInstance._isHiddenCompose = value
                sharedInstance._isHiddenFlow.value = value
            }
        }

    fun hide() {
        isHidden = true
    }

    fun show() {
        isHidden = false
    }

    companion object {
        @Volatile
        private var sharedInstance: TabBarManager = TabBarManager()

        val shared: TabBarManager
            get() = sharedInstance
    }
}

val LocalTabBarManager = staticCompositionLocalOf { TabBarManager.shared }
