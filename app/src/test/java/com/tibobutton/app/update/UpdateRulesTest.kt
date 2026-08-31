package com.tibobutton.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateRulesTest {
    @Test fun parsesOnlyStableThreePartTags() {
        assertEquals(SemanticVersion(0, 3, 1), UpdateRules.parseStableTag("v0.3.1"))
        assertNull(UpdateRules.parseStableTag("0.3.1"))
        assertNull(UpdateRules.parseStableTag("v0.3.1-rc1"))
        assertNull(UpdateRules.parseStableTag("v01.3.1"))
    }

    @Test fun comparesSemanticVersionsByMajorMinorPatch() {
        assertEquals(-1, SemanticVersion(0, 3, 9).compareTo(SemanticVersion(0, 4, 0)))
        assertEquals(-1, SemanticVersion(1, 0, 0).compareTo(SemanticVersion(2, 0, 0)))
        assertEquals(0, SemanticVersion(0, 3, 1).compareTo(SemanticVersion(0, 3, 1)))
        assertEquals(1, SemanticVersion(0, 3, 2).compareTo(SemanticVersion(0, 3, 1)))
    }

    @Test fun normalizesValidGitHubSha256DigestOnly() {
        val digest = "A".repeat(64)
        assertEquals(digest.lowercase(), UpdateRules.normalizeSha256Digest("sha256:$digest"))
        assertEquals(digest.lowercase(), UpdateRules.normalizeSha256Digest(" SHA256:$digest "))
        assertNull(UpdateRules.normalizeSha256Digest(digest))
        assertNull(UpdateRules.normalizeSha256Digest("sha256:xyz"))
    }

    @Test fun findsExactApkFilenameInChecksumManifest() {
        val expected = "a".repeat(64)
        val other = "b".repeat(64)
        val checksums = """
            $other  dist/TiboButton-v0.3.10.apk
            $expected *dist/TiboButton-v0.3.1.apk
        """.trimIndent()

        assertEquals(
            expected,
            UpdateRules.findChecksum(checksums, "TiboButton-v0.3.1.apk")
        )
        assertNull(UpdateRules.findChecksum(checksums, "TiboButton-v0.3.2.apk"))
    }
}
