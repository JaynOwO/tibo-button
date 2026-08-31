# Changelog

## 0.3.1 - Unreleased

- Added a secure in-app updater for the official public `JaynOwO/tibo-button` stable GitHub Releases endpoint.
- Added stable-tag, exact APK asset, SHA-256, package ID, version, and signing-certificate verification before handing an APK to Android's installer.
- Added app-private update caching, scoped FileProvider delivery, unknown-source permission handling, download progress, and explicit system installation confirmation.
- Automatic update checks are opt-in by setting but enabled by default; automatic download and installation are never performed.
- v0.3.1 includes the Reset Pulse and persistent notification deduplication work from the tested v0.3.0 development build.

## 0.3.0 - Development-only build

- Added **Reset Pulse** with the seven most recent broad completed reset records.
- Added a trailing 7-day reset count, average recent reset interval, and an explicitly defined recent cadence streak (consecutive broad completed events no more than 72 hours apart).
- Added plain-language status explanations for confirmed, likely, possible, low, unlikely, stale, and unknown states.
- Added persistent notification fingerprints so the same confirmed/completed signal is not re-alerted just because evidence metadata changes or a process restarts.
- Added a compact 7-day reset count to the 4×2 widget without making the widget denser elsewhere.
- Improved the notification settings copy to explain defaults and deduplication behavior.
- Updated the public README with badges, Reset Pulse semantics, and direct stable-release discovery.
- Bumped Android `versionCode` to 4 and `versionName` to 0.3.0.

## 0.2.1 - 2026-08-30

- Replaced the flipped loading glyph with a genuinely rotating refresh-arrow animation.
- Implemented the widget animation with a supported indeterminate `ProgressBar` and level-driven `RotateDrawable`, avoiding rapid repeated widget updates.
- Added stable release-signing support through environment variables.
- Added a GitHub Actions signed-release workflow with unit tests, tag/version validation, APK signature verification, SHA-256 checksums, and automatic GitHub Release assets.
- Added a PowerShell helper for generating the one-time release keystore without placing passwords in source files.
- Added release documentation and expanded public-repository secret protections.
- Bumped Android `versionCode` to 3 and `versionName` to 0.2.1.

## 0.2.0 - 2026-08-30

- Added immediate widget refresh feedback with a deterministic loading-state icon.
- Added Android notification permission/settings UI.
- Added optional notifications for confirmed schedules, newly completed broad resets, and very-likely status.
- Added a richer in-app detail card with Reset Beacon canonical answer text and evidence link.
- Added parsing for the documented `answer.deadline` field when available.
- Added MIT license, independence/trademark disclaimer, Reset Beacon attribution, and privacy notes.
- Updated GitHub Actions checkout/setup-java actions to v5.
- Updated the Gradle setup action to v6 to remove the Node.js 20 deprecation warning.
- Updated the artifact upload action to v6 for the same Node.js 24 runtime.
- Added a unit-test step before the APK build in GitHub Actions.
- Bumped Android versionCode to 2 and versionName to 0.2.0.
