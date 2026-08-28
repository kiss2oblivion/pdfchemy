# Add project specific ProGuard rules here.

# PdfBox-Android optional dependencies
-dontwarn com.gemalto.jp2.**
-dontwarn javax.xml.stream.**
-dontwarn aQute.bnd.annotation.spi.**
-dontwarn org.codehaus.stax2.**

# Google Play Services Ads & UMP
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-keep interface com.google.android.ump.** { *; }
