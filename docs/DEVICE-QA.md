# Tibo Button Device QA

This checklist covers the device-level behavior that cannot be proven by the JVM test suite or by a successful APK build. It is intended for the Samsung / One UI device used for the Tibo Button acceptance screenshots.

## Preconditions

- Install the debug APK from the `Build Android APK` artifact, or install the current signed stable APK when checking upgrade behavior.
- Add the standard 4×2 and 2×2 widgets plus Pulse Orb and Command Deck to the home screen when the launcher supports all four choices.
- Use a wallpaper with both dark and bright areas behind the widgets.
- Record the device model, One UI version, Android version, display size, and font size with the result.

## Layout and typography matrix

| Scenario | Standard 4×2 | Standard 2×2 | Pulse Orb | Command Deck | App details |
| --- | --- | --- | --- | --- | --- |
| Default display and font | No overlap, status is primary, both probabilities visible | Status, next reset, 24H / 48H probabilities and update footer remain readable | Orb, status, next reset and probabilities are clear | Next reset, two metric blocks and statistics are clear | Status card is first; buttons have clear hierarchy |
| Large font | Text remains identifiable and does not cover the refresh control | No clipped status or probability text that changes its meaning | Orb remains distinct; text stays readable beside it | Metric values and next reset do not overlap | Time metadata, buttons, Pulse summary and history wrap without collision |
| Largest practical font | Widget still has an understandable status and fallback text | No unreadable multi-line overflow | Status and fallback text remain understandable | Status and fallback text remain understandable | Timeline remains supplementary; text history remains understandable |
| Bright/complex wallpaper | Opaque surface and border preserve contrast | Same | Halo remains decorative while text remains legible | Metric outlines remain legible | Cards and status colors remain distinguishable without relying on color alone |

## State and interaction matrix

- Fresh confirmed/scheduled state: show the status, machine-readable time when available, or `已排期 · 时间见来源` when it is not.
- Likely/possible state: show the probability values and do not imply a confirmed schedule.
- Stale forecast: mark the data stale and do not present old probabilities as current.
- No data: show an explicit unknown state without invented time or probability.
- Offline refresh failure: retain the last successful cache, show the failure indication, and keep the source/update time understandable.
- Widget refresh: test all four variants, tap the refresh control and verify the deterministic loading icon plus `正在刷新…`; tap again while loading and verify there is no visible duplicate animation or broken layout; confirm normal state returns after success or failure.
- Widget installation: add each 4×2 variant and confirm One UI does not show `无法添加微件` / `Unable to add widget`; the 2×2 variant must remain addable as a control comparison.
- App refresh: verify the button disables immediately, shows loading feedback, and returns to the normal label after WorkManager finishes or retries.
- Notifications: verify permission/settings behavior, scheduled/confirmed and completed notifications, optional likely notifications, and no repeated notification for the same event.
- Accessibility: use TalkBack or the device accessibility inspector and verify the status summary, refresh action, loading state, Pulse Orb status core, Command Deck probability metrics, Pulse summary, and source action have meaningful descriptions.

## Result recording

Record each item as `PASS`, `FAIL`, or `NOT TESTED`, with a screenshot and a short note for every failure. A green CI run proves tests and APK construction only; it does not replace this device review.
