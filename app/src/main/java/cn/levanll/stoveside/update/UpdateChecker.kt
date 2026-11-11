package cn.levanll.stoveside.update

import cn.levanll.stoveside.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用内更新：查 GitHub Releases 最新正式版。
 * 只用公开 API，不带凭据；失败一律返回 Result.failure 由 UI 提示，不静默。
 */
object UpdateChecker {

    private const val ENDPOINT =
        "https://api.github.com/repos/Levanll/StoveSide/releases/latest"

    data class Release(
        val tag: String,
        val versionName: String,
        val versionCode: Int,
        val notes: String,
        val apkUrl: String,
        val apkName: String,
        val sizeBytes: Long,
        val sha256: String?,
    )

    suspend fun check(): Result<Release> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "StoveSide/${BuildConfig.VERSION_NAME}")
            }
            try {
                if (conn.responseCode != 200) error("http_${conn.responseCode}")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parse(JSONObject(body))
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun parse(json: JSONObject): Release {
        val tag = json.optString("tag_name", "").removePrefix("v")
        val versionCode = tag.substringAfterLast('.').toIntOrNull() ?: 0
        val assets = json.optJSONArray("assets")
        var apkUrl = ""
        var apkName = ""
        var size = 0L
        for (i in 0 until (assets?.length() ?: 0)) {
            val a = assets!!.getJSONObject(i)
            val name = a.optString("name")
            if (name.endsWith(".apk")) {
                apkUrl = a.optString("browser_download_url")
                apkName = name
                size = a.optLong("size")
                break
            }
        }
        return Release(
            tag = tag,
            versionName = tag,
            versionCode = versionCode,
            notes = json.optString("body", ""),
            apkUrl = apkUrl,
            apkName = apkName,
            sizeBytes = size,
            sha256 = null,
        )
    }

    /**
     * 比较版本：优先用 tag 里的语义版本（v1.0.1 → 1.0.1），
     * 因为 GitHub tag 只保证 versionName，versionCode 不在 tag 里。
     */
    fun hasUpdate(remote: Release): Boolean = compareVersions(remote.versionName, BuildConfig.VERSION_NAME) > 0

    fun compareVersions(a: String, b: String): Int {
        val pa = a.trim().removePrefix("v").split('.', '-')
        val pb = b.trim().removePrefix("v").split('.', '-')
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrNull(i)?.toIntOrNull() ?: 0
            val y = pb.getOrNull(i)?.toIntOrNull() ?: 0
            if (x != y) return if (x > y) 1 else -1
        }
        return 0
    }
}