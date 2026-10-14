# Changelog

## 2026-10-14 — Initial repository documentation

- Added README with project goals, status, scope and milestones.
- Added initial static APK analysis notes for Routinery version 3.30.0.
- Recorded the XAPK split inventory and high-level technology/dependency indicators.
- No clone source implementation or build artifact yet.

## 2026-10-14 — Initial APK analysis completed

- Added a feature and dependency inventory based on package metadata and extracted bundle strings.
- Classified routine planning, routine execution, habits/streaks, timers, reminders and basic progress as core local-first clone scope.
- Marked accounts, subscriptions, social/sharing and remote AI analysis for removal or deferral.
- Documented the declared permission surface and a minimal-permission approach for the replacement.
- Explicitly recorded which findings remain unverified until runtime screen walkthroughs and device tests.

## 2026-10-14 — Scope exclusions confirmed

- Explicitly excluded widgets and quick-launch features.
- Explicitly excluded social, friends and sharing.
- Explicitly excluded AI analysis/chat and related service integrations.
- Explicitly excluded accounts, login and cloud sync.
- Explicitly excluded subscriptions, paywalls and billing.
- Explicitly excluded advertising, attribution and analytics SDKs.
- Kept the product focus on local routines, habits, timers, reminders and locally calculated progress.

## 2026-10-14 — Core screen and navigation map

- Added a screen inventory for launch/setup, Today/routine list, routine details/editor, active timer, completion, habits, progress, reminders and settings.
- Added proposed navigation diagram and four core user journeys.
- Marked routes as static-evidence-backed or inferred; runtime screen order and exact UI remain to be verified.
- Kept all six previously agreed feature groups excluded.

## 2026-10-14 — Onboarding order corrected

- Updated the screenshot notes to reflect the user-confirmed sequence: choose morning/evening routine, choose struggle, set wake-up time, preview routine, then the original paywall screen.
- GoodMorning-Lite omits step 5 and proceeds into the app.

## 2026-10-14 — Android starter project committed

- Added Gradle Kotlin DSL project configuration and Android manifest.
- Added a native Java starter activity with onboarding in the user-confirmed order: choose routine, choose struggle, set wake-up time, preview routine.
- Added local SharedPreferences persistence for setup choices and a basic routine home screen.
- Deliberately omitted the trial/paywall screen and all excluded SDK/service integrations.
- Added `.gitignore` for local Android Studio and build output.
- Build not yet verified; routine execution/timer is not implemented yet.

## 2026-10-14 — Routine timer and task progression

- Replaced the Start routine placeholder with a task-by-task timer screen.
- Added a two-minute starter duration per sample task, live countdown, pause/resume, complete-task and skip-task actions.
- Added end-routine action, completion screen, task completion checkmarks and restart flow.
- Persisted current task index, remaining timer duration, completion flags and routine completion locally.
- Source is committed; Android/Gradle build and real-device timing behaviour remain unverified.


## 2026-10-14 — Timer lifecycle hardening

- Starting a routine now starts the countdown immediately instead of opening a paused timer.
- Backgrounding the app pauses the timer and saves the remaining time, avoiding countdown drift while the activity is not visible.
- Returning to the app restores the active task timer screen in a paused state, ready to resume.
- Ending or finishing a routine clears the active-timer state.
- Added a GitHub Actions workflow to compile a debug APK on pushes and pull requests, then upload the APK as a workflow artifact.
- Build and device behaviour still require confirmation from the workflow and hands-on testing.
