package com.tibobutton.app.update

import java.util.Locale

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"
}

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val digest: String?
)

data class StableRelease(
    val version: SemanticVersion,
    val htmlUrl: String,
    val notes: String,
    val apk: ReleaseAsset,
    val checksum: ReleaseAsset?
)

object UpdateRules {
    private val stableTagPattern = Regex(
        "^v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$"
    )
    private val sha256Pattern = Regex("^[0-9a-fA-F]{64}$")

    fun parseStableTag(tag: String): SemanticVersion? {
        val match = stableTagPattern.matchEntire(tag) ?: return null
        return runCatching {
            SemanticVersion(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt()
            )
        }.getOrNull()
    }

    fun normalizeSha256Digest(raw: String?): String? {
        val value = raw?.trim() ?: return null
        val separator = value.indexOf(':')
        if (separator <= 0 || !value.substring(0, separator).equals("sha256", ignoreCase = true)) return null
        val digest = value.substring(separator + 1)
        return digest.takeIf { sha256Pattern.matches(it) }?.lowercase(Locale.ROOT)
    }

    fun findChecksum(checksumText: String, expectedFilename: String): String? {
        return checksumText.lineSequence().firstNotNullOfOrNull { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) return@firstNotNullOfOrNull null

            val columns = line.split(Regex("\\s+"), limit = 2)
            if (columns.size != 2 || !sha256Pattern.matches(columns[0])) {
                return@firstNotNullOfOrNull null
            }

            val filename = columns[1].removePrefix("*")
                .substringAfterLast('/')
                .substringAfterLast('\\')
            if (filename != expectedFilename) return@firstNotNullOfOrNull null
            columns[0].lowercase(Locale.ROOT)
        }
    }
}
