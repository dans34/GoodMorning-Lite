# GoodMorning-Lite

A local-first Android habit and routine planner inspired by the user's analysis of Routinery (`com.alt.goodmorning`). The goal is to preserve a familiar visual structure while simplifying the experience.

## Project aims

- Closely approximate the original app's appearance based on documented screens and user-provided screenshots.
- Retain core habit tracking, routine planning, focus/timing, progress history, and reminders where feasible.
- Store routines, habits, schedules, completions and progress locally; no account required.
- Use local notifications for reminders where Android permits.
- Exclude widgets/quick launch, social/sharing, AI/chat, accounts/cloud sync, subscriptions/paywalls, advertising, attribution and analytics.

## Status

**Research and analysis stage — no replacement app implementation yet.**

Initial XAPK inventory and static component indicators are recorded in [the initial analysis report](docs/findings/2026-10-14-initial-apk-analysis.md). Findings are preliminary until the relevant flows are mapped and tested.

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

## Build status

No APK build has been produced yet.
