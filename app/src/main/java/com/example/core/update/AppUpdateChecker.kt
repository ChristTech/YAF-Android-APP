package com.example.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubReleaseUpdate(
    val latestVersion: String,
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long,
    val publishedAtIso: String?
)

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpToDate(val currentVersion: String, val message: String? = null) : UpdateCheckState()
    data class UpdateAvailable(val release: GitHubReleaseUpdate, val isManualCheck: Boolean) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

object AppUpdateChecker {
    const val GITHUB_OWNER = "ChristTech"
    const val GITHUB_REPO = "YAF-Android-APP"
    const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val GITHUB_RELEASES_PAGE =
        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"

    /**
     * Compares version strings such as "1.0" vs "v1.0.1" or "v1.2.0-beta".
     * Returns true strictly when [latestVersionOrTag] represents a higher numeric version than [currentVersion].
     */
    fun isNewerVersion(currentVersion: String, latestVersionOrTag: String): Boolean {
        val currentParts = parseNumericVersionParts(currentVersion)
        val latestParts = parseNumericVersionParts(latestVersionOrTag)
        if (currentParts.isEmpty() || latestParts.isEmpty()) return false

        val maxLen = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val cur = currentParts.getOrElse(i) { 0 }
            val lat = latestParts.getOrElse(i) { 0 }
            if (lat > cur) return true
            if (lat < cur) return false
        }
        return false
    }

    fun normalizeVersionLabel(raw: String): String {
        return raw.trim().removePrefix("v").removePrefix("V")
    }

    private fun parseNumericVersionParts(raw: String): List<Int> {
        val clean = normalizeVersionLabel(raw)
            .substringBefore('-')
            .substringBefore('+')
            .trim()
        if (clean.isBlank()) return emptyList()
        return clean.split('.')
            .mapNotNull { seg ->
                val digits = seg.takeWhile { it.isDigit() }
                digits.toIntOrNull()
            }
    }

    fun parseGitHubReleaseJson(jsonBody: String): GitHubReleaseUpdate? {
        return try {
            val root = JSONObject(jsonBody)
            if (root.optBoolean("draft", false)) return null
            val tagName = root.optString("tag_name").trim()
            if (tagName.isBlank()) return null

            val latestVersion = normalizeVersionLabel(tagName)
            val releaseTitle = root.optString("name").trim().ifBlank { "Version $latestVersion" }
            val releaseNotes = root.optString("body").trim()
                .ifBlank { "New update available for The Gideonites - The Youth Alive Fellowship." }
            val htmlUrl = root.optString("html_url").trim().ifBlank { GITHUB_RELEASES_PAGE }
            val publishedAtIso = root.optString("published_at").trim().takeIf { it.isNotBlank() }

            var chosenApkUrl: String? = null
            var chosenApkName: String? = null
            var chosenApkSize = 0L

            val assets = root.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name").trim()
                    val downloadUrl = asset.optString("browser_download_url").trim()
                    val size = asset.optLong("size", 0L)
                    if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                        // Prefer release APK over debug APK if multiple exist
                        val isPreferredReleaseApk = !name.contains("debug", ignoreCase = true)
                        if (chosenApkUrl == null || isPreferredReleaseApk) {
                            chosenApkUrl = downloadUrl
                            chosenApkName = name
                            chosenApkSize = size
                            if (isPreferredReleaseApk) break
                        }
                    }
                }
            }

            GitHubReleaseUpdate(
                latestVersion = latestVersion,
                tagName = tagName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                htmlUrl = htmlUrl,
                apkDownloadUrl = chosenApkUrl,
                apkFileName = chosenApkName,
                apkSizeBytes = chosenApkSize,
                publishedAtIso = publishedAtIso
            )
        } catch (_: Exception) {
            null
        }
    }

    fun formatApkSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}

class GitHubUpdateManager(
    context: Context,
    private val currentVersionName: String = BuildConfig.VERSION_NAME
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

    suspend fun checkForUpdates(manual: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!manual) {
            val lastAutoCheck = prefs.getLong(KEY_LAST_AUTO_CHECK_MS, 0L)
            if (now - lastAutoCheck < AUTO_CHECK_INTERVAL_MS && _state.value !is UpdateCheckState.Idle) {
                return
            }
            if (now - lastAutoCheck < AUTO_CHECK_INTERVAL_MS) {
                return
            }
        }

        _state.value = UpdateCheckState.Checking

        val result = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(AppUpdateChecker.GITHUB_LATEST_RELEASE_API)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "${AppUpdateChecker.GITHUB_REPO}-Android/$currentVersionName")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!manual) {
                        prefs.edit().putLong(KEY_LAST_AUTO_CHECK_MS, System.currentTimeMillis()).apply()
                    }
                    when {
                        response.code == 404 -> {
                            FetchOutcome.UpToDate(
                                "You are on v$currentVersionName. No GitHub releases published yet."
                            )
                        }
                        response.code == 403 || response.code == 429 -> {
                            FetchOutcome.Error("GitHub rate limit reached. Try opening Releases directly.")
                        }
                        !response.isSuccessful -> {
                            FetchOutcome.Error("Could not check updates (HTTP ${response.code}).")
                        }
                        else -> {
                            val body = response.body?.string().orEmpty()
                            val parsed = AppUpdateChecker.parseGitHubReleaseJson(body)
                            if (parsed == null) {
                                FetchOutcome.UpToDate("You are on the latest version (v$currentVersionName).")
                            } else if (AppUpdateChecker.isNewerVersion(currentVersionName, parsed.tagName)) {
                                FetchOutcome.FoundUpdate(parsed)
                            } else {
                                FetchOutcome.UpToDate("You are up to date (v$currentVersionName).")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                FetchOutcome.Error("Unable to reach GitHub. Check your internet connection.")
            }
        }

        when (result) {
            is FetchOutcome.FoundUpdate -> {
                val skippedTag = prefs.getString(KEY_SKIPPED_TAG, null)
                if (!manual && skippedTag == result.release.tagName) {
                    _state.value = UpdateCheckState.Idle
                } else {
                    _state.value = UpdateCheckState.UpdateAvailable(
                        release = result.release,
                        isManualCheck = manual
                    )
                }
            }
            is FetchOutcome.UpToDate -> {
                _state.value = if (manual) {
                    UpdateCheckState.UpToDate(currentVersionName, result.note)
                } else {
                    UpdateCheckState.Idle
                }
            }
            is FetchOutcome.Error -> {
                _state.value = if (manual) {
                    UpdateCheckState.Error(result.message)
                } else {
                    UpdateCheckState.Idle
                }
            }
        }
    }

    fun dismissUpdateDialog(tagName: String? = null, skipVersion: Boolean = false) {
        if (skipVersion && !tagName.isNullOrBlank()) {
            prefs.edit().putString(KEY_SKIPPED_TAG, tagName).apply()
        }
        _state.value = UpdateCheckState.Idle
    }

    fun openDownloadInBrowser(context: Context, url: String? = null) {
        val targetUrl = url?.takeIf { it.isNotBlank() } ?: AppUpdateChecker.GITHUB_RELEASES_PAGE
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            if (targetUrl != AppUpdateChecker.GITHUB_RELEASES_PAGE) {
                runCatching {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateChecker.GITHUB_RELEASES_PAGE)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                }
            }
        }
    }

    private sealed class FetchOutcome {
        data class FoundUpdate(val release: GitHubReleaseUpdate) : FetchOutcome()
        data class UpToDate(val note: String) : FetchOutcome()
        data class Error(val message: String) : FetchOutcome()
    }

    companion object {
        private const val PREFS_NAME = "yaf_github_update_prefs"
        private const val KEY_LAST_AUTO_CHECK_MS = "last_auto_check_ms"
        private const val KEY_SKIPPED_TAG = "skipped_release_tag"
        private const val AUTO_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L // 6 hours
    }
}
