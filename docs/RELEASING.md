# Releasing Tibo Button

This document describes the signed GitHub Release flow used for Tibo Button. The same process applies to each stable vX.Y.Z release; the examples below use v0.3.1.

A debug APK is useful for development, but public updates need a stable release-signing key. Every published APK must be signed with the same key so Android can install future versions over the existing app.

## One-time signing setup

### 1. Generate the release keystore

On Windows, open PowerShell in the repository root and run:

~~~powershell
.\scripts\create-release-keystore.ps1
~~~

The script uses keytool, prompts for the keystore/key passwords, and creates:

~~~text
tibo-button-release.jks
tibo-button-release.jks.base64
~~~

If keytool is not on PATH, use the JDK bundled with Android Studio or install a current JDK first.

Keep the .jks file and both passwords in at least two secure backups. Do not commit either generated file. Losing the signing key means direct APK users cannot install future updates over the existing app.

### 2. Add GitHub Actions secrets

Open:

~~~text
Repository → Settings → Secrets and variables → Actions
~~~

Create these repository secrets:

| Secret | Value |
|---|---|
| RELEASE_KEYSTORE_BASE64 | Entire contents of tibo-button-release.jks.base64 |
| RELEASE_STORE_PASSWORD | Keystore password entered in keytool |
| RELEASE_KEY_ALIAS | tibo-button, unless a different alias was chosen |
| RELEASE_KEY_PASSWORD | Private-key password entered in keytool |

Never paste these values into an issue, commit, pull request, Actions log, or chat transcript.

## Pre-release verification

Before tagging a version:

1. Pull the latest origin/main and confirm the worktree is clean.
2. Confirm versionName and versionCode in app/build.gradle.kts match the intended release.
3. Run the normal tests and Debug APK build locally when the required JDK/Gradle tooling is available:

   ~~~bash
   gradle test
   gradle :app:assembleDebug
   ~~~

4. Push main and wait for **Build Android APK** to pass with its TiboButton-debug-apk artifact.
5. Review the complete diff and changelog before creating a public tag.

## Signed dry run

Open:

~~~text
Actions → Build signed release → Run workflow
~~~

Run it from the final release commit on main, before creating the release tag. A manual workflow_dispatch run:

- runs unit tests;
- builds a signed release APK;
- validates the configured release secrets without printing their values;
- verifies the APK signature with apksigner;
- creates SHA256SUMS.txt;
- uploads a temporary artifact named TiboButton-vX.Y.Z-release;
- does **not** create a public GitHub Release because the run is on a branch rather than a release tag.

Inspect the full job and step logs. If the dry run fails, fix the root cause, push the fix to main, and repeat the normal CI and dry-run checks before tagging. Do not create a tag while the dry run is failing.

## Publish a version

After the normal CI and signed dry run are green, create an annotated tag from the verified final commit. The tag must exactly match v + Android versionName:

~~~bash
git tag -a v0.3.1 -m "Tibo Button v0.3.1"
git push origin v0.3.1
~~~

The tag-triggered **Build signed release** workflow validates the tag/version match, rebuilds the APK with the configured release key, verifies its signature, writes the checksum file, and creates or updates the GitHub Release. A successful release contains:

~~~text
TiboButton-v0.3.1.apk
SHA256SUMS.txt
~~~

For another version, replace every 0.3.1 occurrence with the matching versionName; never reuse a published tag for a different build.

## Post-release verification

Confirm all of the following on the published Release:

- the Release exists at the intended tag;
- it is not Draft or Prerelease unless that release explicitly requires it;
- the APK and SHA256SUMS.txt are both present;
- the downloaded APK’s SHA-256 matches SHA256SUMS.txt;
- apksigner verify --verbose --print-certs succeeds;
- the APK application ID, versionName, and versionCode match the verified source;
- the README, latest-release link, changelog, and GitHub About metadata describe the same stable version.

Example checksum verification:

~~~powershell
Get-FileHash .\TiboButton-v0.3.1.apk -Algorithm SHA256
Get-Content .\SHA256SUMS.txt
~~~

Do not claim a release is verified solely because a debug build succeeded; use the signed workflow logs and the published assets as the release evidence.

## Failure handling

- Before a tag is public, read the complete failed workflow job logs, fix actionable code or workflow problems, push a new commit, and repeat the checks.
- After a tag is public, a workflow-only fix may be committed and the workflow rerun without moving, deleting, or reusing the published tag.
- If a post-tag fix requires changing application code or the Android version, stop and use a new version/tag instead of changing the published version.
- Never modify, regenerate, or expose signing keys, passwords, or GitHub Actions Secrets as part of failure recovery.

## Never do these

- Never commit the keystore or its Base64 text.
- Never generate a fresh signing key for each release.
- Never reuse or move an already published version tag.
- Never publish an unsigned APK as a stable release.
- Never expose passwords through command-line arguments, logs, screenshots, release notes, or chat.
