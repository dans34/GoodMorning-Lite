# Package verification, permissions and deeper static indicators

**Date:** 2026-10-14  
**Application:** Routinery  
**Package:** `com.alt.goodmorning`  
**Version name:** `3.30.0`  
**Version code:** `1067754`

## Integrity values

For reproducibility, these hashes identify the provided archive and the primary APK extracted from it:

- XAPK SHA-256: `f992651d105dad73e1586e4ca10ebed5a8e30652eda9cadfff5be222a89f2437`
- Primary APK SHA-256: `f390c20de3c22336d7554ad4faa91fd449dcc93576f1d65043e81df031c0286a`

The uploaded XAPK is 114,686,972 bytes. Its embedded `manifest.json` describes an archive total size of 114,573,480 bytes; that field does not include all ZIP container overhead.

## Metadata from the XAPK manifest

- `xapk_version`: 2
- `min_sdk_version`: 24 (Android 7.0)
- `target_sdk_version`: 36
- Primary split: `com.alt.goodmorning.apk`
- Additional configuration splits: ARM64 native library, multiple languages and `xxhdpi` resources.
- Six DEX files are present in the primary APK.
- JavaScript bundle: `assets/index.android.bundle`, 17,170,708 bytes.

The bundle contains indicators for React Native and Hermes. This means some application logic is represented in a bundled JavaScript/Hermes payload, while Android platform integrations and dependencies also reside in DEX/native code.

## Permission inventory

The XAPK manifest declares permissions including:

### Routine and reminder related
- `WAKE_LOCK`
- `VIBRATE`
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `USE_EXACT_ALARM`
- `SCHEDULE_EXACT_ALARM`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

### Calendar and device integration
- `READ_CALENDAR`
- `WRITE_CALENDAR`
- `SYSTEM_ALERT_WINDOW`
- `ACCESS_NOTIFICATION_POLICY`
- `CAMERA`
- `READ_MEDIA_IMAGES`
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`

### Network, telemetry and commercial integration indicators
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `com.android.vending.BILLING`
- `com.android.vending.CHECK_LICENSE`
- `com.google.android.gms.permission.AD_ID`
- Google/advertising attribution, topics, and install-referrer permissions
- Coarse/fine/background location permissions

A declared permission does **not** by itself prove that the app requests it at runtime or that a feature depends on it. For a local-first replacement, permissions should be requested only for features we actually implement. Calendar access, media access, location, overlay access, billing and advertising should not be added by default.

## Useful string/resource indicators

Static strings in the bundled payload include names and labels associated with routine editing and execution, timers, notifications, calendar utilities, routine analysis, onboarding, subscription screens and account flows. Examples of internal labels surfaced include `TimerStartScreen`, `AnalysisRoutineCalendarUtils`, `resetPassword`, `subscription`, and `signup-or-signin`.

These are implementation clues only. They do not yet define the route graph, exact UI appearance, API contract, or which features work without a remote account. We should avoid treating string matches as proof of behaviour.

## Design and implementation decisions

1. Build a clean, separate app; do not patch the original application or upload its proprietary source, JS bundle, APK, or XAPK into this repo.
2. Maintain a traceable distinction between **observed**, **inferred**, and **not yet verified** findings.
3. Screenshots from a running installation are needed to reproduce layout accurately. Resource names and strings alone cannot establish exact spacing, colors, navigation, or animations.
4. Implement the required core routines and habit features locally, rather than bypassing an original service or payment check.
5. Keep routine timer/notification behaviour reliable with Android's notification permission and scheduling rules. Test foreground/background and reboot behaviour.
