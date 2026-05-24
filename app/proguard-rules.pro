# Keep Moshi reflection
-keep class kotlin.Metadata { *; }
-keep class com.iurie.etfwatch.data.remote.** { *; }
-keep class com.iurie.etfwatch.data.db.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
