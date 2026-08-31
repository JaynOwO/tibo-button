# ⚡ Tibo Button

[![Latest release](https://img.shields.io/github/v/release/JaynOwO/tibo-button?display_name=tag)](https://github.com/JaynOwO/tibo-button/releases/latest)
[![Android CI](https://github.com/JaynOwO/tibo-button/actions/workflows/build-apk.yml/badge.svg)](https://github.com/JaynOwO/tibo-button/actions/workflows/build-apk.yml)
[![License](https://img.shields.io/github/license/JaynOwO/tibo-button)](LICENSE)

Unofficial Android home-screen widgets for tracking public signals about extra shared Codex / ChatGPT Work usage-limit resets.

> **Independent project.** Tibo Button is not affiliated with, endorsed by, or an official product of OpenAI, X Corp., Reset Beacon / Sound Media, or Thibault “Tibo” Sottiaux.

**Stable users:** [Download the latest signed APK from GitHub Releases](https://github.com/JaynOwO/tibo-button/releases/latest).

## v0.3.1 (testing)

- 4×2 and 2×2 Android widgets.
- 24h / 48h Reset Beacon probabilities.
- Last recorded broad reset.
- Scheduled next reset and local-time countdown when a machine-readable deadline is available.
- Manual refresh with an immediate, genuinely rotating refresh-arrow animation and “正在刷新…” feedback.
- Optional Android notifications for:
  - a confirmed/scheduled reset,
  - a newly recorded completed reset,
  - “very likely” status (off by default).
- In-app detail view with Reset Beacon’s canonical answer text when available, plus a link to the strongest current evidence.
- Reset Pulse: the seven most recent broad completed resets, a trailing 7-day count, recent average interval, and an explicitly defined cadence streak.
- Secure in-app updater for future signed GitHub Releases; automatic checks never download or install by themselves.
- 15-minute WorkManager refresh request; Android may defer background work for battery reasons.
- Signed GitHub Release pipeline for installable, in-place-upgradable public APKs.

## Reset Pulse rules

Reset Pulse deliberately avoids fuzzy history math:

- only Reset Beacon events marked `completed` are counted;
- only broad/all-user scopes are counted;
- the history card shows up to the latest seven qualifying events;
- “7-day count” means qualifying events announced in the last seven days;
- “recent streak” means the run starting at the newest event where every adjacent qualifying reset is no more than **72 hours** apart;
- average interval is calculated across the displayed recent qualifying events.

These are descriptive activity metrics, not a promise that another reset will occur.

## Install

After the first signed release is published:

1. Open the repository’s **Releases** page.
2. Download `TiboButton-vX.Y.Z.apk`.
3. Optionally verify it against `SHA256SUMS.txt`.
4. Install the APK on Android and add either Tibo Button widget from the launcher’s widget picker.

Latest release:

```text
https://github.com/JaynOwO/tibo-button/releases/latest
```

Published release APKs are signed with one stable project key. Keep that installed release line separate from ad-hoc debug APKs, whose signatures can differ between build machines.

## In-app updates

v0.3.1 is expected to be the final manual APK update. After a later signed release is published, the app can check the official public GitHub Release and offer a user-confirmed update.

Before Android's installer is opened, the updater requires all of the following:

- a stable `vX.Y.Z` release from `JaynOwO/tibo-button`;
- the exact `TiboButton-vX.Y.Z.apk` asset;
- a matching SHA-256 digest from the GitHub asset or `SHA256SUMS.txt`;
- matching `com.tibobutton.app` package ID and versionName/versionCode;
- a signing certificate matching the currently installed app.

The APK is kept in the app-private `cache/updates/` directory and handed to Android through a narrowly scoped FileProvider. Android's final installation confirmation always remains visible. If a check fails, the APK is deleted and installation stops. No GitHub login, token, or API key is required. Debug builds may correctly refuse to update a stable release because their signing certificate is intentionally different.

## Data source and attribution

The app reads Reset Beacon’s documented public JSON endpoints:

- `GET https://resetbeacon.com/api/forecast`
- `GET https://resetbeacon.com/api/history`

Reset Beacon’s terms permit reuse of its classification record with attribution and a link back, and describe its unauthenticated JSON API as suitable for dashboard/bot-style fair use. This app attributes Reset Beacon in the widget and links users back to the source/evidence.

Archived or quoted public-post text remains the property of its original author. This repository does not claim ownership of that material.

## Trademarks

Codex, ChatGPT, and OpenAI are trademarks of OpenAI. They are referenced only to identify the subject being tracked. No OpenAI logo or other brand artwork is included.

“Tibo” is used descriptively to identify the public source account whose reset-related announcements are being tracked. The project does not imply endorsement by Thibault Sottiaux.

## Privacy

Tibo Button does not ask for an OpenAI login, X login, API key, or your Codex usage data. Widget state, cached history summaries, notification dedup fingerprints, and notification preferences are stored locally on the Android device. The app makes public GET requests to Reset Beacon.

## Development build

The repository includes `.github/workflows/build-apk.yml`.

1. Open **Actions** → **Build Android APK**.
2. Run the workflow.
3. Download the `TiboButton-debug-apk` artifact.
4. Extract `app-debug.apk` and install it on Android.

Debug builds are development artifacts and may not upgrade over builds signed elsewhere.

## Signed release process

The signed release workflow is:

```text
.github/workflows/release.yml
```

It can perform a manual dry run, or publish a GitHub Release when a matching `vX.Y.Z` tag is pushed. Signing credentials stay in GitHub Actions secrets and are never committed.

See [docs/RELEASING.md](docs/RELEASING.md) for the one-time keystore setup, dry-run procedure, and release checklist.

## Open-source publishing checklist

Keep the non-affiliation notice visible, keep Reset Beacon attribution and source links, and avoid using OpenAI logos, Tibo’s portrait/avatar, or copied post archives as project branding.

Recommended repository description:

> Unofficial Android widgets for tracking public Codex / ChatGPT Work shared-reset signals from Reset Beacon.

## License

Source code in this repository is released under the [MIT License](LICENSE), except third-party material and trademarks, which remain subject to their respective owners’ rights. See [NOTICE.md](NOTICE.md).
