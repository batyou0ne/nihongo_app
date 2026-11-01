# Add project specific ProGuard rules here.
# Keep Kotlinx Serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Kuromoji dictionary resources
-dontwarn com.atilika.kuromoji.**
-keep class com.atilika.kuromoji.** { *; }
