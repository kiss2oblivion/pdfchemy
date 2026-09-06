package com.pdfchemy.desktop.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.prefs.Preferences

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val htmlUrl: String,
    val body: String,
    val publishedAt: String,
    val isNewer: Boolean
)

object DesktopUpdateManager {
    const val CURRENT_VERSION = "1.0.2"
    const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/kiss2oblivion/pdfchemy/releases/latest"
    const val GITHUB_RELEASES_WEB = "https://github.com/kiss2oblivion/pdfchemy/releases"

    private const val PREF_KEY_LAST_CHECK = "last_update_check_time"
    private const val PREF_KEY_DISMISSED_TAG = "dismissed_update_tag"

    private val prefs: Preferences by lazy {
        Preferences.userNodeForPackage(DesktopUpdateManager::class.java)
    }

    var dismissedTag: String?
        get() = prefs.get(PREF_KEY_DISMISSED_TAG, null)
        set(value) {
            try {
                if (value != null) {
                    prefs.put(PREF_KEY_DISMISSED_TAG, value)
                } else {
                    prefs.remove(PREF_KEY_DISMISSED_TAG)
                }
                prefs.flush()
            } catch (_: Exception) {}
        }

    fun isVersionNewer(remoteTag: String, current: String = CURRENT_VERSION): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V").split("-")[0]
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V").split("-")[0]

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    suspend fun checkForUpdates(
        currentVersion: String = CURRENT_VERSION,
        timeoutMs: Int = 6000
    ): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URI(GITHUB_LATEST_RELEASE_API).toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "PDFchemy-Desktop/$CURRENT_VERSION")

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("GitHub API returned HTTP $responseCode")
                )
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            val json = reader.use { it.readText() }

            val tagName = extractJsonField(json, "tag_name") ?: "v$currentVersion"
            val name = extractJsonField(json, "name") ?: "PDFchemy $tagName"
            val htmlUrl = extractJsonField(json, "html_url") ?: "$GITHUB_RELEASES_WEB/tag/$tagName"
            val publishedAt = extractJsonField(json, "published_at")?.take(10) ?: ""
            val body = extractJsonBody(json) ?: ""

            val newer = isVersionNewer(tagName, currentVersion)

            // Record check timestamp
            try {
                prefs.putLong(PREF_KEY_LAST_CHECK, System.currentTimeMillis())
                prefs.flush()
            } catch (_: Exception) {}

            Result.success(
                ReleaseInfo(
                    tagName = tagName,
                    name = name,
                    htmlUrl = htmlUrl,
                    body = body,
                    publishedAt = publishedAt,
                    isNewer = newer
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJsonField(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }

    private fun extractJsonBody(json: String): String? {
        val keyIdx = json.indexOf("\"body\"")
        if (keyIdx == -1) return null
        val colonIdx = json.indexOf(':', keyIdx)
        if (colonIdx == -1) return null
        val startQuote = json.indexOf('"', colonIdx)
        if (startQuote == -1) return null

        val sb = StringBuilder()
        var escaped = false
        var i = startQuote + 1
        while (i < json.length) {
            val c = json[i]
            if (escaped) {
                when (c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(c)
                }
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}
