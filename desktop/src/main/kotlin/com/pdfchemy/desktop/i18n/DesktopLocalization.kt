package com.pdfchemy.desktop.i18n

import androidx.compose.runtime.mutableStateOf
import java.util.Locale
import java.util.prefs.Preferences

/**
 * Supported desktop languages with full parity to the 20 Android languages (21 locales).
 */
enum class DesktopLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean = false
) {
    EN("en", "English", "English"),
    DE("de", "Deutsch", "German"),
    ES("es", "Español", "Spanish"),
    FR("fr", "Français", "French"),
    RO("ro", "Română", "Romanian"),
    IT("it", "Italiano", "Italian"),
    PT("pt", "Português", "Portuguese"),
    PT_BR("pt-BR", "Português (Brasil)", "Portuguese (Brazil)"),
    PL("pl", "Polski", "Polish"),
    NL("nl", "Nederlands", "Dutch"),
    RU("ru", "Русский", "Russian"),
    TR("tr", "Türkçe", "Turkish"),
    AR("ar", "العربية", "Arabic", isRtl = true),
    HI("hi", "हिन्दी", "Hindi"),
    ID("id", "Bahasa Indonesia", "Indonesian"),
    JA("ja", "日本語", "Japanese"),
    KO("ko", "한국어", "Korean"),
    TH("th", "ไทย", "Thai"),
    VI("vi", "Tiếng Việt", "Vietnamese"),
    ZH_CN("zh-CN", "简体中文", "Chinese (Simplified)"),
    ZH_TW("zh-TW", "繁體中文", "Chinese (Traditional)");

    companion object {
        fun fromCode(code: String?): DesktopLanguage {
            if (code.isNullOrBlank()) return EN
            val normalized = code.trim().replace('_', '-')
            return entries.firstOrNull { it.code.equals(normalized, ignoreCase = true) }
                ?: entries.firstOrNull { it.code.startsWith(normalized.split("-")[0], ignoreCase = true) }
                ?: EN
        }

        fun detectSystemLanguage(): DesktopLanguage {
            val loc = Locale.getDefault()
            val tag = loc.toLanguageTag()
            val lang = loc.language.lowercase()
            val country = loc.country.uppercase()

            if (lang == "pt" && country == "BR") return PT_BR
            if (lang == "zh") {
                if (country == "TW" || country == "HK" || tag.contains("Hant", ignoreCase = true)) return ZH_TW
                return ZH_CN
            }
            if (lang == "in") return ID // Android/Java legacy code for Indonesian
            return entries.firstOrNull { it.code.equals(lang, ignoreCase = true) } ?: EN
        }
    }
}

/**
 * State and lifecycle manager for desktop localization.
 * Backed by Compose mutableStateOf for instant reactive UI updates.
 */
object DesktopLocalization {
    private const val PREF_KEY_LANG = "desktop_language"
    private const val PREF_KEY_FIRST_RUN_DONE = "desktop_setup_completed"

    private val prefs: Preferences by lazy {
        Preferences.userNodeForPackage(DesktopLocalization::class.java)
    }

    val currentLanguageState = mutableStateOf(resolveInitialLanguage())

    var currentLanguage: DesktopLanguage
        get() = currentLanguageState.value
        set(value) {
            currentLanguageState.value = value
            try {
                prefs.put(PREF_KEY_LANG, value.code)
                prefs.putBoolean(PREF_KEY_FIRST_RUN_DONE, true)
                prefs.flush()
            } catch (_: Exception) {}
        }

    val isFirstRun: Boolean
        get() = !prefs.getBoolean(PREF_KEY_FIRST_RUN_DONE, false)

    var cliOverrideActive: Boolean = false
        private set

    /**
     * Initializes localization from CLI arguments or environment.
     * Returns true if the first-run installation/setup dialog should be shown.
     */
    fun initFromCli(cliLang: String?, forceSetup: Boolean = false): Boolean {
        if (!cliLang.isNullOrBlank()) {
            val lang = DesktopLanguage.fromCode(cliLang)
            currentLanguageState.value = lang
            cliOverrideActive = true
            return false
        }
        if (forceSetup) {
            return true
        }
        return isFirstRun
    }

    fun completeSetup(language: DesktopLanguage) {
        currentLanguage = language
    }

    private fun resolveInitialLanguage(): DesktopLanguage {
        // 1. JVM system property (e.g. -Dpdfchemy.lang=de)
        val sysProp = System.getProperty("pdfchemy.lang")
        if (!sysProp.isNullOrBlank()) {
            return DesktopLanguage.fromCode(sysProp)
        }
        // 2. Persisted user preference
        try {
            val saved = prefs.get(PREF_KEY_LANG, null)
            if (!saved.isNullOrBlank()) {
                return DesktopLanguage.fromCode(saved)
            }
        } catch (_: Exception) {}
        // 3. Dynamic OS detection (Linux $LANG / Windows default locale)
        return DesktopLanguage.detectSystemLanguage()
    }

    val strings: DesktopStrings
        get() = DesktopStringStore.getStrings(currentLanguage)
}
