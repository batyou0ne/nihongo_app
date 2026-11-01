package com.batuhankeskn0.kaimon

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Kaimon (開門) Android Uygulama Giriş Noktası.
 * Hilt bağımlılık enjeksiyonu grafiğini başlatır.
 */
@HiltAndroidApp
class KaimonApplication : Application()
