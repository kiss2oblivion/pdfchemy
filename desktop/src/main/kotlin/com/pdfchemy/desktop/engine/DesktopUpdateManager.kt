package com.pdfchemy.desktop.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.Locale
import java.util.prefs.Preferences

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val htmlUrl: String,
    val body: String,
    val publishedAt: String,
    val isNewer: Boolean,
    val assets: List<ReleaseAsset> = emptyList(),
    val sha256SumsUrl: String? = null
)

object DesktopUpdateManager {
    const val CURRENT_VERSION = "1.0.4"
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

    /**
     * Strictly verifies that the URL uses HTTPS and points exclusively to GitHub official hosts.
     * Prevents SSRF, MitM, and malicious redirect attacks.
     */
    fun validateSecureGitHubUri(uri: URI) {
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "https") {
            throw SecurityException("Security violation: Only secure HTTPS connections are permitted ($uri)")
        }
        val host = uri.host?.lowercase(Locale.ROOT)
            ?: throw SecurityException("Security violation: Invalid host in URL ($uri)")

        val allowedHosts = listOf(
            "api.github.com",
            "github.com",
            "objects.githubusercontent.com",
            "raw.githubusercontent.com",
            "github-releases.githubusercontent.com"
        )
        val isAllowed = allowedHosts.any { allowed -> host == allowed || host.endsWith(".$allowed") }
        if (!isAllowed) {
            throw SecurityException("Security violation: Untrusted update host '$host'. Only GitHub official domains are permitted.")
        }
    }

    suspend fun checkForUpdates(
        currentVersion: String = CURRENT_VERSION,
        timeoutMs: Int = 8000
    ): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val uri = URI(GITHUB_LATEST_RELEASE_API)
            validateSecureGitHubUri(uri)

            val conn = uri.toURL().openConnection() as HttpURLConnection
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

            val parsedAssets = parseAssets(json)
            val sha256Url = parsedAssets.firstOrNull { it.name.equals("SHA256SUMS.txt", ignoreCase = true) }?.downloadUrl

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
                    isNewer = newer,
                    assets = parsedAssets,
                    sha256SumsUrl = sha256Url
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Finds the best installer asset corresponding to the current running OS architecture.
     */
    fun findBestAssetForCurrentPlatform(assets: List<ReleaseAsset>): ReleaseAsset? {
        val osName = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        return when {
            osName.contains("win") -> {
                // Windows: Prefer MSI installer, fallback to EXE installer, then portable JAR
                assets.firstOrNull { it.name.endsWith(".msi", ignoreCase = true) }
                    ?: assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) }
                    ?: assets.firstOrNull { it.name.contains("windows", ignoreCase = true) && it.name.endsWith(".jar", ignoreCase = true) }
                    ?: assets.firstOrNull { it.name.endsWith(".jar", ignoreCase = true) }
            }
            osName.contains("linux") -> {
                // Linux: Prefer DEB (Ubuntu/Debian/Mint), then RPM (Fedora/RHEL), then shell/jar
                assets.firstOrNull { it.name.endsWith(".deb", ignoreCase = true) }
                    ?: assets.firstOrNull { it.name.endsWith(".rpm", ignoreCase = true) }
                    ?: assets.firstOrNull { it.name.contains("linux", ignoreCase = true) && it.name.endsWith(".jar", ignoreCase = true) }
                    ?: assets.firstOrNull { it.name.endsWith(".jar", ignoreCase = true) }
            }
            else -> {
                // Fallback to standalone JAR
                assets.firstOrNull { it.name.endsWith(".jar", ignoreCase = true) }
            }
        }
    }

    /**
     * Downloads and parses official SHA256SUMS.txt from the release.
     * Maps filename -> expected SHA-256 hex string (lowercase).
     */
    suspend fun fetchSha256Checksums(sha256Url: String, timeoutMs: Int = 10000): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            var currentUrl = sha256Url
            var redirects = 0
            var text = ""

            while (redirects < 5) {
                val uri = URI(currentUrl)
                validateSecureGitHubUri(uri)

                val conn = uri.toURL().openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.setRequestProperty("User-Agent", "PDFchemy-Desktop/$CURRENT_VERSION")

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                        ?: return@withContext Result.failure(IllegalStateException("Redirect without Location header"))
                    currentUrl = location
                    redirects++
                    continue
                }

                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("Failed to download SHA256SUMS.txt: HTTP $code"))
                }

                text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                break
            }

            val map = mutableMapOf<String, String>()
            text.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    // Standard format: <hash>  <filename> or <hash> *<filename>
                    val parts = trimmed.split(Regex("\\s+"), limit = 2)
                    if (parts.size == 2) {
                        val hash = parts[0].trim().lowercase(Locale.ROOT)
                        val fname = parts[1].trim().removePrefix("*").trim()
                        map[fname] = hash
                    }
                }
            }
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Streams the release asset to a secure temporary file with progress tracking and cancel checking.
     */
    suspend fun downloadAssetFile(
        asset: ReleaseAsset,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
        isCancelled: () -> Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val extension = when {
                asset.name.endsWith(".msi", ignoreCase = true) -> ".msi"
                asset.name.endsWith(".exe", ignoreCase = true) -> ".exe"
                asset.name.endsWith(".deb", ignoreCase = true) -> ".deb"
                asset.name.endsWith(".rpm", ignoreCase = true) -> ".rpm"
                asset.name.endsWith(".jar", ignoreCase = true) -> ".jar"
                else -> ".tmp"
            }

            tempFile = File.createTempFile("pdfchemy_update_", extension)
            tempFile.deleteOnExit()

            var currentUrl = asset.downloadUrl
            var redirects = 0

            while (redirects < 5) {
                if (isCancelled()) throw CancellationException("Update download was cancelled by user.")

                val uri = URI(currentUrl)
                validateSecureGitHubUri(uri)

                val conn = uri.toURL().openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.setRequestProperty("User-Agent", "PDFchemy-Desktop/$CURRENT_VERSION")

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                        ?: return@withContext Result.failure(IllegalStateException("Redirect without Location header"))
                    currentUrl = location
                    redirects++
                    continue
                }

                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("Download failed with HTTP $code"))
                }

                val contentLength = conn.contentLengthLong.let { if (it > 0) it else asset.size }
                var downloadedBytes = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024) // 64 KB chunk
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (isCancelled()) {
                                throw CancellationException("Update download was cancelled by user.")
                            }
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            onProgress(downloadedBytes, contentLength)
                        }
                        output.flush()
                    }
                }
                break
            }

            val finalFile = tempFile ?: return@withContext Result.failure(IllegalStateException("File creation failed"))
            Result.success(finalFile)
        } catch (e: Exception) {
            try {
                tempFile?.delete()
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    /**
     * Calculates the SHA-256 hash of a file on disk and verifies it against the expected hash.
     */
    suspend fun verifyFileSha256(file: File, expectedSha256: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val calculated = digest.digest().joinToString("") { "%02x".format(it) }
            val cleanExpected = expectedSha256.trim().lowercase(Locale.ROOT)
            val match = calculated.equals(cleanExpected, ignoreCase = true)
            if (!match) {
                // Immediate security deletion of untrusted binary
                try { file.delete() } catch (_: Exception) {}
            }
            match
        } catch (_: Exception) {
            try { file.delete() } catch (_: Exception) {}
            false
        }
    }

    /**
     * Safely launches the installer using strict parameterized ProcessBuilder without shell concatenation.
     */
    fun launchInstaller(installerFile: File): Result<Unit> {
        return try {
            val canonicalPath = installerFile.canonicalPath
            val name = installerFile.name.lowercase(Locale.ROOT)
            val osName = System.getProperty("os.name", "").lowercase(Locale.ROOT)

            val processBuilder = when {
                name.endsWith(".msi") -> {
                    // Windows MSI installer via msiexec
                    ProcessBuilder("msiexec.exe", "/i", canonicalPath)
                }
                name.endsWith(".exe") -> {
                    // Windows executable installer
                    ProcessBuilder(canonicalPath)
                }
                name.endsWith(".deb") -> {
                    // Linux DEB package: open with default GUI package manager (e.g. Ubuntu Software, GDebi)
                    ProcessBuilder("xdg-open", canonicalPath)
                }
                name.endsWith(".rpm") -> {
                    // Linux RPM package: open with default GUI installer
                    ProcessBuilder("xdg-open", canonicalPath)
                }
                name.endsWith(".jar") -> {
                    // Standalone executable JAR
                    ProcessBuilder("java", "-jar", canonicalPath)
                }
                else -> {
                    if (osName.contains("win")) {
                        ProcessBuilder("explorer.exe", canonicalPath)
                    } else {
                        ProcessBuilder("xdg-open", canonicalPath)
                    }
                }
            }

            processBuilder.start()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseAssets(json: String): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()
        val assetsKey = "\"assets\""
        val startIdx = json.indexOf(assetsKey)
        if (startIdx == -1) return assets

        val arrayStart = json.indexOf('[', startIdx)
        if (arrayStart == -1) return assets

        var depth = 0
        var arrayEnd = -1
        for (i in arrayStart until json.length) {
            when (json[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        arrayEnd = i
                        break
                    }
                }
            }
        }
        if (arrayEnd == -1) return assets

        val assetsJson = json.substring(arrayStart + 1, arrayEnd)
        // Split by individual JSON objects
        var objDepth = 0
        var objStart = -1
        for (i in assetsJson.indices) {
            val ch = assetsJson[i]
            if (ch == '{') {
                if (objDepth == 0) objStart = i
                objDepth++
            } else if (ch == '}') {
                objDepth--
                if (objDepth == 0 && objStart != -1) {
                    val obj = assetsJson.substring(objStart, i + 1)
                    val name = extractJsonField(obj, "name")
                    val downloadUrl = extractJsonField(obj, "browser_download_url")
                    val size = extractJsonLong(obj, "size") ?: 0L
                    if (!name.isNullOrBlank() && !downloadUrl.isNullOrBlank()) {
                        assets.add(ReleaseAsset(name = name, downloadUrl = downloadUrl, size = size))
                    }
                    objStart = -1
                }
            }
        }
        return assets
    }

    private fun extractJsonField(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }

    private fun extractJsonLong(json: String, key: String): Long? {
        val pattern = Regex("\"$key\"\\s*:\\s*([0-9]+)")
        return pattern.find(json)?.groupValues?.getOrNull(1)?.toLongOrNull()
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
