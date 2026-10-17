# ============================================
# Gson
# ============================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================
# OkHttp
# ============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================
# Room
# ============================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements *Dao { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ============================================
# Модели данных
# ============================================
-keep class com.example.advancedrussian_hebrewdict.data.Word { *; }
-keep class com.example.advancedrussian_hebrewdict.data.Folder { *; }
-keep class com.example.advancedrussian_hebrewdict.data.FolderWithCount { *; }
-keep class com.example.advancedrussian_hebrewdict.AzureDictionaryHelper$DictionaryEntry { *; }

# ============================================
# BuildConfig (страховка для доступа к ключам)
# ============================================
-keep class com.example.advancedrussian_hebrewdict.BuildConfig { *; }

# ============================================
# Отладочная информация
# ============================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile