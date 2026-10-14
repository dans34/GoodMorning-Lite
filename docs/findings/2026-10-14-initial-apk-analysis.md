# Routinery APK Analysis — Initial Findings

**Project:** GoodMorning-Lite  
**Date:** 2026-10-14  
**Source package:** `com.alt.goodmorning`  
**Source version:** 3.30.0  
**Source archive:** `Routine+Planner,+Habit+Tracker_3.30.0_APKPure.xapk`

## Goals

Build a separate Android application inspired by the user's authorised analysis of Routinery, prioritising visual similarity while retaining core habit and routine functionality. The requested simplifications are to remove login, account requirements, and billing/subscription flows. Prefer local storage and local reminders where feasible.

## Package inventory

The XAPK is a ZIP container of approximately 114,686,972 bytes. It contains 22 entries, including:

- `com.alt.goodmorning.apk` (~98.6 MB) — primary APK
- `config.arm64_v8a.apk` (~14.5 MB) — ARM64 native split
- `config.ar.apk`, `config.de.apk`, `config.en.apk`, `config.es.apk`, `config.fr.apk`, `config.hi.apk`, `config.in.apk`, `config.it.apk`, `config.ja.apk`, `config.ko.apk`, `config.my.apk`, `config.pt.apk`, `config.ru.apk`, `config.th.apk`, `config.tr.apk`, `config.vi.apk`, `config.zh.apk` — language/resource splits
- `config.xxhdpi.apk` — screen-density resources
- `icon.png`
- `manifest.json`

The archive contains six DEX bytecode files in the primary application bundle. Static inspection identifies React Native with Hermes, with a JavaScript bundle of roughly 17.2 MB. The reported minimum Android version is 7.0 and target SDK is 36.

## Initial component indicators

The package includes references to features and dependencies associated with:

- Habits, routines, timers, reminders, calendar and routine analysis
- Login, sign-up and paywall/subscription interfaces
- Adapty and Google Play Billing
- Firebase, analytics and advertising-related components
- Montserrat and Pretendard fonts

These are static package findings, not proof that each feature operates offline or a full behavioural map. Runtime behaviour and remote dependencies still need to be investigated.

## Important technical notes

1. Keep the original downloaded package unmodified and track its identity with a SHA-256 hash.
2. Do not commit the original third-party XAPK/APK binaries or extracted proprietary code to this repository. Keep only analysis notes and original-source-derived observations. Implement the clone as a separate clean project.
3. First map navigation, screens, strings, font assets, colors, icons, and local/remote service dependencies.
4. Test whether core functions require a login or server. Where possible, implement equivalent local-first behaviour rather than merely hiding login or subscription UI.
5. Reminders need Android notification permissions and careful scheduling across reboot and battery restrictions.
6. Visual fidelity should be guided by app screenshots and observed behaviour, rather than assumptions based only on resource names.

## Next steps

- Verify manifest metadata and certificate/version details from all relevant XAPK splits.
- Extract a safe inventory of resource names, strings, fonts, images, and bundle identifiers.
- Map the main dashboard, routine editor, timer, habit/progress screens, reminders, settings, and paywall/account entry points.
- Create an Android Studio project for the independent clone.
- Commit each meaningful report/source milestone with a changelog entry.
