# PixelAppLock ProGuard Rules
-keepattributes *Annotation*
-dontwarn java.lang.invoke.**
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
