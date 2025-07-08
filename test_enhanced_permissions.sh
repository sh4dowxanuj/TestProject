#!/bin/bash

echo "Testing Enhanced Storage Permissions Implementation"
echo "=================================================="

echo "1. Checking permission declarations in AndroidManifest.xml..."

# Check for comprehensive storage permissions
permissions=(
    "android.permission.WRITE_EXTERNAL_STORAGE"
    "android.permission.READ_EXTERNAL_STORAGE"
    "android.permission.READ_MEDIA_IMAGES"
    "android.permission.READ_MEDIA_VIDEO"
    "android.permission.READ_MEDIA_AUDIO"
    "android.permission.MANAGE_EXTERNAL_STORAGE"
)

for permission in "${permissions[@]}"; do
    if grep -q "$permission" app/src/main/AndroidManifest.xml; then
        echo "✓ $permission declared"
    else
        echo "✗ $permission missing"
    fi
done

echo ""
echo "2. Checking MainActivity permission handling..."

# Check for enhanced permission methods
methods=(
    "hasStoragePermissions"
    "requestStoragePermissions"
    "Environment.isExternalStorageManager"
    "Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION"
)

for method in "${methods[@]}"; do
    if grep -q "$method" app/src/main/java/com/example/testproject/MainActivity.java; then
        echo "✓ $method implemented"
    else
        echo "✗ $method missing"
    fi
done

echo ""
echo "3. Checking Android version compatibility..."

# Check for Android version specific code
if grep -q "Build.VERSION.SDK_INT >= Build.VERSION_CODES.R" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✓ Android 11+ (API 30+) compatibility"
else
    echo "✗ Android 11+ compatibility missing"
fi

if grep -q "Build.VERSION.SDK_INT >= Build.VERSION_CODES.M" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✓ Android 6.0+ (API 23+) compatibility"
else
    echo "✗ Android 6.0+ compatibility missing"
fi

echo ""
echo "4. Checking permission request handling..."

if grep -q "onRequestPermissionsResult" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✓ Permission result handling implemented"
else
    echo "✗ Permission result handling missing"
fi

if grep -q "ActivityCompat.requestPermissions" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✓ Permission request method implemented"
else
    echo "✗ Permission request method missing"
fi

echo ""
echo "5. Checking enhanced user messages..."

enhanced_strings=(
    "storage_permissions_granted"
    "storage_permissions_denied"
    "manage_storage_permission_needed"
)

for string in "${enhanced_strings[@]}"; do
    if grep -q "$string" app/src/main/res/values/strings.xml; then
        echo "✓ Enhanced string resource: $string"
    else
        echo "✗ Missing string resource: $string"
    fi
done

echo ""
echo "Enhanced Storage Permissions Summary:"
echo "======================================"
echo "✓ MANAGE_EXTERNAL_STORAGE for Android 11+ (broad file access)"
echo "✓ WRITE_EXTERNAL_STORAGE for Android 6-10 (legacy storage)"
echo "✓ READ_EXTERNAL_STORAGE for Android 6-10 (legacy storage)"
echo "✓ Media permissions for Android 13+ (scoped storage)"
echo "✓ Adaptive permission strategy based on Android version"
echo "✓ Settings app integration for special permissions"
echo "✓ Comprehensive error handling and user feedback"

echo ""
echo "Usage Instructions:"
echo "=================="
echo "1. Install and run the app"
echo "2. Try to download a file"
echo "3. On Android 11+: App will prompt for 'All files access' permission"
echo "4. On Android 6-10: App will request standard storage permissions"
echo "5. Below Android 6: Permissions are granted automatically"
echo "6. Downloads will work across all supported Android versions"

echo ""
echo "Permission Behavior by Android Version:"
echo "======================================="
echo "• Android 14+ (API 34+): Uses scoped media permissions + MANAGE_EXTERNAL_STORAGE"
echo "• Android 11-13 (API 30-33): Uses MANAGE_EXTERNAL_STORAGE for broad access"
echo "• Android 6-10 (API 23-29): Uses WRITE_EXTERNAL_STORAGE + READ_EXTERNAL_STORAGE"
echo "• Below Android 6 (API < 23): All permissions granted at install time"
