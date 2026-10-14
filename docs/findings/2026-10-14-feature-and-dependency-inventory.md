# APK Analysis Milestone 1 — Feature and Dependency Inventory

**Date:** 2026-10-14  
**Project:** GoodMorning-Lite  
**Reference:** Routinery 3.30.0, package `com.alt.goodmorning`  
**Evidence level:** Static archive, manifest and extracted-string inspection. This is not a runtime-verified screen map.

## Outcome

The supplied XAPK has been inventoried and its base package and major technology indicators identified. Bundle string extraction gives evidence for feature groups and operational constraints. This completes the initial static-analysis milestone; screen layouts, navigation order, and exact online/offline behaviour remain open until runtime screenshots and interaction tests are available.

## Artifact identity

| Item | Finding |
|---|---|
| App label | Routinery |
| Package | `com.alt.goodmorning` |
| Version / version code | `3.30.0` / `1067754` |
| Minimum / target SDK | 24 (Android 7.0) / 36 |
| XAPK format | 2 |
| Archive entries | 22 |
| Base APK | approx. 98.6 MB |
| ARM64 ABI split | approx. 14.5 MB |
| Base DEX files | 6 |
| React Native bundle | approx. 17.17 MB |
| Runtime indicators | React Native and Hermes |
| Font-family indicators | Montserrat, Pretendard |

The XAPK manifest includes the base APK, ARM64 native split, language splits, a density split, icon and manifest. Package metadata is read from the supplied XAPK's `manifest.json`. Recorded SHA-256 values are in the initial analysis report.

## Feature inventory from bundle strings

These are static feature indicators, not proof of a complete UI or offline operation.

| Feature group | Static evidence | Clone decision |
|---|---|---|
| Routine planning | Create/edit routines, routine list, task ordering and recommended routines | **Core:** implement locally |
| Routine execution | Start routine, start/complete task, task order/duration, timer transitions, skip/continue copy | **Core:** local timer and step progression |
| Habits and streaks | Habit wording, daily checklists, streak-day explanations and completion references | **Core:** local completion history |
| Focus/time tracking | Timer settings, duration labels, focus references and elapsed-time messages | **Core:** timer, pause/resume and completion records |
| Reminders/alarms | Push-versus-alarm settings, alarm troubleshooting, exact-alarm and notification permission strings | **Core, permission-dependent:** local reminders with Android caveats |
| Calendar | Calendar account instructions and calendar permissions | **Optional:** start with in-app schedules; external calendar integration later if wanted |
| Progress/analysis | Routine analysis, insights, streak summaries and performance/focus references | **Core basic version:** calculate from local history; defer remote AI analysis |
| Onboarding/personalisation | Onboarding steps and questions about routine type, sleep, concerns and goals | **Simplify:** optional local setup with a skip path |
| Widgets/quick launch | References to home/lock-screen widgets for starting routines | **Later:** not required for the first usable release |
| Sharing/social/friends | Friend requests, routine sharing, public routines and social copy | **Out of initial scope:** likely account/server dependent |
| AI analysis/chat | AI routine analysis, chat, image upload and analysis endpoint strings | **Out of initial scope:** remote dependency, not required for core planning |
| Account and sync | Login, sign-up, profile, account management and cross-device sync wording | **Remove from clone:** no mandatory account |
| Subscription/paywall | Premium plans, free trials, restore purchase, subscription management and billing indicators | **Remove from clone:** no billing SDK or paywall |
| Advertising/attribution/analytics | Ad ID, AppLovin, Firebase, Amplitude, Airbridge and install-referrer indicators | **Remove unless explicitly needed:** minimise data collection |

## Android permissions and integrations

The XAPK manifest declares capabilities related to internet/network state, wake locks, vibration, notifications, boot completion, exact alarms, foreground services, calendar read/write, camera, image/media access, location, overlays, battery-optimisation exemption requests, notification-policy access, advertising identifiers, and Google Play billing/licensing.

A manifest declaration only means a capability is declared; it does not prove every permission is requested at runtime or used by every flow.

For GoodMorning-Lite, start with a minimal permission set:
- Notifications when reminders are enabled.
- Exact-alarm scheduling only if the intended reminder behaviour requires it and Android's rules call for it.
- Boot rescheduling only if persistent scheduled reminders are implemented.
- Wake locks/foreground services only if testing demonstrates they are needed for the intended timer behaviour.

Do not inherit calendar, location, camera, overlay, advertising, billing or storage permissions simply because the reference app declares them. Test reminder behaviour on target Android versions.

## Technical conclusions

1. **Not a simple native Java app:** the base package contains six DEX files and a large React Native/Hermes bundle. An independent implementation is more appropriate than patching the original package.
2. **Strings are discovery evidence, not a call graph:** they identify candidate features but do not prove active paths, screen placement or backend requirements.
3. **Visual matching needs screenshots:** extracted strings and colour literals cannot establish exact layouts, spacing, navigation, animations or rendered states.
4. **Timer/reminder reliability is a key risk:** Android background execution, notification permissions, exact-alarm restrictions and battery optimisation can affect delivery; test on a real device.
5. **Keep core data local:** routines, ordered tasks, durations, schedules, completion history, streaks and basic insights can be modelled on-device. Sync/social/AI must not block core use.
6. **Keep the repository clean:** do not commit the reference APK/XAPK, extracted proprietary code, or proprietary assets. Commit analysis observations and independently written implementation only.

## Confirmed versus not yet confirmed

### Confirmed by package metadata/static extraction
- Package/version/SDK metadata and XAPK split inventory.
- React Native/Hermes indicators and a large JS bundle.
- Static indicators for routines, tasks, timers, habits/streaks, reminders, calendar, analysis, account and subscription features.
- Declared permission inventory and third-party SDK/dependency indicators.

### Not confirmed yet
- Exact home-screen layout, navigation structure or screen order.
- First-run onboarding requirements in a real session.
- Whether routine creation, timer use and habit completion work without a network connection.
- Which core screens are subscription-gated in practice.
- Whether reminders survive force-stop, reboot or battery restrictions.
- Exact rendered colour tokens, spacing, icons and typography.
- A successful clean build of GoodMorning-Lite; no replacement app has been built yet.

## Next milestone

Map screens and interactions from a running reference installation, recording each screen's purpose, navigation, controls, empty states, and whether a path appears to require login, billing or network. Use those observations to implement the first local-first dashboard and routine-run flow.

## Handling note

The source XAPK/APK remains a local analysis input and must not be committed to the repository. Do not publish extracted proprietary code or assets. This report records observations and implementation decisions for an independent replacement.
