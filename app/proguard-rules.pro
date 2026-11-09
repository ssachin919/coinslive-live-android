# CoinsLive Live — release shrinker rules

# --- LiveKit / WebRTC ---
-keep class io.livekit.** { *; }
-keep class livekit.** { *; }
-keep class org.webrtc.** { *; }
-keep class com.twilio.audioswitch.** { *; }
# JNI stubs referenced by LiveKit WebRTC packaging (not always on the compile classpath).
-dontwarn livekit.org.jni_zero.**
-dontwarn livekit.org.**
-dontwarn io.livekit.**
-dontwarn org.webrtc.**
-dontwarn tvi.webrtc.**

# Protobuf (used by the LiveKit signalling layer)
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# --- Clerk ---
-keep class com.clerk.** { *; }
-dontwarn com.clerk.**

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

-keep,includedescriptorclasses class in.coinslive.live.**$$serializer { *; }
-keepclassmembers class in.coinslive.live.** {
    *** Companion;
}
-keepclasseswithmembers class in.coinslive.live.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- OkHttp / Okio ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep line numbers for crash reports
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
