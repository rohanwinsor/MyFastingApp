# Contributing

MyFastingApp is privacy-first and offline-first. Contributions should preserve these constraints:

- Do not add network permissions, analytics, ads, proprietary SDKs, Firebase, Google Play Services, or remote configuration.
- Store fasting data locally and make user-controlled export/import the only data transfer path.
- Add focused tests for changes to timer math, persistence, backup formats, reminders, and widget actions.

Run unit tests before submitting changes:

```powershell
.\gradlew.bat test
```

For release-sensitive changes, also run:

```powershell
.\gradlew.bat check assembleDebug assembleRelease bundleRelease
.\gradlew.bat connectedDebugAndroidTest
```

Keep `docs/APP_REFERENCE.md` and `docs/TEST_REPORT.md` current when changing storage, backup, notifications, widgets, or release behavior.
