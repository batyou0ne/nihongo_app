package com.batuhankeskn0.kaimon.data.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dokunsal (Haptic) geri bildirimleri yöneten merkezi servis.
 * iOS `FeedbackManager.swift` karşılığıdır:
 * - `playSuccess()`: Doğru cevap verildiğinde orta şiddette net titreşim.
 * - `playError()`: Yanlış cevap verildiğinde çift vuruşlu hata titreşimi.
 * - `playSelection()`: Harf/sekme seçildiğinde hafif dokunma geri bildirimi.
 */
@Singleton
class FeedbackManager private constructor() {

    private var appContext: Context? = null

    @Inject
    constructor(@ApplicationContext context: Context) : this() {
        attachContext(context)
        sharedInstance = this
    }

    internal fun attachContext(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * Doğru cevap verildiğinde çalınacak başarı geri bildirimi (`.medium` impact karşılığı).
     */
    fun playSuccess() {
        vibrate { vibrator ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(25L, 150))
            }
        }
    }

    /**
     * Yanlış cevap verildiğinde çalınacak hata geri bildirimi (`.error` notification karşılığı).
     */
    fun playError() {
        vibrate { vibrator ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0L, 40L, 60L, 60L),
                        intArrayOf(0, 200, 0, 255),
                        -1
                    )
                )
            }
        }
    }

    /**
     * Butona veya alfabe harfine basma hissiyatı veren hafif geri bildirim (`selectionChanged` / `.light` karşılığı).
     */
    fun playSelection() {
        vibrate { vibrator ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(12L, 80))
            }
        }
    }

    private fun vibrate(block: (Vibrator) -> Unit) {
        val ctx = appContext ?: sharedInstance.appContext ?: return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                block(vibrator)
            }
        }
    }

    companion object {
        @Volatile
        private var sharedInstance: FeedbackManager = FeedbackManager()

        val shared: FeedbackManager
            get() = sharedInstance

        fun init(context: Context) {
            sharedInstance.attachContext(context)
        }
    }
}
