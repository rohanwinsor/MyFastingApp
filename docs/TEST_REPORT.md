# MyFastingApp Release Test Report

Date: 2026-06-28

Device target used for prior manual checks: Android API 36 emulator, package `org.myfastingapp.app`.

## Automated Results

Passed:

```powershell
.\gradlew.bat check assembleRelease bundleRelease --stacktrace
```

Current release audit did not rerun connected emulator tests because no emulator or physical device is connected. Prior instrumentation passes covered the API 36 emulator.

Unit and instrumentation coverage includes timer math, stats, weight trend projection, backup parsing, stress backup round trip, Room DAO behavior, Compose primary dashboard action, widget provider discovery, widget idle rendering, widget active/progress rendering, and widget start/end broadcast actions.

Widget service registration was also checked with `dumpsys appwidget`; Android registered `org.myfastingapp.app.widget.MyFastingAppWidgetProvider`.

## Release Artifacts

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Test-install release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

The release APK and AAB are built, minified, shrunk, and signed with the MyFastingApp release key.

Upload certificate:

- Owner: `CN=MyFastingApp Upload, O=MyFastingApp, C=US`
- SHA-1: `33:3F:52:35:2D:EF:64:67:7E:2B:1B:D1:F8:CF:77:D2:FA:09:9D:52`
- SHA-256: `C2:DC:03:FB:3F:5B:19:93:A0:EB:98:6C:9B:9F:5B:BD:C1:BF:2E:D1:25:E1:5A:A0:F1:8D:92:86:B3:27:E4:03`

## Permission And Dependency Audit

Manifest/app APK permission scan found:

- `android.permission.POST_NOTIFICATIONS`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- AndroidX generated dynamic receiver permission

No `INTERNET` permission. No `ACCESS_NETWORK_STATE` permission.

Source/build scan found no Play Services, Firebase, analytics, or ads dependencies.

Artifact and source scans found no stale development names or production sample-data entry points.

## Widget Audit

The widget root uses an opaque cream background with a subtle border, so text remains legible over light, dark, and photographic wallpapers. Widget updates use `goAsync()` during provider updates, render one shared `RemoteViews` payload for all instances, and do not run a continuously ticking chronometer or periodic refresh worker.

## Battery Audit

- The Compose one-second timer is lifecycle-aware and runs only while an active fast is visible in a resumed activity.
- The previous ten-minute `RTC_WAKEUP` notification polling loop was removed.
- Background notifications and the widget use event-driven minute snapshots.
- Six requested milestone alerts may wake the device during a fast.
- Phase-only status updates use non-waking alarms.
- Target reminders are only scheduled when enabled by the user.
- No foreground service, WorkManager job, wake lock, exact alarm, or periodic widget update is used.

## Manual Walkthrough Screenshots

Screenshots are stored in `release/screenshots/phone`.

- `01_myfastingapp_timer.png`: renamed idle timer screen showing `MyFastingApp`.

The prior screenshot set was removed after the development rename so stale branding is not kept in the repository. Store-listing screenshots should be recaptured from the final signed build before publication.

## Stress Data

Large local datasets were exercised during development:

- Week, month, and year trend tabs rendered for fasting and weight.
- History screen rendered dense local data without requiring remote services.

The backup unit stress test round-tripped 400 sessions and 365 weights through JSON encoding/decoding.

## Exploratory Stress

An Android Monkey run was attempted with 300 events before the rename. The emulator generated noisy system tombstone monitor output, so the run was aborted and is not counted as a conclusive pass. Checked logs did not show a MyFastingApp `FATAL EXCEPTION` or ANR during that attempt.

## Remaining Release Checks

- Run on at least one physical device.
- Check Android battery usage across one complete fast after installing 1.0.1.
- Confirm the F-Droid reproducible-build comparison passes.
- Replace privacy/contact placeholders with public store-ready URLs and email.
