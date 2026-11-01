import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val sharedResourcesDir = rootProject.projectDir.resolve("../Nihongo_app/Nihongo_app/Resources").canonicalFile

android {
    namespace = "com.batuhankeskn0.kaimon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.batuhankeskn0.kaimon"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
