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
-include ../proguard-zendesk.txt

# Google Play Services
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

# v7 support library
-keep public class android.support.v7.widget.SearchView { *; }

# Crashylytics Reuirement
-keepattributes *Annotation*


# Fix http lib issues.
-keep class org.apache.http.** { *; }
-dontwarn com.android.volley.**
-dontwarn com.google.android.gms.**
-dontwarn com.zendesk.sdk.**

# Glide
-keep class com.bumptech.glide.integration.volley.VolleyGlideModule
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Default
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.android.gms.**
-dontwarn com.android.volley.toolbox.**

# Crashylytics
-keepattributes SourceFile,LineNumberTable

-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**