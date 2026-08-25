import os

file_path = "app/src/main/java/com/pdfchemy/app/MainActivity.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Force dark theme and consent
content = content.replace(
    'val hasConsentedInitial = prefs.getBoolean("has_consented", false)',
    'val hasConsentedInitial = true'
)
content = content.replace(
    'val isDarkThemeInitial = prefs.getBoolean("is_dark_theme", false)',
    'val isDarkThemeInitial = true'
)

# Disable UMP and AdMob by specifically targeting the exact block
block = """val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        if (consentInformation.canRequestAds()) {
            MobileAds.initialize(this) { }
        }

        val debugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("TEST-EMULATOR")
            .build()

        val params = ConsentRequestParameters.Builder()
            // Uncomment the next line to force the GDPR form for testing
            // .setConsentDebugSettings(debugSettings) 
            .build()

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this
                ) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w("UMP", "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                    }
                    if (consentInformation.canRequestAds()) {
                        MobileAds.initialize(this) { }
                    }
                }
            },
            { requestConsentError ->
                Log.w("UMP", "${requestConsentError.errorCode}: ${requestConsentError.message}")
            }
        )"""

if block in content:
    content = content.replace(block, '/* ' + block + ' */')
else:
    print("WARNING: Could not find exact UMP block!")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Patched correctly!")
