# TestProject

An Android application template with API level 22 minimum support.

## 📱 Ready-to-Install APK Files

The repository includes pre-built APK files for immediate installation:

### APK Files Available:
- **`browser.apk`** - Latest debug build (recommended for testing)
- **`debug_browser.apk`** - Debug build with debugging enabled
- **`release_browser.apk`** - Optimized release build
- **`apk/browser.apk`** - Copy in apk directory

### Installation:
```bash
# Install via ADB
adb install browser.apk

# Or install any specific build
adb install debug_browser.apk
adb install release_browser.apk
```

### APK Sizes:
- Debug builds: ~5.5MB
- Release builds: ~4.5MB

## Project Structure

This is a standard Android project created with:
- **Minimum SDK**: API 22 (Android 5.1 Lollipop)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34

## ✅ SDK Installation Complete

The Android SDK has been successfully installed and configured at `/opt/android-sdk`. The project is ready to build!

**Installed Components:**
- Android SDK Platform 22 (API level 22)
- Android SDK Platform 34 (API level 34) 
- Android SDK Build Tools 34.0.0
- Android SDK Platform Tools

## Build Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 8 or later
- Android SDK with API 22+ installed

### Setting up the Android SDK

#### Option 1: Using Android Studio (Recommended)
1. Download and install [Android Studio](https://developer.android.com/studio)
2. Open Android Studio and go through the setup wizard
3. The SDK will be automatically configured

#### Option 2: Command Line Tools Only
1. Download the [Android SDK Command Line Tools](https://developer.android.com/studio#command-tools)
2. Extract to a directory (e.g., `/opt/android-sdk`)
3. Set the `ANDROID_HOME` environment variable:
   ```bash
   export ANDROID_HOME=/path/to/android-sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
   ```
4. Accept licenses and install required components:
   ```bash
   sdkmanager --licenses
   sdkmanager "platforms;android-22" "platforms;android-34" "build-tools;34.0.0"
   ```
5. Update `local.properties` file with SDK path:
   ```
   sdk.dir=/path/to/android-sdk
   ```

### Building the Project

1. Clone or download this project
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build and run the application

### Using Gradle Command Line

```bash
# Build debug APK
./gradlew assembleDebug

# Install debug APK to connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Project Features

- Basic Android app template
- Material Design 3 theming
- AndroidX libraries
- Unit and instrumentation tests setup
- Proper launcher icons configuration

## Dependencies

- **AndroidX AppCompat**: Modern UI compatibility
- **Material Design Components**: Material 3 design system
- **ConstraintLayout**: Advanced layout management
- **JUnit**: Unit testing framework
- **Espresso**: UI testing framework

## API Level 22 Support

This project is configured to support devices running Android 5.1 (API level 22) and above, providing broad device compatibility while using modern Android development practices.
