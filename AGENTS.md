# AGENTS.md

## Project Overview

**Tibo Button** is an unofficial Android home-screen widget and companion app for tracking publicly available Codex / ChatGPT Work usage-reset signals associated with Tibo (Thibault Sottiaux) and Reset Beacon data.

The repository is public and intended to remain lightweight, transparent, privacy-friendly, and easy to build.

Primary goals:

- Show the latest confirmed or predicted reset status at a glance.
- Show the last known reset time.
- Show the next scheduled reset time when one is explicitly available.
- Show 24h / 48h reset probability when the upstream data is fresh.
- Give immediate visual feedback when the user manually refreshes.
- Optionally notify the user when a reset becomes scheduled, confirmed, or completed.
- Remain safe to publish as an unofficial open-source project.

This is **not** an official OpenAI, ChatGPT, Codex, Reset Beacon, or Tibo product.

---

## Repository / Tech Stack

- Platform: Android
- Language: Kotlin
- Build system: Gradle
- UI: Android XML layouts + App Widgets
- Background work: WorkManager
- Upstream data: Reset Beacon public API
- CI: GitHub Actions
- Default branch: `main`

Important existing areas include:

- `app/src/main/java/com/tibobutton/app/`
- `app/src/main/java/com/tibobutton/app/data/`
- `app/src/main/java/com/tibobutton/app/widget/`
- `app/src/main/java/com/tibobutton/app/work/`
- `app/src/main/res/layout/`
- `app/src/main/res/xml/`
- `app/src/test/`
- `.github/workflows/build-apk.yml`

Do not restructure the entire project without a clear technical reason.

---

## Product Principles

### 1. The widget is the primary product

The home-screen widget is more important than the in-app screen.

Changes should prioritize:

- glanceability
- fast refresh feedback
- low battery usage
- readable typography
- correct local-time display
- graceful stale/offline states

Do not turn the widget into a dense dashboard.

### 2. Never invent certainty

The app must clearly distinguish between:

- no meaningful signal
- possible
- likely
- very likely
- scheduled / confirmed
- completed reset
- stale / unavailable data

Never infer an exact reset time from vague text.

If upstream data does not provide a reliable machine-readable scheduled time, prefer wording such as:

> 已排期 · 时间见来源

instead of guessing.

### 3. Stale data must look stale

Do not display old probabilities as if they are current.

If forecast data is expired or invalid:

- hide or mark the percentage as stale/unavailable
- keep the last known confirmed event only if clearly labeled
- do not silently reuse old predictions

### 4. Local time first

All user-facing timestamps should be converted to the device's local timezone unless a source timezone is explicitly being shown for context.

Do not hard-code New York, Pacific, UTC, or China time as the default UI timezone.

---

## Manual Refresh UX

Manual refresh must provide immediate visible feedback.

Expected behavior:

1. User taps the widget refresh control.
2. The widget immediately changes to a loading / refreshing state.
3. The footer shows text such as `正在刷新…`.
4. The refresh control visually changes while work is in progress.
5. The network refresh runs.
6. The widget returns to its normal state on success or failure.

Important:

- Do not rely on fragile continuous launcher-side animation.
- Android launchers differ in how they update RemoteViews.
- Prefer a deterministic loading-state swap over a fancy animation that may not render.
- Avoid duplicate parallel refresh jobs from repeated rapid taps when practical.

---

## Notification Rules

Notifications should be useful, not noisy.

Default behavior:

- Scheduled / confirmed reset: notification enabled.
- Completed reset: notification enabled.
- "Very likely" / high-probability forecast: optional and disabled by default.

Requirements:

- Deduplicate notifications for the same upstream event.
- Do not notify repeatedly on every WorkManager refresh.
- Persist enough event identity/state to know whether a notification has already been sent.
- If the upstream event changes materially, a new notification may be appropriate.
- Notification text must not imply official OpenAI confirmation unless the underlying source actually supports that wording.

---

## Reset Beacon Integration

Reset Beacon is the primary structured data source.

General rules:

- Use public documented endpoints only.
- Do not require users to provide API keys unless the upstream service later explicitly requires them.
- Preserve attribution to Reset Beacon in the app / widget / documentation where appropriate.
- Link users to the source rather than copying large amounts of source text.
- Handle network errors gracefully.
- Handle malformed or missing fields defensively.
- Prefer structured fields over scraping prose.

If the upstream API changes:

1. Inspect the new response.
2. Update models/parsing narrowly.
3. Add or update tests.
4. Preserve backward-safe behavior where reasonable.
5. Never "fix" an API mismatch by hard-coding current event values.

---

## Open-Source / Branding Rules

The project must remain clearly unofficial.

Do not:

- use the OpenAI logo as the app logo
- use Tibo's portrait/avatar as the app logo
- imply sponsorship, endorsement, affiliation, or official status
- name the app in a way that makes it appear to be an official ChatGPT/Codex application
- commit copyrighted source screenshots or copied post archives unnecessarily

Keep or improve a visible disclaimer similar to:

> This is an unofficial independent project and is not affiliated with, sponsored by, or endorsed by OpenAI, ChatGPT, Codex, Reset Beacon, or Thibault "Tibo" Sottiaux.

Trademark names may be referenced descriptively where needed.

Preserve Reset Beacon attribution.

---

## Secrets / Public Repository Safety

This repository is public.

Never commit:

- API keys
- access tokens
- passwords
- `.env` secrets
- private signing keys
- `.jks` / `.keystore` files
- private certificates
- service-account credentials
- `local.properties`
- personal authentication cookies

Before adding a new service, ask whether it requires a secret and whether that secret can safely remain outside the repository.

Use GitHub Secrets for CI credentials if release signing is added later.

---

## Code Change Policy

Prefer small, understandable changes.

Before editing:

1. Read the relevant existing files.
2. Understand current state flow.
3. Reuse existing patterns where reasonable.
4. Avoid replacing working code just to use a different style.

When modifying data/state behavior, inspect at minimum:

- models
- API parsing
- classifier / status logic
- preferences/cache
- widget renderer
- refresh worker
- tests

When modifying widget UI, inspect both the Kotlin renderer and corresponding XML layout/resources.

Do not delete working behavior unless the requested change explicitly replaces it.

---

## Testing Requirements

At minimum, for meaningful code changes run:

```bash
gradle test
```

and:

```bash
gradle :app:assembleDebug
```

If project wrappers are added later, prefer:

```bash
./gradlew test
./gradlew :app:assembleDebug
```

Add or update unit tests for:

- status classification
- stale forecast behavior
- scheduled reset precedence
- probability thresholds
- notification dedup logic when practical
- parsing edge cases when API fields change

A green compile is not enough if business logic changed.

---

## GitHub Actions

The repository contains:

```text
.github/workflows/build-apk.yml
```

Expected CI behavior:

- build on relevant pushes
- build a debug APK
- upload the APK as a workflow artifact

Do not remove CI without explicit instruction.

If GitHub Actions fails:

1. Open the failed workflow run.
2. Read the failing job and exact failing step.
3. Fix the root cause.
4. Commit and push again.
5. Repeat until the workflow succeeds.

Do not stop after saying "CI failed" if the failure is actionable.

---

## Commit / Push Workflow

For user-requested implementation work, unless the user explicitly asks for local-only changes:

1. Inspect `main`.
2. Confirm the worktree is clean or understand existing changes.
3. Implement the requested change.
4. Run tests.
5. Run `:app:assembleDebug`.
6. Review the diff.
7. Commit with a concise descriptive message.
8. Push to `origin/main`.
9. Verify GitHub Actions.
10. If CI fails, inspect logs and fix it.
11. Report the final commit SHA and CI result.

Do not create meaningless commits just to trigger CI.

---

## Versioning

When a user refers to a release such as `v0.2`, `v0.3`, etc.:

- keep version naming consistent in documentation
- update Android version metadata when appropriate
- do not silently call an unverified build a release
- distinguish source state, successful CI build, and published GitHub Release

A successful debug artifact is not automatically a signed production release.

---

## APK / Signing

Current development builds may use debug signing.

Be aware:

- Debug APK signatures may differ between environments.
- A user may be unable to install one debug build over another if signing keys differ.

If proper public releases are introduced:

- use a stable release signing key
- never commit the private signing key
- store signing secrets securely
- generate signed release APK/AAB through CI only after the release process is explicitly configured

---

## Performance / Battery

This app should remain lightweight.

Avoid:

- overly frequent background polling
- exact alarms unless truly necessary
- second-by-second background widget updates
- unnecessary wake locks
- repeated network calls caused by rendering

WorkManager timing is not exact.

A 15-minute periodic schedule means "approximately / system-managed", not a guaranteed refresh every exactly 15 minutes.

Manual refresh should remain available.

---

## UI Style

Current visual direction:

- dark
- clean
- compact
- modern
- information-first
- restrained accent colors
- readable on a home screen at a glance

Status color can reinforce meaning, but text must remain understandable without relying only on color.

Do not introduce cluttered cards, excessive gradients, decorative charts, or tiny text unless explicitly requested.

---

## Language

The current user-facing experience is primarily Simplified Chinese.

When adding new user-visible strings:

- keep Chinese wording concise and natural
- avoid machine-translated phrasing
- keep English technical/product names where appropriate
- place reusable strings in Android resources where reasonable

Do not hard-code large amounts of user-facing text in Kotlin when it belongs in `strings.xml`.

---

## When Requirements Are Ambiguous

If a requested change is small and the likely interpretation is safe, implement the reasonable interpretation.

Ask before proceeding when ambiguity could affect:

- destructive data behavior
- public release behavior
- signing credentials
- notification spam
- privacy
- branding/legal positioning
- major architecture changes

Do not ask unnecessary questions for routine implementation details.

---

## Definition of Done

A change is considered done only when applicable items below are complete:

- requested behavior implemented
- existing behavior not unintentionally broken
- relevant tests pass
- debug APK builds
- public-repository safety preserved
- no secrets introduced
- documentation updated if needed
- code committed
- code pushed
- GitHub Actions checked
- final status reported clearly

Final report should include:

- summary of changes
- important files changed
- test commands and results
- build result
- commit SHA
- GitHub Actions status
- artifact name if produced
- any remaining manual action
