# ==============================================================================
# Kaimon (開門) R8 / ProGuard Kuralları
# ==============================================================================

# 1. Genel Özellikler & Annotations
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, SourceFile, LineNumberTable
-dontnote kotlinx.serialization.AnnotationsKt

# 2. Kotlinx Serialization
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# 3. Kaimon Model & Entity Sınıfları (Room & Domain)
-keep class com.batuhankeskn0.kaimon.data.model.** { *; }
-keep class com.batuhankeskn0.kaimon.data.local.** { *; }
-keep class com.batuhankeskn0.kaimon.data.service.UserProfile { *; }
-keep class com.batuhankeskn0.kaimon.data.service.AuthUserInfo { *; }
-keep class com.batuhankeskn0.kaimon.data.service.PlanOffer { *; }

# 4. Room Database & Converters
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 5. Kuromoji Sözlük ve Morfolojik Analiz Sınıfları
-dontwarn com.atilika.kuromoji.**
-keep class com.atilika.kuromoji.** { *; }
-keepclassmembers class com.atilika.kuromoji.** { *; }

# 6. Firebase (Auth, Firestore, Common)
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }

# 7. Google Play Billing Library v7
-dontwarn com.android.billingclient.**
-keep class com.android.billingclient.** { *; }
-keepclassmembers class com.android.billingclient.** { *; }

# 8. Google Mobile Ads & UMP (User Messaging Platform)
-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-keep public class com.google.android.gms.ads.internal.ClientApi { *; }

# 9. AndroidX Credential Manager & Google ID
-dontwarn androidx.credentials.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# 10. Hilt & Dependency Injection
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
