# ProGuard configuration for PDFchemy Desktop

-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,Exceptions

# Keep Main Entry Point
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Keep Compose Multiplatform Desktop runtime
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.skiko.** { *; }
-keep class org.jetbrains.skia.** { *; }

# Keep PDFchemy Desktop application code
-keep class com.pdfchemy.desktop.** { *; }

# Keep Apache PDFBox & FontBox
-keep class org.apache.pdfbox.** { *; }
-keep class org.apache.fontbox.** { *; }

# Keep Document Libraries (Jackson, Jsoup, Flexmark)
-keep class com.fasterxml.jackson.** { *; }
-keep class org.jsoup.** { *; }
-keep class com.vladsch.flexmark.** { *; }

# Ignore unresolved references from optional dependencies
-dontwarn javax.annotation.**
-dontwarn org.apache.commons.logging.**
-dontwarn java.awt.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn com.ibm.icu.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.log4j.**
-dontwarn org.slf4j.**
-dontwarn net.sf.saxon.**

-dontobfuscate
-dontoptimize
