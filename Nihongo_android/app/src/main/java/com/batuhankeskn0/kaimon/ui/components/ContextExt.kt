package com.batuhankeskn0.kaimon.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Compose `LocalContext` zincirini (ContextWrapper) açarak ana `Activity`'yi bulur.
 * Billing, Credential Manager ve reklam akışları `Activity` gerektirir; ViewModel'lere saklanmadan parametre olarak geçirilir.
 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
