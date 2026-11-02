import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

/**
 * `google-services.json` yalnızca mevcutsa (Firebase Console'dan indirilip `app/` altına konduysa)
 * `google-services` eklentisi etkinleşir. Dosya yoksa derleme kırılmaz; `AuthService`/`UserProfileService`
 * çalışma zamanında `FirebaseApp` yokluğunu algılayıp yerel (offline) moda düşer.
 */
val hasGoogleServicesConfig: Boolean = file("google-services.json").exists()
if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.lifecycle("ℹ︎ [firebase] app/google-services.json bulunamadı; google-services eklentisi atlandı (yerel mod).")
}

val sharedResourcesDir = rootProject.projectDir.resolve("../Nihongo_app/Nihongo_app/Resources").canonicalFile

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun resolveSecret(key: String, defaultValue: String = ""): String {
    val fromProps = localProperties.getProperty(key)?.trim()
    if (!fromProps.isNullOrEmpty()) return fromProps
    val fromEnv = System.getenv(key)?.trim()
    if (!fromEnv.isNullOrEmpty()) return fromEnv
    return defaultValue
}

fun escapeBuildConfigString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.batuhankeskn0.kaimon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.batuhankeskn0.kaimon"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        val geminiApiKey = resolveSecret("GEMINI_API_KEY", resolveSecret("AI_API_KEY", ""))
        val openAiApiKey = resolveSecret("OPENAI_API_KEY", "")
        val aiProvider = resolveSecret("AI_PROVIDER", "gemini")

        buildConfigField("String", "GEMINI_API_KEY", "\"${escapeBuildConfigString(geminiApiKey)}\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"${escapeBuildConfigString(openAiApiKey)}\"")
        buildConfigField("String", "AI_PROVIDER", "\"${escapeBuildConfigString(aiProvider)}\"")

        // AdMob: varsayılanlar Google'ın resmi TEST kimlikleridir. Canlı kimlikler `local.properties` / ortam
        // değişkeninden (ADMOB_APP_ID, ADMOB_BANNER_ID, ADMOB_INTERSTITIAL_ID, ADMOB_REWARDED_ID) okunur; repoya girmez.
        val admobAppId = resolveSecret("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        val admobBannerId = resolveSecret("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/9214589741")
        val admobInterstitialId = resolveSecret("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")
        val admobRewardedId = resolveSecret("ADMOB_REWARDED_ID", "ca-app-pub-3940256099942544/5224354917")
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${escapeBuildConfigString(admobBannerId)}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${escapeBuildConfigString(admobInterstitialId)}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${escapeBuildConfigString(admobRewardedId)}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release imzası: `KAIMON_KEYSTORE_PATH/PASSWORD`, `KAIMON_KEY_ALIAS/PASSWORD` tanımlıysa kullanılır;
    // aksi halde `assembleRelease` imzasız APK üretir (CI/yerel doğrulama için derlemeyi kırmaz).
    val releaseKeystorePath = resolveSecret("KAIMON_KEYSTORE_PATH")
    val hasReleaseSigning = releaseKeystorePath.isNotEmpty() && file(releaseKeystorePath).exists()
    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = resolveSecret("KAIMON_KEYSTORE_PASSWORD")
                keyAlias = resolveSecret("KAIMON_KEY_ALIAS")
                keyPassword = resolveSecret("KAIMON_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            // Single Source of Truth: iOS Resources klasörünü kopyalamadan doğrudan Android AssetManager'a bağlar.
            assets.srcDirs("src/main/assets", sharedResourcesDir.path)
        }
    }

    androidResources {
        noCompress += listOf("mp3")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/CONTRIBUTORS.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

/**
 * Single Source of Truth Asset Doğrulama ve Senkronizasyon Görevi.
 * Dosyaları kopyalayarak repo boyutunu ikiye katlamak yerine `sourceSets["main"].assets.srcDirs`
 * üzerinden doğrudan bağlanan ortak `Nihongo_app/Nihongo_app/Resources` dizinindeki
 * 28 JSON dosyasını ve `Audio/audio_manifest.json` varlığını derleme öncesi (`preBuild`) doğrular.
 */
val syncSharedAssets by tasks.registering {
    group = "kaimon"
    description = "Validates zero-copy Single Source of Truth shared assets from Nihongo_app/Nihongo_app/Resources."
    inputs.dir(sharedResourcesDir)

    doLast {
        if (!sharedResourcesDir.exists()) {
            throw GradleException("Ortak Resources dizini bulunamadı: ${sharedResourcesDir.absolutePath}")
        }
        val jsonFiles = sharedResourcesDir.listFiles { file ->
            file.isFile && file.extension.equals("json", ignoreCase = true)
        }.orEmpty()
        val audioManifest = sharedResourcesDir.resolve("Audio/audio_manifest.json")

        if (jsonFiles.size < 28 || !audioManifest.exists()) {
            throw GradleException(
                "Ortak asset doğrulaması başarısız! JSON sayısı: ${jsonFiles.size} (beklenen: 28), " +
                    "audio_manifest.json mevcut: ${audioManifest.exists()}"
            )
        }
        logger.lifecycle(
            "✓ [syncSharedAssets] ${jsonFiles.size} JSON ve Audio/ klasörü sıfır kopya ile bağlandı: ${sharedResourcesDir.path}"
        )
    }
}

tasks.named("preBuild") {
    dependsOn(syncSharedAssets)
}

dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Serialization & Coroutines
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Firebase (Auth + Firestore) & Credential Manager (Google ile Giriş)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Google Play Billing v7 (Pro abonelik) & AdMob + UMP
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.ump)

    // Kuromoji (Auto-Furigana)
    implementation(libs.kuromoji.ipadic)

    // Unit & Instrumentation Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
