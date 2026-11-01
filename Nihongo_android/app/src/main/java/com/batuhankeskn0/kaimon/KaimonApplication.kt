package com.batuhankeskn0.kaimon

import android.app.Application
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import dagger.hilt.android.HiltAndroidApp

/**
 * Kaimon (開門) Android Uygulama Giriş Noktası.
 * Hilt bağımlılık enjeksiyonu grafiğini ve paylaşılan servisleri başlatır.
 */
@HiltAndroidApp
class KaimonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageManager.init(this)
        ContentStore.init(this)
        UserDatabaseManager.init(this)
        FeatureGate.init(this)
        FeedbackManager.init(this)
        AudioService.init(this)
    }
}
