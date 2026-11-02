package com.batuhankeskn0.kaimon.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.service.AuthService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.data.service.UserProfile
import com.batuhankeskn0.kaimon.data.service.UserProfileService
import com.batuhankeskn0.kaimon.ui.components.findActivity
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.launch

private enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}

/**
 * Hesap oluşturma / Giriş yapma ekranı.
 * iOS `Views/Auth/SignInView.swift` bileşeninin birebir Jetpack Compose karşılığıdır.
 *
 * - Misafir kullanımını engellemez; kullanıcı isterse Google veya Email/Şifre ile hesap bağlayıp
 *   ilerlemesini buluta taşır.
 * - Anonim hesap yeni kimliğe "link" edildiği için mevcut `uid` ve ilerleme korunur.
 */
@Composable
fun SignInScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSignedIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findActivity() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var ageText by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var infoMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isWorking by rememberSaveable { mutableStateOf(false) }

    val isFirebaseReady = AuthService.shared.isFirebaseAvailable

    fun submitEmailPassword() {
        focusManager.clearFocus()
        errorMessage = null
        infoMessage = null

        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
            errorMessage = L10n.emptyEmailPassword
            return
        }

        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()

        if (mode == AuthMode.SIGN_UP) {
            if (trimmedFirstName.isEmpty() || trimmedLastName.isEmpty()) {
                errorMessage = L10n.emptyName
                return
            }
        }

        isWorking = true
        scope.launch {
            try {
                if (mode == AuthMode.SIGN_UP) {
                    val displayName = "$trimmedFirstName $trimmedLastName".trim()
                    AuthService.shared.signUp(
                        email = trimmedEmail,
                        password = trimmedPassword,
                        displayName = displayName
                    )
                    val profile = UserProfile(
                        firstName = trimmedFirstName,
                        lastName = trimmedLastName,
                        age = ageText.trim().toIntOrNull()
                    )
                    UserProfileService.shared.save(profile)
                } else {
                    AuthService.shared.signIn(
                        email = trimmedEmail,
                        password = trimmedPassword
                    )
                }
                isWorking = false
                FeedbackManager.shared.playSuccess()
                onSignedIn()
                onDismiss()
            } catch (e: Throwable) {
                isWorking = false
                errorMessage = AuthService.friendlyMessage(e)
                FeedbackManager.shared.playError()
            }
        }
    }

    fun submitGoogleSignIn() {
        focusManager.clearFocus()
        errorMessage = null
        infoMessage = null

        val activity = hostActivity
        if (activity == null) {
            errorMessage = L10n.authMissingGoogleConfig
            return
        }

        isWorking = true
        scope.launch {
            try {
                val didSignIn = AuthService.shared.signInWithGoogle(activity)
                isWorking = false
                if (didSignIn) {
                    FeedbackManager.shared.playSuccess()
                    onSignedIn()
                    onDismiss()
                }
            } catch (e: Throwable) {
                isWorking = false
                errorMessage = AuthService.friendlyMessage(e)
                FeedbackManager.shared.playError()
            }
        }
    }

    fun sendPasswordReset() {
        focusManager.clearFocus()
        errorMessage = null
        infoMessage = null

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            errorMessage = L10n.passwordResetPrompt
            return
        }

        isWorking = true
        scope.launch {
            try {
                AuthService.shared.sendPasswordReset(trimmedEmail)
                isWorking = false
                infoMessage = L10n.passwordResetSent(trimmedEmail)
                FeedbackManager.shared.playSelection()
            } catch (e: Throwable) {
                isWorking = false
                errorMessage = AuthService.friendlyMessage(e)
                FeedbackManager.shared.playError()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 32.dp)
    ) {
        // Kapat (xmark) butonu
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    FeedbackManager.shared.playSelection()
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = L10n.closeButton,
                tint = Theme.ink,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Kanji Başlık (開門)
        Text(
            text = "開門",
            style = Theme.display(48.sp),
            color = Theme.accent
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Ekran Başlığı (Giriş yap / Hesap oluştur)
        Text(
            text = if (mode == AuthMode.SIGN_UP) L10n.signUpTitle else L10n.signInTitle,
            style = Theme.display(30.sp),
            color = Theme.ink
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Açıklama
        Text(
            text = L10n.signInDescription,
            fontSize = 15.sp,
            color = Theme.secondaryInk,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isFirebaseReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered(lineWidth = 1.5.dp)
                    .background(Theme.paper)
                    .padding(14.dp)
            ) {
                Text(
                    text = L10n.authFirebaseUnavailable,
                    fontSize = 13.sp,
                    color = Theme.secondaryInk
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Email / Şifre Formu
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (mode == AuthMode.SIGN_UP) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KaimonTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        placeholder = L10n.firstNamePlaceholder,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    KaimonTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        placeholder = L10n.lastNamePlaceholder,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                KaimonTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { char -> char.isDigit() } },
                    placeholder = L10n.agePlaceholder,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
            }

            KaimonTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = L10n.emailPlaceholder,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            KaimonTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = L10n.passwordPlaceholder,
                isPassword = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitEmailPassword() })
            )

            // Hata / Bilgilendirme Mesajları
            errorMessage?.let { error ->
                Text(
                    text = error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.accent
                )
            }

            infoMessage?.let { info ->
                Text(
                    text = info,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.ink
                )
            }

            // Gönder Butonu (Giriş Yap / Kayıt Ol)
            PrimaryButton(
                text = if (mode == AuthMode.SIGN_UP) L10n.signUpButton else L10n.signInButton,
                onClick = { submitEmailPassword() },
                enabled = !isWorking && isFirebaseReady,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Mod Değiştirme & Şifremi Unuttum
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == AuthMode.SIGN_UP) L10n.alreadyHaveAccount else L10n.noAccount,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.ink,
                    modifier = Modifier.clickable {
                        FeedbackManager.shared.playSelection()
                        mode = if (mode == AuthMode.SIGN_UP) AuthMode.SIGN_IN else AuthMode.SIGN_UP
                        errorMessage = null
                        infoMessage = null
                    }
                )

                if (mode == AuthMode.SIGN_IN) {
                    Text(
                        text = L10n.forgotPassword,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.secondaryInk,
                        modifier = Modifier.clickable {
                            FeedbackManager.shared.playSelection()
                            sendPasswordReset()
                        }
                    )
                }
            }
        }

        // Ayırıcı ("veya")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(Theme.ink)
            )
            Text(
                text = L10n.orDivider,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(Theme.ink)
            )
        }

        // Google ile Giriş Butonu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .inkBordered(lineWidth = 2.dp)
                .clickable(enabled = !isWorking && isFirebaseReady) {
                    FeedbackManager.shared.playSelection()
                    submitGoogleSignIn()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isWorking) {
                CircularProgressIndicator(
                    color = Theme.ink,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "G",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Theme.accent
                    )
                    Text(
                        text = L10n.googleSignIn,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.ink
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Misafir Olarak Devam Et
        Text(
            text = L10n.continueAsGuest,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Theme.accent,
            modifier = Modifier
                .clickable {
                    FeedbackManager.shared.playSelection()
                    onDismiss()
                }
                .padding(vertical = 8.dp)
        )
    }
}

/**
 * Kaimon Brutalist Metin Giriş Kutusu.
 */
@Composable
private fun KaimonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val shape = RoundedCornerShape(16.dp)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Theme.secondaryInk.copy(alpha = 0.8f),
                fontSize = 15.sp
            )
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Theme.paper,
            unfocusedContainerColor = Theme.paper,
            disabledContainerColor = Theme.paper,
            focusedTextColor = Theme.ink,
            unfocusedTextColor = Theme.ink,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = Theme.accent
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 2.dp, color = Theme.ink, shape = shape)
    )
}
