# APK Signing Setup

## Current Configuration
The project is now configured with proper APK signing to fix the "app not installed" issue.

### Debug Builds
- Uses the default Android debug keystore
- APK: `debug_browser.apk`

### Release Builds
- Currently using a debug keystore for testing purposes
- APK: `release_browser.apk`
- Keystore: `app/debug.keystore`

## For Production Release

### Create a Production Keystore
```bash
cd app
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
```

### Update build.gradle for Production
Replace the release signing config in `app/build.gradle`:

```gradle
signingConfigs {
    release {
        storeFile file('release.keystore')
        storePassword 'YOUR_STORE_PASSWORD'
        keyAlias 'release'
        keyPassword 'YOUR_KEY_PASSWORD'
    }
}
```

## Fixed Issues
1. ✅ Release APK now properly signed
2. ✅ Removed legacy storage permissions for API 34 compatibility
3. ✅ Fixed Gradle deprecation warnings
4. ✅ Both debug and release APKs should now install successfully

## Verification Commands
```bash
# Verify APK signing
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose release_browser.apk

# Check APK permissions
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging release_browser.apk | grep uses-permission
```
