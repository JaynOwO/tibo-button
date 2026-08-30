# ⚡ Tibo Button

Unofficial Android home-screen widgets for tracking public signals about extra shared Codex / ChatGPT Work usage-limit resets.

> **Independent project.** Tibo Button is not affiliated with, endorsed by, or an official product of OpenAI, X Corp., Reset Beacon / Sound Media, or Thibault “Tibo” Sottiaux.

## v0.2

- 4×2 and 2×2 Android widgets.
- 24h / 48h Reset Beacon probabilities.
- Last recorded broad reset.
- Scheduled next reset and local-time countdown when a machine-readable deadline is available.
- Manual refresh with an immediate deterministic loading-state icon and “正在刷新…” feedback.
- Optional Android notifications for:
  - a confirmed/scheduled reset,
  - a newly recorded completed reset,
  - “very likely” status (off by default).
- In-app detail view with Reset Beacon’s canonical answer text when available, plus a link to the strongest current evidence.
- The app deliberately avoids republishing archived public-post text in the interface; it links to the evidence instead.
- 15-minute WorkManager refresh request; Android may defer background work for battery reasons.

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

Tibo Button does not ask for an OpenAI login, X login, API key, or your Codex usage data. Widget state and notification preferences are stored locally on the Android device. The app makes public GET requests to Reset Beacon.

## Build

The repository includes `.github/workflows/build-apk.yml`.

1. Open **Actions** → **Build Android APK**.
2. Run the workflow.
3. Download the `TiboButton-debug-apk` artifact.
4. Extract `app-debug.apk` and install it on Android.

> Debug APKs built on fresh GitHub-hosted runners may use different debug signing keys between runs. Until a stable release-signing key is configured, you may need to uninstall an older debug build before installing a newer one.

## Open-source publishing checklist

This repository is ready to be public on GitHub. Recommended repository description:

> Unofficial Android widgets for tracking public Codex / ChatGPT Work shared-reset signals from Reset Beacon.

Keep the non-affiliation notice visible, keep Reset Beacon attribution and source links, and avoid using OpenAI logos, Tibo’s portrait/avatar, or copied post archives as project branding.

## License

Source code in this repository is released under the [MIT License](LICENSE), except third-party material and trademarks, which remain subject to their respective owners’ rights. See [NOTICE.md](NOTICE.md).
