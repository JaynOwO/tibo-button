# Changelog

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
