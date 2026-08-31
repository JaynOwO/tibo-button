package com.tibobutton.app.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

sealed class UpdateCheckResult {
    data class Success(val release: StableRelease) : UpdateCheckResult()
    data class Failure(val message: String) : UpdateCheckResult()
}

sealed class UpdateDownloadResult {
    data object NeedsUnknownSourcesPermission : UpdateDownloadResult()
    data class Progress(val percent: Int) : UpdateDownloadResult()
    data object Verifying : UpdateDownloadResult()
    data class Verified(val apk: File) : UpdateDownloadResult()
    data class Failure(val message: String) : UpdateDownloadResult()
}

class UpdateManager(context: Context) {
    private val appContext = context.applicationContext
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val checkInProgress = AtomicBoolean(false)
    private val downloadInProgress = AtomicBoolean(false)

    fun installedVersionName(): String = runCatching {
        installedPackageInfo().versionName ?: "未知"
    }.getOrDefault("未知")

    fun installedVersion(): SemanticVersion? = UpdateRules.parseStableTag("v${installedVersionName()}")

    fun installedVersionCode(): Long = packageVersionCode(installedPackageInfo())

    fun canRequestPackageInstalls(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || appContext.packageManager.canRequestPackageInstalls()

    fun checkLatest(callback: (UpdateCheckResult) -> Unit): Boolean {
        if (!checkInProgress.compareAndSet(false, true)) return false
        executor.execute {
            val result = try {
                UpdateCheckResult.Success(fetchLatestRelease())
            } catch (t: Throwable) {
                UpdateCheckResult.Failure(t.message?.take(180) ?: "检查更新失败")
            }
            post { callback(result) }
            checkInProgress.set(false)
        }
        return true
    }

    fun downloadAndVerify(
        release: StableRelease,
        callback: (UpdateDownloadResult) -> Unit
    ): Boolean {
        if (!downloadInProgress.compareAndSet(false, true)) return false
        if (!canRequestPackageInstalls()) {
            downloadInProgress.set(false)
            post { callback(UpdateDownloadResult.NeedsUnknownSourcesPermission) }
            return true
        }

        executor.execute {
            var target: File? = null
            var partial: File? = null
            var verified = false
            try {
                val updatesDir = File(appContext.cacheDir, UPDATES_DIRECTORY)
                if (!updatesDir.exists() && !updatesDir.mkdirs()) {
                    throw UpdateException("无法创建应用私有更新缓存目录")
                }

                val targetFile = File(updatesDir, release.apk.name)
                val partialFile = File(updatesDir, "${release.apk.name}.part")
                target = targetFile
                partial = partialFile
                deleteQuietly(targetFile)
                deleteQuietly(partialFile)

                downloadToFile(release.apk.downloadUrl, partialFile) { percent ->
                    post { callback(UpdateDownloadResult.Progress(percent)) }
                }
                if (!partialFile.renameTo(targetFile)) throw UpdateException("无法保存下载的 APK")

                post { callback(UpdateDownloadResult.Verifying) }
                verifyDownloadedApk(release, targetFile)
                verified = true
                post { callback(UpdateDownloadResult.Verified(targetFile)) }
            } catch (t: Throwable) {
                deleteQuietly(target)
                deleteQuietly(partial)
                post { callback(UpdateDownloadResult.Failure(t.message?.take(180) ?: "更新验证失败")) }
            } finally {
                if (!verified) {
                    deleteQuietly(target)
                    deleteQuietly(partial)
                }
                downloadInProgress.set(false)
            }
        }
        return true
    }

    fun installerUri(verifiedApk: File): Uri {
        val updatesDir = File(appContext.cacheDir, UPDATES_DIRECTORY).canonicalFile
        val apk = verifiedApk.canonicalFile
        val allowedPrefix = updatesDir.path + File.separator
        if (!apk.path.startsWith(allowedPrefix)) {
            throw UpdateException("更新 APK 不在受限缓存目录中")
        }
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apk
        )
    }

    fun shutdown() {
        executor.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun fetchLatestRelease(): StableRelease {
        val connection = openSuccessfulConnection(LATEST_RELEASE_URL, "application/vnd.github+json")
        val body = readText(connection, MAX_JSON_BYTES)
        val root = JSONObject(body)

        if (root.optBoolean("draft", false)) throw UpdateException("最新 Release 仍是 Draft")
        if (root.optBoolean("prerelease", false)) throw UpdateException("最新 Release 是 Prerelease")

        val tag = root.optString("tag_name", "")
        val version = UpdateRules.parseStableTag(tag)
            ?: throw UpdateException("Release tag 不是稳定版 vX.Y.Z")
        val releaseUrl = root.optString("html_url", "")
        requireReleaseUrl(releaseUrl)

        val assets = root.optJSONArray("assets") ?: throw UpdateException("Release 没有资产列表")
        val parsedAssets = buildList {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                if (name.isBlank() || downloadUrl.isBlank()) continue
                requireAssetUrl(downloadUrl)
                add(
                    ReleaseAsset(
                        name = name,
                        downloadUrl = downloadUrl,
                        digest = asset.optString("digest", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        }

        val expectedApkName = "TiboButton-${version.tagName()}.apk"
        val apk = parsedAssets.firstOrNull { it.name == expectedApkName }
            ?: throw UpdateException("Release 缺少精确资产 $expectedApkName")
        val checksum = parsedAssets.firstOrNull { it.name == "SHA256SUMS.txt" }
        if (UpdateRules.normalizeSha256Digest(apk.digest) == null && checksum == null) {
            throw UpdateException("Release 缺少 APK digest 和 SHA256SUMS.txt")
        }

        return StableRelease(
            version = version,
            htmlUrl = releaseUrl,
            notes = root.optString("body", "").trim().take(MAX_NOTES_LENGTH),
            apk = apk,
            checksum = checksum
        )
    }

    private fun verifyDownloadedApk(release: StableRelease, apk: File) {
        val expectedDigest = UpdateRules.normalizeSha256Digest(release.apk.digest)
            ?: release.checksum?.let { checksumAsset ->
                val checksumConnection = openSuccessfulConnection(
                    checksumAsset.downloadUrl,
                    "text/plain, */*"
                )
                val checksumText = readText(checksumConnection, MAX_CHECKSUM_BYTES)
                UpdateRules.findChecksum(checksumText, release.apk.name)
                    ?: throw UpdateException("SHA256SUMS.txt 找不到 ${release.apk.name}")
            }
            ?: throw UpdateException("没有可用的 APK SHA-256 校验值")

        val actualDigest = sha256(apk)
        if (!actualDigest.equals(expectedDigest, ignoreCase = true)) {
            throw UpdateException("APK SHA-256 校验失败")
        }

        val packageManager = appContext.packageManager
        val archiveInfo = archivePackageInfo(packageManager, apk)
            ?: throw UpdateException("无法读取下载 APK 的包信息")
        if (archiveInfo.packageName != appContext.packageName) {
            throw UpdateException("APK applicationId 不匹配")
        }
        if (archiveInfo.versionName != release.version.toString()) {
            throw UpdateException("APK versionName 与 Release tag 不匹配")
        }

        val installedInfo = installedPackageInfo()
        if (packageVersionCode(archiveInfo) <= packageVersionCode(installedInfo)) {
            throw UpdateException("APK 不是比当前安装版本更新的版本")
        }

        val installedSigners = signerDigests(installedInfo)
        val archiveSigners = signerDigests(archiveInfo)
        if (installedSigners.isEmpty() || archiveSigners.isEmpty() || installedSigners != archiveSigners) {
            throw UpdateException("APK 签名证书与当前安装版本不匹配")
        }
    }

    private fun downloadToFile(url: String, target: File, onProgress: (Int) -> Unit) {
        val connection = openSuccessfulConnection(url, "application/vnd.android.package-archive")
        val contentLength = connection.contentLengthLong
        if (contentLength > MAX_APK_BYTES) throw UpdateException("APK 文件过大")

        var total = 0L
        var lastPercent = -1
        try {
            connection.inputStream.buffered(BUFFER_SIZE).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_APK_BYTES) throw UpdateException("APK 文件超过大小限制")
                        output.write(buffer, 0, read)
                        val percent = if (contentLength > 0) {
                            ((total * 100L / contentLength).toInt()).coerceIn(0, 99)
                        } else {
                            -1
                        }
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        onProgress(100)
    }

    private fun readText(connection: HttpURLConnection, maxBytes: Long): String {
        var total = 0L
        return try {
            connection.inputStream.buffered(BUFFER_SIZE).use { input ->
                val bytes = input.readBytesLimited(maxBytes) { count ->
                    total += count
                    if (total > maxBytes) throw UpdateException("GitHub 响应过大")
                }
                bytes.toString(Charsets.UTF_8)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openSuccessfulConnection(rawUrl: String, accept: String): HttpURLConnection {
        var current = URL(rawUrl)
        repeat(MAX_REDIRECTS + 1) { attempt ->
            requireAllowedUrl(current)
            val connection = (current.openConnection() as? HttpURLConnection)
                ?: throw UpdateException("更新地址不是 HTTP(S) 连接")
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", accept)
                setRequestProperty("User-Agent", USER_AGENT)
                if (current.host.equals("api.github.com", ignoreCase = true)) {
                    setRequestProperty("X-GitHub-Api-Version", GITHUB_API_VERSION)
                }
                useCaches = false
            }

            val code = connection.responseCode
            if (code in 200..299) return connection
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank() || attempt == MAX_REDIRECTS) {
                    throw UpdateException("GitHub 下载重定向无效")
                }
                current = URL(current, location)
            } else {
                connection.disconnect()
                throw UpdateException("GitHub 请求失败 HTTP $code")
            }
        }
        throw UpdateException("GitHub 下载重定向次数过多")
    }

    private fun requireReleaseUrl(rawUrl: String) {
        val url = runCatching { URL(rawUrl) }.getOrNull()
        if (url == null || url.protocol != "https" || !url.host.equals("github.com", true) ||
            !url.path.startsWith("/JaynOwO/tibo-button/releases/")) {
            throw UpdateException("Release 链接不是官方 GitHub 仓库")
        }
    }

    private fun requireAssetUrl(rawUrl: String) {
        val url = runCatching { URL(rawUrl) }.getOrNull()
        if (url == null || url.protocol != "https" ||
            !url.host.equals("github.com", true) ||
            !url.path.startsWith("/JaynOwO/tibo-button/releases/download/")) {
            throw UpdateException("Release 资产链接不是官方 GitHub 下载地址")
        }
    }

    private fun requireAllowedUrl(url: URL) {
        val allowedHost = url.protocol.equals("https", true) && listOf(
            "api.github.com",
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        ).any { url.host.equals(it, true) }
        if (!allowedHost) throw UpdateException("更新请求被拒绝：非 GitHub 官方 HTTPS 地址")
    }

    @Suppress("DEPRECATION")
    private fun installedPackageInfo(): PackageInfo {
        val packageManager = appContext.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.PackageInfoFlags.of(packageInfoFlags().toLong())
            )
        } else {
            packageManager.getPackageInfo(appContext.packageName, packageInfoFlags())
        }
    }

    @Suppress("DEPRECATION")
    private fun archivePackageInfo(packageManager: PackageManager, apk: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                apk.absolutePath,
                PackageManager.PackageInfoFlags.of(packageInfoFlags().toLong())
            )
        } else {
            packageManager.getPackageArchiveInfo(apk.absolutePath, packageInfoFlags())
        }

    @Suppress("DEPRECATION")
    private fun packageInfoFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        PackageManager.GET_SIGNATURES
    }

    @Suppress("DEPRECATION")
    private fun packageVersionCode(info: PackageInfo): Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        info.versionCode.toLong()
    }

    @Suppress("DEPRECATION")
    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            info.signatures ?: emptyArray()
        }
        return signatures.map { sha256(it.toByteArray()) }.toSet()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun post(block: () -> Unit) {
        mainHandler.post(block)
    }

    private fun deleteQuietly(file: File?) {
        if (file != null && file.exists()) file.delete()
    }

    private class UpdateException(message: String) : Exception(message)

    companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/JaynOwO/tibo-button/releases/latest"
        const val LATEST_RELEASE_FALLBACK_URL =
            "https://github.com/JaynOwO/tibo-button/releases/latest"

        private const val USER_AGENT = "TiboButton/0.3.1 Android (+https://github.com/JaynOwO/tibo-button)"
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val UPDATES_DIRECTORY = "updates"
        private const val BUFFER_SIZE = 16 * 1024
        private const val MAX_REDIRECTS = 3
        private const val MAX_APK_BYTES = 50L * 1024L * 1024L
        private const val MAX_JSON_BYTES = 2L * 1024L * 1024L
        private const val MAX_CHECKSUM_BYTES = 1L * 1024L * 1024L
        private const val MAX_NOTES_LENGTH = 600
    }
}

private fun java.io.InputStream.readBytesLimited(
    maxBytes: Long,
    onChunk: (Long) -> Unit
): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        onChunk(read.toLong())
        output.write(buffer, 0, read)
        if (output.size().toLong() > maxBytes) throw IllegalStateException("响应过大")
    }
    return output.toByteArray()
}

private fun SemanticVersion.tagName(): String = "v$this"
