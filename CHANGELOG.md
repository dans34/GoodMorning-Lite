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

## 2026-10-15 — Home screen, routine management and timer polish

- Made the home screen the default destination when the app launches after setup.
- Moved the routine selector and task list into a clearer Today screen layout; kept the existing warm, minimal visual style.
- Added a task library with categories, descriptions, suggested durations and quick-add controls.
- Added custom task creation and task editing, including task name, duration, optional notes and a task-specific emoji.
- Persisted task IDs and emoji selections so editing a task preserves its identity and icon.
- Added a compact task-card layout on the home screen with task icons, durations and completion indicators.
- Kept home task cards display-only; task editing is accessed through Manage Routines.
- Improved Manage Routines with Morning/Evening/Night selection, compact task rows, a separate delete control with confirmation, and long-press drag-and-drop reordering within the selected routine.
- Added drag-list auto-scroll and reduced spacing between routine task rows.
- Added task count and estimated combined duration to the selected routine in Manage Routines.
- Unified custom task creation and editing so both use the same editor.
- Removed the task action menu's Edit/Move up/Move down/Delete options; tapping a task now opens the editor directly, while deletion remains available via the separate bin icon and drag-and-drop remains available for reordering.
- Improved the task editor with explicit labels for Task Name, Duration (Minutes), Notes (Optional) and Task Icon.
- Replaced the long emoji selection dialog with a compact six-column icon grid and selected-icon preview.
- Added local routine history and completion streaks, plus a History shortcut in the home header.
- Made history session entries read-only and improved the history icon.
- Refined the active timer screen with a large pale timer circle, task emoji, progress bar, task notes and four equal-sized controls: Pause/Resume, Complete, Skip and End. Ending a routine requires confirmation.
- Added task completion feedback and a full-routine completion summary.
- Experimented with circle-pop and confetti celebrations, but the effect did not render reliably on the Nokia C12. Shelved and removed that animation code; task completion now advances without the custom celebration or toast.
- Added a small credits easter egg with a personal message.
- Improved shared screen scrolling and tightened header spacing while preserving the approved home-screen design.
- Fixed Java source issues encountered during iteration, including a missing `GridLayout` import, a lambda button-initialisation error, and a leftover reference to the shelved celebration code.
- Android Studio build and device behaviour should be verified after each latest change; changes have been committed to the repository, but not independently compiled by the assistant.

## 2026-10-15 — Weekly history overview

- Redesigned History & streaks with a highlighted current streak summary.
- Added this-week metrics for fully completed routines, sessions recorded and task completion rate.
- Added a seven-day activity row and clearer individual session cards showing routine, date, task count and elapsed time.
- Kept history stored locally and reused the existing session records; no new permissions or online services added.
- Build and Nokia C12 behaviour still need to be checked in Android Studio.

## 2026-10-15 — Just get started mode

- Added a secondary “Just get started” action on Today, alongside the regular Start routine button.
- Starts at the first unfinished task and, after that task is completed, offers “Finish for now” or “Continue routine”.
- Finishing for now uses the existing completion summary and saves a partial session to local history; completed task flags are preserved.
- Persisted the mode while the task timer is active so it survives ordinary activity recreation.
- Source committed; Android Studio build and Nokia C12 behaviour still need user verification.

## 2026-10-15 — Gentle consistency tracking

- Added a rolling seven-day progress measure to History & streaks, counting calendar days with at least one completed task in a saved session.
- Kept the existing full-completion streak unchanged; partial sessions contribute to consistency without being treated as fully completed routines.
- Added supportive wording: “Every fresh start counts.”
- Build and Nokia C12 behaviour still need user verification.

## 2026-10-15 — V1 Alpha baseline

- Named the current working milestone **V1 Alpha**.
- Set the Android app version name to `1.0-alpha` (version code remains 1).
- Updated the README to describe the current feature set and the user-confirmed Nokia C12 test status.
- This is an alpha baseline, not a claim that a fresh build or formal GitHub release has been verified.
