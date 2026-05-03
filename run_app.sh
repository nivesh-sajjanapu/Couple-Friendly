#!/bin/bash

echo "🚀 Starting CoupleFriendly App..."
echo ""

# Check if emulator is running
if ~/Library/Android/sdk/platform-tools/adb devices | grep -q "emulator"; then
    echo "✅ Emulator detected!"
    echo "📦 Installing app..."
    ./gradlew installDebug

    if [ $? -eq 0 ]; then
        echo "🎉 App installed successfully!"
        echo "🚀 Launching app..."
        ~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.couplefriendly/.MainActivity
        echo "✅ App is running!"
    else
        echo "❌ Installation failed!"
        exit 1
    fi
else
    echo "❌ No emulator detected!"
    echo ""
    echo "Please start your Android emulator first, then run this script again."
    echo ""
    echo "Or you can manually install the APK:"
    echo "  - Location: app/build/outputs/apk/debug/app-debug.apk"
    echo "  - Drag and drop it onto the emulator screen"
    exit 1
fi
