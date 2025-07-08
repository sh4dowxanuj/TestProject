#!/bin/bash

# Build and Install Script for TestProject Browser
# Usage: ./build.sh [debug|release|both|install]

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

case "${1:-both}" in
    "debug")
        echo "🔨 Building debug APK..."
        ./gradlew assembleDebug
        echo "✅ Debug APK built: debug_browser.apk"
        ;;
    "release")
        echo "🔨 Building release APK..."
        ./gradlew assembleRelease
        echo "✅ Release APK built: release_browser.apk"
        ;;
    "both")
        echo "🔨 Building both debug and release APKs..."
        ./gradlew assembleDebug assembleRelease
        echo "✅ APKs built:"
        echo "   📱 debug_browser.apk (with debugging)"
        echo "   📱 release_browser.apk (optimized)"
        echo "   📱 browser.apk (latest debug)"
        ;;
    "install")
        echo "📱 Installing browser.apk..."
        if command -v adb >/dev/null 2>&1; then
            adb install -r browser.apk
            echo "✅ APK installed successfully!"
        else
            echo "❌ ADB not found. Please install Android SDK tools."
            exit 1
        fi
        ;;
    "help"|"-h"|"--help")
        echo "Build and Install Script for TestProject Browser"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  debug    - Build debug APK only"
        echo "  release  - Build release APK only"
        echo "  both     - Build both debug and release APKs (default)"
        echo "  install  - Install browser.apk to connected device"
        echo "  help     - Show this help message"
        echo ""
        echo "APK Files:"
        echo "  browser.apk        - Latest debug build (recommended)"
        echo "  debug_browser.apk  - Debug build with debugging enabled"
        echo "  release_browser.apk - Optimized release build"
        echo ""
        ;;
    *)
        echo "❌ Unknown command: $1"
        echo "Run '$0 help' for usage information."
        exit 1
        ;;
esac

echo ""
echo "📂 APK files are available in: $PROJECT_DIR"
ls -lh *.apk 2>/dev/null || echo "   No APK files found. Run build first."
