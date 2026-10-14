# Architecture notes (initial)

## Current evidence

The reference app is a React Native application with a Hermes JavaScript bundle and Android native integrations. The supplied XAPK includes platform/resource splits. Static strings and dependencies indicate routines, habits, timer screens, calendar functionality, reminders, analysis, account flows and subscriptions.

## Clone direction

The replacement will be a **separate implementation**, not a modified build of the original package. Starting direction is an Android Studio project with local persistence and narrowly scoped Android permissions.

### Core modules to design

- **Routine model:** ordered routine steps, estimated duration and schedule.
- **Habit model:** habit title, schedule and completion history.
- **Timer engine:** step countdown, pause/resume/skip, elapsed-time tracking.
- **Reminder scheduler:** local notifications, rescheduling after reboot and permission handling.
- **Repository/storage layer:** on-device data with export/backup considered later.
- **Screens:** home/today, routine editor, active routine, habit list/history, insights, settings.

## Visual fidelity

Do not infer a pixel-exact design from bundled resource names. Gather screenshots from the app at a known version and screen size, then record colors, spacing, typography, iconography, component states and navigation. Implement original UI code from those observations; do not include the original app's proprietary code or bundled assets without permission.

## Account and billing simplification

The replacement should have no mandatory identity provider, account creation, paywall, subscription SDK or Play Billing dependency. Features that are fundamentally cloud-backed in the reference should be replaced by local equivalents where feasible, or documented as intentionally omitted. Do not simply skip server authorization checks in the original app.
