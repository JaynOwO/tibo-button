# Releasing Tibo Button

This document describes the signed GitHub Release flow introduced for v0.2.1.

A debug APK is useful for development, but public updates need a stable release-signing key. Every published APK must be signed with the same key so Android can install future versions over the existing app.

## One-time signing setup

### 1. Generate the release keystore

On Windows, open PowerShell in the repository root and run:

```powershell
.\scripts\create-release-keystore.ps1
```

The script uses `keytool`, prompts for the keystore/key passwords, and creates:

```text
tibo-button-release.jks
tibo-button-release.jks.base64
```

If `keytool` is not on `PATH`, use the JDK bundled with Android Studio or install a current JDK first.

Keep the `.jks` file and both passwords in at least two secure backups. Do not commit either generated file. Losing the signing key means direct APK users cannot install future updates over the existing app.

### 2. Add GitHub Actions secrets

Open:

```text
Repository → Settings → Secrets and variables → Actions
```

Create these repository secrets:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Entire contents of `tibo-button-release.jks.base64` |
| `RELEASE_STORE_PASSWORD` | Keystore password entered in `keytool` |
| `RELEASE_KEY_ALIAS` | `tibo-button`, unless a different alias was chosen |
| `RELEASE_KEY_PASSWORD` | Private-key password entered in `keytool` |

Never paste these values into an issue, commit, pull request, Actions log, or chat transcript.

### 3. Dry-run the signed build

Open:

```text
Actions → Build signed release → Run workflow
```

A manual run:

- runs unit tests,
- builds a signed release APK,
- verifies the APK signature,
- creates `SHA256SUMS.txt`,
- uploads a temporary workflow artifact,
- does **not** create a public GitHub Release.

Install the dry-run APK once and confirm the widgets, refresh spinner, notifications, and in-app details work.

## Publish a version

Before tagging:

1. `main` must be clean and pushed.
2. The normal `Build Android APK` workflow must be green.
3. `versionName` and `versionCode` must be updated.
4. `CHANGELOG.md` must describe the version.
5. A signed release dry run should have passed.

For v0.2.1:

```bash
git tag -a v0.2.1 -m "Tibo Button v0.2.1"
git push origin v0.2.1
```

The tag must exactly match `v` + Android `versionName`. The release workflow rejects mismatches.

A successful tag build automatically creates or updates a GitHub Release containing:

```text
TiboButton-v0.2.1.apk
SHA256SUMS.txt
```

The APK signature is checked before publication.

## Never do these

- Never commit the keystore or its Base64 text.
- Never generate a fresh signing key for each release.
- Never reuse or move an already published version tag.
- Never publish an unsigned APK as a stable release.
- Never expose passwords through command-line arguments, logs, screenshots, or release notes.

For a release fix, increase `versionCode`, choose a new `versionName`, commit, and create a new tag.
