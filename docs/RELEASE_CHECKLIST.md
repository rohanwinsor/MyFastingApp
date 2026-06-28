# MyFastingApp F-Droid Release Checklist

MyFastingApp is distributed through F-Droid. Releases are built from source by
F-Droid and verified against the signed APK attached to the matching GitHub
release.

## Before Tagging

Run from the repository root:

```powershell
.\gradlew.bat check lint assembleRelease --stacktrace
```

Confirm:

- `versionName` and `versionCode` increased.
- Fastlane metadata and the matching changelog are current.
- The manifest has no `INTERNET` or `ACCESS_NETWORK_STATE` permission.
- The release APK is signed with the existing MyFastingApp key.
- The working tree is clean and the release commit is pushed.

## Signing Key

Private local files:

- `release/signing/myfastingapp-upload.jks`
- `keystore.properties`

Both files are ignored by git. Never commit or upload either file. Keep at least
two encrypted backups in separate locations and retain the passwords in a
password manager. Losing this key prevents publishing compatible signed updates.

Public certificate:

- Owner: `CN=MyFastingApp Upload, O=MyFastingApp, C=US`
- SHA-256: `c2dc03fb3f5b1993a0eb986c9b9f5bbdc1bf2ed125e15aa0f18d9286b327e403`

Verify a release APK with:

```powershell
apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
```

## Publish Upstream Binary

1. Tag the exact release commit as `v<versionName>` and push the tag.
2. Rename the signed APK to `MyFastingApp-<versionName>.apk`.
3. Create the matching GitHub release and attach that APK.
4. Confirm the versioned download URL works without authentication:
   `https://github.com/rohanwinsor/MyFastingApp/releases/download/v%v/MyFastingApp-%v.apk`

Do not rebuild between verification and upload. The attached APK must be the
signed artifact produced from the tagged release commit.

## F-Droid Metadata

The fdroiddata entry must contain:

```yaml
Binaries: https://github.com/rohanwinsor/MyFastingApp/releases/download/v%v/MyFastingApp-%v.apk
AllowedAPKSigningKeys: c2dc03fb3f5b1993a0eb986c9b9f5bbdc1bf2ed125e15aa0f18d9286b327e403
```

Each `Builds` entry must use the full source commit hash, not a tag or branch.
After editing metadata, run:

```powershell
fdroid rewritemeta org.myfastingapp.app
fdroid lint org.myfastingapp.app
```

F-Droid builds the unsigned APK from source, compares its contents with the
upstream signed APK, verifies the allowed certificate, and publishes the
upstream binary only when verification succeeds.

## Release Notes

Version 1.0.1:

- Stops the visible timer when the app is backgrounded.
- Removes ten-minute background polling and continuous background chronometers.
- Keeps milestone alerts while making phase refreshes non-waking.
- Reduces duplicate widget rendering work.

## References

- F-Droid build metadata: https://f-droid.org/docs/Build_Metadata_Reference/
- F-Droid reproducible builds: https://f-droid.org/docs/Reproducible_Builds/
- Android app signing: https://developer.android.com/studio/publish/app-signing
