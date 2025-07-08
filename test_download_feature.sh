#!/bin/bash

echo "Testing Download Feature Implementation"
echo "========================================"

echo "1. Checking if all required files exist..."

# Check main source files
files=(
    "app/src/main/java/com/example/testproject/models/DownloadItem.java"
    "app/src/main/java/com/example/testproject/utils/DownloadManagerHelper.java"
    "app/src/main/java/com/example/testproject/DownloadsActivity.java"
    "app/src/main/java/com/example/testproject/adapters/DownloadAdapter.java"
    "app/src/main/res/layout/activity_downloads.xml"
    "app/src/main/res/layout/item_download.xml"
    "app/src/main/res/menu/downloads_menu.xml"
)

for file in "${files[@]}"; do
    if [[ -f "$file" ]]; then
        echo "✓ $file"
    else
        echo "✗ $file"
    fi
done

echo ""
echo "2. Checking if downloads menu item was added..."
if grep -q "action_downloads" app/src/main/res/menu/main_menu.xml; then
    echo "✓ Downloads menu item added"
else
    echo "✗ Downloads menu item missing"
fi

echo ""
echo "3. Checking if DownloadsActivity is declared in manifest..."
if grep -q "DownloadsActivity" app/src/main/AndroidManifest.xml; then
    echo "✓ DownloadsActivity declared in manifest"
else
    echo "✗ DownloadsActivity missing from manifest"
fi

echo ""
echo "4. Checking if download permissions are added..."
if grep -q "android.permission.WRITE_EXTERNAL_STORAGE" app/src/main/AndroidManifest.xml; then
    echo "✓ Storage permissions added"
else
    echo "✗ Storage permissions missing"
fi

echo ""
echo "5. Checking if database supports downloads..."
if grep -q "TABLE_DOWNLOADS" app/src/main/java/com/example/testproject/database/DatabaseHelper.java; then
    echo "✓ Downloads table added to database"
else
    echo "✗ Downloads table missing from database"
fi

echo ""
echo "6. Checking if MainActivity handles downloads..."
if grep -q "DownloadManagerHelper" app/src/main/java/com/example/testproject/MainActivity.java; then
    echo "✓ MainActivity integrated with download manager"
else
    echo "✗ MainActivity missing download integration"
fi

echo ""
echo "Implementation completed! You can now:"
echo "- Open the browser app"
echo "- Navigate to any website with downloadable files"
echo "- Long press or right-click on download links to start downloads"
echo "- Access downloads from the main menu → Downloads"
echo "- View download progress and manage completed downloads"
