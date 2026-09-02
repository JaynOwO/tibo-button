# Changelog

## 0.3.3 - Unreleased

### Widget compatibility

- Replaced `Space` gaps in all 4×2 RemoteViews layouts with standard layout margins to improve Samsung / One UI widget-host compatibility without changing widget content or refresh behavior.
- Kept the 2×2 layout, WidgetState data contract, provider registrations, and refresh pipeline unchanged.

## 0.3.2 - 2026-09-02

### Device experience

- Improved App and Widget text resource usage and accessibility descriptions for status, refresh, source, loading, and cached-error states.
- Reflowed App metadata and action controls so large system fonts do not force time fields or buttons into a crowded horizontal row.
- Made the App-only Reset Pulse timeline scale its labels with the system font size while retaining the readable history summary.
- Updated the 2×2 Widget to show both 24H and 48H probabilities when data is fresh, while stale data remains unavailable instead of reusing old percentages.
- Added two additional 4×2 launcher choices: the state-focused Pulse Orb and the compact Command Deck.
- Added static state-colored orb visuals, separate probability metrics, concise real-history statistics, and 48dp refresh hit areas without changing the cache or refresh protocol.
- Added a Samsung / One UI device QA matrix for widget sizing, font scaling, refresh states, stale/offline states, notifications, and accessibility.

## 0.3.1 - 2026-08-31

### App UI and Widget

- Promoted the dark, compact, information-first UI redesign to the stable release.
- Made the current reset state the primary App detail and Widget content, with local-time timestamps, source attribution, and stale/unknown wording kept explicit.
- Refined the 4×2 and 2×2 XML Widgets with a stable dark surface, clearer hierarchy, compact metrics, and a deterministic refresh loading state.
- Preserved the immediate refresh flow and the “正在刷新…” feedback without depending on continuous Launcher-side animation.

### Reset Pulse and notifications

- Added the App-only Reset Pulse timeline using up to seven real recent broad completed reset events.
- Added a trailing 7-day count, recent average interval, and explicitly defined recent cadence streak, including an accessible text summary and an empty state for insufficient history.
- Added persistent notification fingerprints so the same scheduled/confirmed or completed reset is not re-alerted after metadata changes or process restarts.
- Kept “very likely” notifications optional and disabled by default.

### Updates and release safety

- Added the secure in-app updater for the public JaynOwO/tibo-button stable GitHub Releases endpoint.
- Added stable-tag, exact APK asset, SHA-256, package ID, version, and signing-certificate verification before handing an APK to Android’s installer.
- Added app-private update caching, scoped FileProvider delivery, unknown-source permission handling, download progress, and explicit system installation confirmation.
- Automatic update checks remain opt-in by setting but enabled by default; automatic download and installation are never performed.
- Added the signed release workflow with tests, tag/version validation, APK signature verification, checksums, and GitHub Release assets.

## 0.3.0 - Development-only build

- Added **Reset Pulse** with the seven most recent broad completed reset records.
- Added a trailing 7-day reset count, average recent reset interval, and an explicitly defined recent cadence streak (consecutive broad completed events no more than 72 hours apart).
- Added plain-language status explanations for confirmed, likely, possible, low, unlikely, stale, and unknown states.
- Added persistent notification fingerprints so the same confirmed/completed signal is not re-alerted just because evidence metadata changes or a process restarts.
- Added a compact 7-day reset count to the 4×2 widget without making the widget denser elsewhere.
- Improved the notification settings copy to explain defaults and deduplication behavior.
- Updated the public README with badges, Reset Pulse semantics, and direct stable-release discovery.
- Bumped Android versionCode to 4 and versionName to 0.3.0.

## 0.2.1 - 2026-08-30

- Replaced the flipped loading glyph with a genuinely rotating refresh-arrow animation.
- Implemented the widget animation with a supported indeterminate ProgressBar and level-driven RotateDrawable, avoiding rapid repeated widget updates.
- Added stable release-signing support through environment variables.
- Added a GitHub Actions signed-release workflow with unit tests, tag/version validation, APK signature verification, SHA-256 checksums, and automatic GitHub Release assets.
- Added a PowerShell helper for generating the one-time release keystore without placing passwords in source files.
- Added release documentation and expanded public-repository secret protections.
- Bumped Android versionCode to 3 and versionName to 0.2.1.

## 0.2.0 - 2026-08-30

- Added immediate widget refresh feedback with a deterministic loading-state icon.
- Added Android notification permission/settings UI.
- Added optional notifications for confirmed schedules, newly completed broad resets, and very-likely status.
- Added a richer in-app detail card with Reset Beacon canonical answer text and evidence link.
- Added parsing for the documented answer.deadline field when available.
- Added MIT license, independence/trademark disclaimer, Reset Beacon attribution, and privacy notes.
- Updated GitHub Actions checkout/setup-java actions to v5.
- Updated the Gradle setup action to v6 to remove the Node.js 20 deprecation warning.
- Updated the artifact upload action to v6 for the same Node.js 24 runtime.
- Added a unit-test step before the APK build in GitHub Actions.
- Bumped Android versionCode to 2 and versionName to 0.2.0.
