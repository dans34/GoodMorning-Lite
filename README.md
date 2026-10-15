# GoodMorning-Lite

A local-first Android habit and routine planner inspired by the user's analysis of Routinery (`com.alt.goodmorning`). The goal is to preserve a familiar visual structure while simplifying the experience.

## Project aims

- Closely approximate the original app's appearance based on documented screens and user-provided screenshots.
- Retain core habit tracking, routine planning, focus/timing, progress history, and reminders where feasible.
- Store routines, habits, schedules, completions and progress locally; no account required.
- Use local notifications for reminders where Android permits.
- Exclude widgets/quick launch, social/sharing, AI/chat, accounts/cloud sync, subscriptions/paywalls, advertising, attribution and analytics.

## Status

**V1 Alpha — working baseline tested on a Nokia C12.**

The current alpha includes local routine management, task timers, partial-session support through “Just get started”, routine history, full-completion streaks and a gentle rolling seven-day consistency measure. The user has confirmed the latest “Just get started” and History & Streaks changes work on the Nokia C12. This is an early alpha, not a final release; continue testing as features are added.

Initial XAPK inventory and static component indicators are recorded in [the initial analysis report](docs/findings/2026-10-14-initial-apk-analysis.md). The [core screen map](docs/navigation/core-screen-map.md) and [onboarding screenshot notes](docs/navigation/onboarding-screenshot-notes.md) capture the known navigation and visual reference.

## Explicit exclusions

GoodMorning-Lite will not include home-screen/lock-screen widgets or quick-launch shortcuts; sharing, social or friend features; AI analysis/chat; login, accounts or cloud sync; subscription/paywall/billing flows; or advertising, attribution and analytics SDKs. These are excluded from the project, not just deferred.

## Scope and provenance

This repository is for an independent replacement implementation informed by analysis of the provided APK. Do not add the original APK/XAPK, extracted proprietary source, credentials, private user data, or signing keys to Git. Keep original binary artifacts locally and document hashes only when needed.

## Planned structure

- `app/` — Android Studio application source
- `docs/findings/` — dated analysis reports
- `docs/screenshots/` — reference screenshots supplied for UI comparison
- `docs/architecture/` — design decisions and local/remote dependency notes
- `CHANGELOG.md` — project milestones and changes

## Development milestones

1. Verify package identity and record archive metadata.
2. Map original screens, navigation, design tokens and local/remote dependencies.
3. Define an independent Android architecture.
4. Implement the main dashboard and routine flows.
5. Implement persistent habits, timers, local reminders and progress history.
6. Compare screens and behaviour against references, then refine.

## Implementation status

The native Android Studio project under `app/` includes onboarding, local routine/task management, task timers, pause/resume, complete/skip/end controls, a completion summary, routine history, streaks and seven-day consistency tracking. “Just get started” lets the user complete one task and then either finish for now or continue. The trial/paywall step and the other explicitly excluded online/social/commercial features remain omitted.

**Version:** `1.0-alpha` (V1 Alpha). The user has confirmed the latest feature changes work on a Nokia C12. The assistant has not independently verified a fresh Gradle build for this version; build the current source in Android Studio before distributing an APK. See `CHANGELOG.md` for the milestone history.
