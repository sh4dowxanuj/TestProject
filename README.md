# Chrome Like Browser - Android App

A modern, feature-rich Android browser application built with Java, designed to provide a smooth and intuitive browsing experience similar to Chrome.

## 🚀 Features

### Core Browser Features
- **Multi-Tab Browsing** - Open and manage multiple tabs with smooth switching
- **WebView Integration** - Full HTML5, CSS3, and JavaScript support
- **Navigation Controls** - Back, forward, refresh, and home buttons
- **URL Bar** - Smart URL input with search functionality
- **Progress Indicator** - Visual loading progress for web pages
- **Fullscreen Video Support** - Seamless video playback experience

### Tab Management
- **Dynamic Tab Creation** - Create new tabs with a single tap
- **Tab Switching** - Quick switching between open tabs
- **Auto-Scrolling Tabs** - Automatically scroll to show active tab
- **Visual Tab Indicators** - Clear visual feedback for selected tabs
- **Tab Close Functionality** - Easy tab closure with dedicated close buttons
- **Compact Tab Layout** - Optimized tab sizing for better screen usage

### Bookmarks & History
- **Bookmark Management** - Save and organize favorite websites
- **Browsing History** - Track and revisit previously visited pages
- **Quick Access** - Easy access to bookmarks and history
- **Search & Filter** - Find bookmarks and history entries quickly
- **Delete Functionality** - Remove individual bookmarks and history items

### Settings & Customization
- **JavaScript Toggle** - Enable/disable JavaScript execution
- **Cache Management** - Clear browser cache and temporary files
- **Data Management** - Clear all browsing data and app data
- **Dark Theme** - Modern dark theme for comfortable browsing
- **Responsive Design** - Adaptive UI for different screen sizes

### Security & Privacy
- **Secure Browsing** - HTTPS support with secure connections
- **Privacy Controls** - Clear browsing data and cache
- **Permission Management** - Proper handling of web permissions
- **No Tracking** - No user data collection or tracking

## 📱 User Interface

### Modern Design
- **Material Design 3** - Following latest Android design guidelines
- **Dark Theme** - Comfortable browsing in low-light conditions
- **Smooth Animations** - Fluid transitions and interactions
- **Touch-Friendly** - Optimized for mobile touch interactions
- **Responsive Layout** - Adapts to different screen sizes and orientations

### Navigation
- **Intuitive Controls** - Easy-to-use navigation buttons
- **Smart URL Bar** - Auto-complete and search suggestions
- **Tab Bar** - Horizontal scrollable tab interface
- **Menu System** - Organized popup menu for quick actions
- **Gesture Support** - Back gesture and touch navigation

## 🛠️ Technical Specifications

### Platform & Requirements
- **Target SDK**: Android 14 (API 34)
- **Minimum SDK**: Android 7.0 (API 24)
- **Language**: Java
- **Build System**: Gradle with Kotlin DSL support
- **Architecture**: Modern Android app architecture

### Dependencies
- **AndroidX Libraries**: AppCompat 1.7.0, Material Design 1.12.0, ConstraintLayout 2.1.4
- **WebView**: Enhanced WebView with modern web standards support and security improvements
- **RecyclerView**: Efficient list management for bookmarks and history (v1.3.2)
- **Room Database**: Local data storage for bookmarks and history
- **Material Components**: Modern UI components and theming
- **Core Library**: AndroidX Core 1.13.1 for modern WindowInsets support

### Performance Features
- **Memory Management**: Efficient WebView memory handling
- **Background Processing**: Optimized for smooth performance
- **Resource Optimization**: Minimal resource usage
- **Fast Startup**: Quick app launch and tab creation
- **Smooth Scrolling**: Optimized scrolling performance

## 🔧 Recent Updates & Fixes

### Version 1.0 - Latest Updates

#### 🎯 Tab UI Improvements (Latest)
- **Enhanced Tab Management**: Improved tab creation and switching logic
- **Visual Feedback**: Better selected tab indication with borders and colors
- **Auto-Scrolling**: Automatic scrolling to show active tab
- **Compact Design**: Reduced tab size for better screen utilization
- **Improved Close Buttons**: Better positioned and sized close buttons
- **State Management**: Robust tab state handling prevents crashes

#### 🔒 APK Installation Fixes
- **Proper Signing**: Fixed APK signing configuration for all build types
- **Permission Updates**: Removed legacy storage permissions for API 34 compatibility
- **Gradle Modernization**: Updated build configuration with modern syntax
- **Release Build Support**: Proper release APK generation with signing

#### 🛠️ Deprecation Fixes & Modernization (July 2025)
- **System UI API**: Replaced deprecated `SYSTEM_UI_FLAG_*` with modern `WindowInsetsControllerCompat`
- **Back Press Handling**: Updated from deprecated `onBackPressed()` to `OnBackPressedDispatcher`
- **Color Resources**: Migrated from deprecated `getResources().getColor()` to `ContextCompat.getColor()`
- **WebSettings Security**: Removed deprecated file access methods for improved security
- **Gradle Syntax**: Fixed deprecated property assignment syntax warnings
- **Dependencies**: Updated all AndroidX libraries to latest stable versions
- **API Compatibility**: Maintains backward compatibility while using modern APIs

#### 🏗️ Build System Improvements
- **Gradle 8.14.1**: Latest Gradle version with improved performance
- **Modern Syntax**: Updated property assignment syntax
- **Deprecation Fixes**: Resolved all build-time deprecation warnings
- **Multi-APK Output**: Automatic APK copying to multiple locations

### Architecture Improvements
- **Error Handling**: Enhanced error handling throughout the app
- **Memory Management**: Better WebView lifecycle management
- **State Preservation**: Proper state handling during configuration changes
- **Performance Optimization**: Reduced memory usage and improved responsiveness

## 📋 Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17 or later
- Android SDK with API 34
- Gradle 8.14.1 or later

### Building the Project
```bash
# Clone the repository
git clone https://github.com/sh4dowxanuj/TestProject.git

# Navigate to project directory
cd TestProject

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on device
./gradlew installDebug
```

### APK Files
The build process generates multiple APK files:
- `debug_browser.apk` - Debug version for testing
- `release_browser.apk` - Release version for distribution
- `browser.apk` - Latest debug build in root directory

## 🎯 Usage Guide

### Basic Navigation
1. **Open the app** - Launches with Google homepage
2. **Enter URL** - Type in the URL bar or search terms
3. **Create new tab** - Tap the + button in the tab bar
4. **Switch tabs** - Tap on any tab to switch to it
5. **Close tab** - Tap the X button on any tab

### Managing Bookmarks
1. **Add bookmark** - Menu → Add Bookmark
2. **View bookmarks** - Menu → Bookmarks
3. **Open bookmark** - Tap on any bookmark
4. **Delete bookmark** - Tap the delete button on bookmark

### Browsing History
1. **View history** - Menu → History
2. **Revisit page** - Tap on any history item
3. **Clear history** - Menu → Clear History
4. **Delete item** - Tap delete button on history item

### Settings & Privacy
1. **Access settings** - Menu → Settings
2. **Toggle JavaScript** - Use the JavaScript switch
3. **Clear cache** - Tap "Clear Cache" button
4. **Clear all data** - Tap "Clear All Data" button

## 🔄 Project Structure

```
app/
├── src/main/
│   ├── java/com/example/testproject/
│   │   ├── MainActivity.java           # Main browser activity
│   │   ├── BookmarksActivity.java      # Bookmark management
│   │   ├── HistoryActivity.java        # History management
│   │   ├── SettingsActivity.java       # Settings and preferences
│   │   ├── models/                     # Data models
│   │   ├── adapters/                   # RecyclerView adapters
│   │   └── database/                   # Database helper
│   ├── res/
│   │   ├── layout/                     # XML layouts
│   │   ├── drawable/                   # Icons and graphics
│   │   ├── values/                     # Colors, strings, themes
│   │   └── menu/                       # Menu definitions
│   └── AndroidManifest.xml            # App configuration
├── build.gradle                       # Module build configuration
└── debug.keystore                     # Debug signing keystore
```

## 🤝 Contributing

We welcome contributions to improve the browser! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/new-feature`)
3. **Make your changes** with proper testing
4. **Commit your changes** (`git commit -am 'Add new feature'`)
5. **Push to the branch** (`git push origin feature/new-feature`)
6. **Create a Pull Request**

### Development Guidelines
- Follow Android development best practices
- Maintain consistent code style
- Add proper error handling
- Test on multiple devices and API levels
- Update documentation for new features

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

## 🐛 Known Issues

- WebView memory usage optimization ongoing
- Tab animation improvements planned

**Recent Fixes Completed:**
- ✅ All deprecation warnings resolved
- ✅ Modern Android API migration completed
- ✅ Build system modernization finished
- ✅ Security improvements implemented

## 📞 Support

If you encounter any issues or have suggestions:
- Open an issue on GitHub
- Check the documentation in the `docs/` folder
- Review the `TAB_UI_FIXES.md` for recent tab improvements
- Check `SIGNING_SETUP.md` for APK signing information

## 🎉 Acknowledgments

- Material Design Components for Android
- AndroidX libraries and WebView component
- Open source community for inspiration and guidance

---

**Version**: 1.0  
**Last Updated**: July 2025  
**Compatibility**: Android 7.0+ (API 24+)  
**Target**: Android 14 (API 34)