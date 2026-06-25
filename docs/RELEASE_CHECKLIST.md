# MyFastingApp Google Play Release Checklist

MyFastingApp 1.0.0 is prepared for Google Play publishing. F-Droid is not part of the current release path.

## Local Release Build

From the repository root:

```powershell
.\gradlew.bat check assembleRelease bundleRelease --stacktrace
```

Current Play-ready outputs:

- Signed release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Signed release APK for local install testing: `app/build/outputs/apk/release/app-release.apk`

The release build is minified, resource-shrunk, and signed with the local upload key configured in `keystore.properties`.

## Signing

Private local files:

- `release/signing/myfastingapp-upload.jks`
- `keystore.properties`

These files are ignored by git. Back them up somewhere private before uploading the first release.

Upload certificate:

- Owner: `CN=MyFastingApp Upload, O=MyFastingApp, C=US`
- SHA-1: `33:3F:52:35:2D:EF:64:67:7E:2B:1B:D1:F8:CF:77:D2:FA:09:9D:52`
- SHA-256: `C2:DC:03:FB:3F:5B:19:93:A0:EB:98:6C:9B:9F:5B:BD:C1:BF:2E:D1:25:E1:5A:A0:F1:8D:92:86:B3:27:E4:03`

For a new Google Play app, upload the signed AAB and use Play App Signing. Google Play will protect the app signing key; this local key remains the upload key for future updates.

## Google Play Console Steps

1. Create the Play Console app with package `org.myfastingapp.app`.
2. Choose App Bundle upload / Play App Signing.
3. Upload `app/build/outputs/bundle/release/app-release.aab` to Internal testing first.
4. Complete Store listing using `fastlane/metadata/android/en-US`.
5. Upload final screenshots from `release/screenshots/phone` after one last device walkthrough.
6. Complete App content:
   - Privacy policy URL.
   - Data Safety.
   - Content rating questionnaire.
   - Target audience.
   - Ads declaration: no ads.
7. Run the Play pre-launch report.
8. Fix any crash, policy, privacy, accessibility, or device-compatibility findings.
9. Promote from Internal testing to Closed/Open testing or Production.

## Data Safety

MyFastingApp has no accounts, ads, analytics, SDK telemetry, cloud sync, or internet permission. App data is stored locally and can be exported/imported only when the user chooses files through Android system pickers.

Recommended Play Data Safety posture:

- Data collection: no data collected.
- Data sharing: no data shared.
- Encryption in transit: not applicable because the app does not transmit data.
- Data deletion: users can delete all local app data from Settings.

## Store Privacy Text

Use `PRIVACY.md` as the policy source, but publish it at a stable HTTPS URL before submitting to review. Replace placeholder contact details with a real support email.

## Release Notes

Initial 1.0.0 highlights:

- Offline fasting timer with editable active fasts and history.
- Week, month, and year fasting and weight trends.
- Local weight logging with target projection and kg/lb settings.
- JSON backup/import and CSV session export.
- Home-screen widget and local fasting notifications.

## Official References

- Google Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756
- Android app signing: https://developer.android.com/studio/publish/app-signing
- Play Data Safety: https://support.google.com/googleplay/android-developer/answer/10787469
