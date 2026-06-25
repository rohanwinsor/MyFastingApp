# MyFastingApp Release Test Report

Date: 2026-06-25

Device target used for prior manual checks: Android API 36 emulator, package `org.myfastingapp.app`.

## Automated Results

Passed:

```powershell
.\gradlew.bat check assembleRelease bundleRelease --stacktrace
```

Current release audit did not rerun connected emulator tests. The local emulator currently fails to start because Android hardware acceleration is unavailable on this machine. Prior instrumentation passes covered the API 36 emulator.

Unit and instrumentation coverage includes timer math, stats, weight trend projection, backup parsing, stress backup round trip, Room DAO behavior, Compose primary dashboard action, widget provider discovery, widget idle rendering, widget active/progress rendering, and widget start/end broadcast actions.

Widget service registration was also checked with `dumpsys appwidget`; Android registered `org.myfastingapp.app.widget.MyFastingAppWidgetProvider`.

## Release Artifacts

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Test-install release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

The release APK and AAB are built, minified, shrunk, and signed with the generated MyFastingApp upload key.

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

The widget root now uses an opaque cream background with a subtle border instead of transparency, so text remains legible over light, dark, and photographic wallpapers. Widget updates now use `goAsync()` during provider updates, and widget/boot/reminder paths repair active built-in plan targets before rendering or notifying.

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

## Remaining Pre-Store Checks

- Run on at least one physical device.
- Run Google Play pre-launch report after internal testing upload.
- Confirm screenshots on tablet/foldable layouts if those form factors are targeted.
- Replace privacy/contact placeholders with public store-ready URLs and email.
