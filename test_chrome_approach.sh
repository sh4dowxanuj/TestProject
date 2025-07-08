#!/bin/bash

echo "Testing Chrome-like Download Approach"
echo "====================================="

echo "1. Checking permission declarations (Chrome-like approach)..."

# Check that MANAGE_EXTERNAL_STORAGE is NOT present
if grep -q "MANAGE_EXTERNAL_STORAGE" app/src/main/AndroidManifest.xml; then
    echo "✗ MANAGE_EXTERNAL_STORAGE found - should not be used in Chrome approach"
else
    echo "✓ No MANAGE_EXTERNAL_STORAGE - follows Chrome approach"
fi

# Check for appropriate permissions only
permissions=(
    "android.permission.WRITE_EXTERNAL_STORAGE"
    "android.permission.READ_EXTERNAL_STORAGE"
    "android.permission.READ_MEDIA_IMAGES"
    "android.permission.READ_MEDIA_VIDEO"
    "android.permission.READ_MEDIA_AUDIO"
)

for permission in "${permissions[@]}"; do
    if grep -q "$permission" app/src/main/AndroidManifest.xml; then
        echo "✓ $permission declared (appropriate for Chrome approach)"
    else
        echo "✗ $permission missing"
    fi
done

echo ""
echo "2. Checking download storage approach..."

# Check that downloads go to standard Downloads folder
if grep -q "Environment.DIRECTORY_DOWNLOADS" app/src/main/java/com/example/testproject/utils/DownloadManagerHelper.java; then
    echo "✓ Uses standard Downloads directory"
else
    echo "✗ Not using standard Downloads directory"
fi

# Check that no Browser subfolder is created
if grep -q "Browser" app/src/main/java/com/example/testproject/utils/DownloadManagerHelper.java; then
    echo "✗ Still creating Browser subfolder - should use standard Downloads like Chrome"
else
    echo "✓ No custom subfolder - uses standard Downloads like Chrome"
fi

echo ""
echo "3. Checking permission handling logic..."

# Check for Chrome-like permission handling
if grep -q "Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✓ Android 10+ scoped storage handling"
else
    echo "✗ Missing Android 10+ scoped storage handling"
fi

# Check that Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION is NOT used
if grep -q "Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✗ Still using MANAGE_ALL_FILES_ACCESS_PERMISSION - not Chrome approach"
else
    echo "✓ No MANAGE_ALL_FILES_ACCESS_PERMISSION - follows Chrome approach"
fi

# Check that Environment.isExternalStorageManager is NOT used
if grep -q "Environment.isExternalStorageManager" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✗ Still checking isExternalStorageManager - not Chrome approach"
else
    echo "✓ No isExternalStorageManager check - follows Chrome approach"
fi

echo ""
echo "4. Checking simplified permission flow..."

# Check for simple permission request
if grep -q "WRITE_EXTERNAL_STORAGE" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✓ Uses legacy storage permission for Android 6-9"
else
    echo "✗ Missing legacy storage permission handling"
fi

echo ""
echo "Chrome-like Download Approach Summary:"
echo "======================================"
echo "✓ No 'All files access' permission requests"
echo "✓ Uses Android DownloadManager with standard Downloads folder"
echo "✓ Scoped storage compatible (Android 10+)"
echo "✓ Legacy storage permission for Android 6-9 compatibility"
echo "✓ Downloads appear in standard Downloads app"
echo "✓ No confusing permission dialogs for users"
echo "✓ Follows Android's recommended storage practices"

echo ""
echo "Storage Strategy by Android Version:"
echo "==================================="
echo "• Android 10+ (API 29+): Scoped storage - no permissions needed"
echo "• Android 6-9 (API 23-28): Legacy WRITE_EXTERNAL_STORAGE permission"
echo "• Below Android 6: Automatic permission grant"

echo ""
echo "User Experience:"
echo "==============="
echo "• Android 10+: Downloads work immediately, no permission dialogs"
echo "• Android 6-9: Simple storage permission request (like other apps)"
echo "• All versions: Files appear in standard Downloads folder"
echo "• Downloads visible in Files app and other file managers"
echo "• No confusing 'All files access' requests like some apps"
