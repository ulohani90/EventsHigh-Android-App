# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/paragsarda/src/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-include ../proguard-com.twitter.sdk.android.twitter.txt

# Flurry
# https://github.com/krschultz/android-proguard-snippets/blob/master/libraries/proguard-flurry.pro
-keep class com.flurry.** { *; }
-keep interface com.flurry.** { *; }
-keep enum com.flurry.** { *; }
-dontwarn com.flurry.**
-keepattributes *Annotation*,EnclosingMethod
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int); 
}

# Amplitude
-keep public class com.google.android.gms.ads.** { public protected *; } 

