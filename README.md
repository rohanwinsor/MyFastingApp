# MyFastingApp

MyFastingApp is a fully offline, GPLv3 Android fasting tracker. It has no accounts, no ads, no analytics, no cloud sync, no proprietary SDKs, and no network permission.

## Features

- Active fasting timer with built-in plans: 13:11, 16:8, 18:6, 20:4, OMAD, 24h, and custom duration.
- One active fast at a time, editable start/end times, editable history, streaks, totals, average duration, and longest fast.
- Evidence-based fasting phase labels shown on the main timer, with short text and phase-specific colors.
- Local weight logging, target weight, kg/lb setting, trend graph, and simple on-device projection.
- Week, month, and year trend views for fasting and weight.
- Home-screen widget with idle and active states, progress ring, and start/end/open controls.
- Ongoing local notification while fasting plus milestone notifications at 25%, 50%, 75%, 90%, 95%, and 100%.
- Local JSON backup import/export and CSV session export through Android's Storage Access Framework.

## Build

Install JDK 17 and Android SDK Platform 36. Android Studio can provide both. From the repository root:

```powershell
.\gradlew.bat check assembleDebug assembleRelease bundleRelease
.\gradlew.bat connectedDebugAndroidTest
```

Current release outputs:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Signed release APK: `app/build/outputs/apk/release/app-release.apk`
- Signed release bundle: `app/build/outputs/bundle/release/app-release.aab`

The release build enables code minification and resource shrinking. Google Play upload signing is configured through local ignored files: `keystore.properties` and `release/signing/myfastingapp-upload.jks`.

## Offline And Privacy Guarantees

The manifest intentionally omits `INTERNET` and `ACCESS_NETWORK_STATE`. App data stays in the local Room database and DataStore preferences unless the user explicitly exports a backup.

The only declared app permissions are local notification support and reboot handling for local reminders/widget refresh. MyFastingApp does not use Google Play Services, Firebase, analytics, crash reporting, ads, remote config, or health/cloud integrations.

## Project Docs

- `docs/APP_REFERENCE.md` explains architecture, data model, backup format, notifications, widget behavior, and release invariants.
- `docs/TEST_REPORT.md` records the current release-hardening test pass and screenshot walkthrough.
- `docs/RELEASE_CHECKLIST.md` lists the remaining Google Play publishing steps.

## License

GPL-3.0-only. See `LICENSE`.
