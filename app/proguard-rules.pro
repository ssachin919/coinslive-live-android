# CoinsLive Live — release shrinker rules

# --- LiveKit / WebRTC ---
-keep class io.livekit.** { *; }
-keep class livekit.** { *; }
-keep class org.webrtc.** { *; }
-keep class com.twilio.audioswitch.** { *; }
-keep class androidx.camera.** { *; }
# JNI stubs referenced by LiveKit WebRTC packaging (not always on the compile classpath).
-dontwarn livekit.org.jni_zero.**
-dontwarn livekit.org.**
-dontwarn io.livekit.**
-dontwarn org.webrtc.**
-dontwarn tvi.webrtc.**
-dontwarn androidx.camera.**

# LiveKit UniFFI / JNA (data tracks); keep reflective Structure subclasses.
-dontwarn java.awt.**
-keep class com.sun.jna.** { *; }
-keep class * extends com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }

# Preserve WebRTC native bridges (LiveKit-packaged package).
-keepclasseswithmembernames,includedescriptorclasses class livekit.org.webrtc.** {
    native <methods>;
}
-keepclasseswithmembers,includedescriptorclasses class * {
    @livekit.**.CalledByNative <methods>;
}
-keepclasseswithmembers,includedescriptorclasses class * {
    @livekit.**.CalledByNativeUnchecked <methods>;
}

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
