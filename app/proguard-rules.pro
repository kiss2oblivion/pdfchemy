# Add project specific ProGuard rules here.

# PdfBox-Android optional dependencies
-dontwarn com.gemalto.jp2.**
-dontwarn javax.xml.stream.**
-dontwarn aQute.bnd.annotation.spi.**
-dontwarn org.codehaus.stax2.**

# Note: Google Mobile Ads and UMP libraries package their own consumer ProGuard/R8 rules.
# Blanket keep rules for com.google.android.gms.ads and com.google.android.ump have been removed
# to allow R8 to perform proper dead-code elimination and resource shrinking on release builds.

