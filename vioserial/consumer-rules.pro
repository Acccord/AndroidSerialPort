# Consumer ProGuard rules for serial.
# Keep JNI entry points and public API surface.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.temon.serial.internal.serialport.** { *; }
-keep class com.temon.serial.** { *; }

