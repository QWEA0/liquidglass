# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========================================
# Native Library - JNI 保护规则
# ========================================

# 保护 NativeGauss 类及其所有 native 方法
-keep class com.example.blur.NativeGauss {
    native <methods>;
    public <methods>;
}

# 保护 NativeChromaticAberration 类及其所有 native 方法
-keep class com.example.liquidglass.NativeChromaticAberration {
    native <methods>;
    public <methods>;
}

# 保护所有 native 方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保护 Bitmap 相关类（JNI 中使用）
-keep class android.graphics.Bitmap {
    *;
}