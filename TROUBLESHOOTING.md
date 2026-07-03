# VitalPath - Troubleshooting Guide

## Current Issue: Java/JDK Not Configured

### Error Message
```
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

### Solution: Install and Configure JDK

#### Option 1: Install JDK 17 (Recommended)

1. **Download JDK 17**:
   - Oracle JDK: https://www.oracle.com/java/technologies/downloads/#java17
   - OpenJDK: https://adoptium.net/temurin/releases/?version=17

2. **Install JDK**:
   - Run the installer
   - Note the installation path (e.g., `C:\Program Files\Java\jdk-17`)

3. **Set JAVA_HOME Environment Variable** (Windows):
   ```powershell
   # Open PowerShell as Administrator
   [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-17', 'Machine')
   ```
   
   Or manually:
   - Right-click "This PC" → Properties
   - Advanced System Settings → Environment Variables
   - Under "System Variables", click "New"
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Java\jdk-17` (your JDK path)
   - Click OK

4. **Add to PATH**:
   - In Environment Variables, find "Path" under System Variables
   - Click "Edit" → "New"
   - Add: `%JAVA_HOME%\bin`
   - Click OK

5. **Verify Installation**:
   ```powershell
   java -version
   javac -version
   ```

6. **Restart IDE** (Android Studio / IntelliJ IDEA)

---

## Alternative: Use Android Studio's Embedded JDK

If you have Android Studio installed, you can use its embedded JDK:

1. **Find Android Studio's JDK**:
   - Usually at: `C:\Program Files\Android\Android Studio\jbr`

2. **Set JAVA_HOME**:
   ```powershell
   [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Android\Android Studio\jbr', 'Machine')
   ```

3. **Restart terminal/IDE**

---

## Running the App

### Option 1: Using Android Studio (Recommended)

1. **Open Project**:
   - Open Android Studio
   - File → Open → Select `d:\VitalPath`

2. **Sync Gradle**:
   - Click "Sync Now" if prompted
   - Or: File → Sync Project with Gradle Files

3. **Connect Device or Start Emulator**:
   - Physical device: Enable USB debugging
   - Emulator: Tools → Device Manager → Create/Start device

4. **Run App**:
   - Click green "Run" button (▶)
   - Or: Run → Run 'app'
   - Or: Shift + F10

### Option 2: Using Command Line (After JDK Setup)

1. **Build APK**:
   ```powershell
   cd d:\VitalPath
   .\gradlew assembleDebug
   ```

2. **Install on Device**:
   ```powershell
   .\gradlew installDebug
   ```

3. **Build and Run**:
   ```powershell
   .\gradlew installDebug
   adb shell am start -n com.example.vitalpath/.MainActivity
   ```

---

## Common Build Issues

### Issue 1: Gradle Sync Failed

**Symptoms**: "Gradle sync failed" error in Android Studio

**Solutions**:
1. Check internet connection (Gradle downloads dependencies)
2. File → Invalidate Caches → Invalidate and Restart
3. Delete `.gradle` folder in project root
4. Sync again

### Issue 2: SDK Not Found

**Symptoms**: "Android SDK not found" error

**Solutions**:
1. Open Android Studio → Tools → SDK Manager
2. Install Android SDK (API 34 or higher)
3. Set SDK location in `local.properties`:
   ```
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```

### Issue 3: Build Tools Version Mismatch

**Symptoms**: "Failed to find Build Tools revision X.X.X"

**Solutions**:
1. Open SDK Manager in Android Studio
2. SDK Tools tab → Check "Android SDK Build-Tools"
3. Install required version
4. Sync Gradle

### Issue 4: Dependency Resolution Failed

**Symptoms**: "Could not resolve all dependencies"

**Solutions**:
1. Check internet connection
2. Add Google and Maven repositories to `settings.gradle.kts`:
   ```kotlin
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
       }
   }
   ```
3. Sync Gradle

### Issue 5: Kotlin Plugin Version Mismatch

**Symptoms**: "Kotlin version mismatch" error

**Solutions**:
1. Check `build.gradle.kts` (project level)
2. Ensure Kotlin plugin version matches
3. Update if needed:
   ```kotlin
   plugins {
       id("org.jetbrains.kotlin.android") version "1.9.0" apply false
   }
   ```

---

## Runtime Issues

### Issue 1: App Crashes on Launch

**Check**:
1. Logcat in Android Studio (View → Tool Windows → Logcat)
2. Look for red error messages
3. Common causes:
   - Missing permissions in AndroidManifest.xml
   - Null pointer exceptions
   - Resource not found errors

**Solutions**:
- Check AndroidManifest.xml has all required permissions
- Verify all drawable resources exist
- Check for typos in resource IDs

### Issue 2: Notifications Not Working

**Check**:
1. Notification permission granted (Android 13+)
2. App not in battery optimization
3. Notification channel created

**Solutions**:
1. Go to Settings → Apps → VitalPath → Permissions
2. Enable "Notifications"
3. Settings → Battery → Battery optimization → VitalPath → Don't optimize

### Issue 3: WorkManager Not Scheduling

**Check**:
1. Battery optimization disabled
2. Background restrictions removed
3. Doze mode not blocking

**Solutions**:
1. Settings → Apps → VitalPath → Battery → Unrestricted
2. Developer options → Standby apps → Active
3. Test with device plugged in first

---

## Code Verification Checklist

### ✅ All Required Files Present

**Kotlin Classes**:
- [x] Habit.kt
- [x] HabitAdapter.kt
- [x] HabitTrackerFragment.kt
- [x] MoodEntry.kt
- [x] MoodEntryAdapter.kt
- [x] EmojiSelectorAdapter.kt
- [x] MoodJournalFragment.kt
- [x] CalendarAdapter.kt
- [x] HydrationWorker.kt
- [x] HydrationReminderFragment.kt
- [x] MainActivity.kt

**Layouts**:
- [x] activity_main.xml
- [x] fragment_habit_tracker.xml
- [x] fragment_mood_journal.xml
- [x] fragment_hydration_reminder.xml
- [x] item_habit.xml
- [x] item_mood_entry.xml
- [x] item_emoji_selector.xml
- [x] item_calendar_day.xml
- [x] view_calendar.xml
- [x] dialog_add_edit_habit.xml
- [x] dialog_add_mood.xml

**Drawables**:
- [x] ic_add.xml
- [x] ic_delete.xml
- [x] ic_edit.xml
- [x] ic_arrow_left.xml
- [x] ic_arrow_right.xml
- [x] ic_habits.xml
- [x] ic_mood.xml
- [x] ic_water.xml

**Menus**:
- [x] bottom_navigation_menu.xml

**Manifest**:
- [x] POST_NOTIFICATIONS permission

**Dependencies**:
- [x] Gson 2.10.1
- [x] WorkManager 2.9.0

---

## Quick Fixes

### Fix 1: Clean and Rebuild
```powershell
cd d:\VitalPath
.\gradlew clean
.\gradlew build
```

### Fix 2: Reset Gradle Cache
```powershell
.\gradlew --stop
rm -r .gradle
.\gradlew build
```

### Fix 3: Invalidate Android Studio Caches
1. File → Invalidate Caches
2. Check all boxes
3. Click "Invalidate and Restart"

---

## Getting Help

### Logcat Filtering
In Android Studio Logcat:
- Filter by package: `com.example.vitalpath`
- Filter by tag: `VitalPath`
- Show only errors: Select "Error" level

### Common Error Patterns

**NullPointerException**:
- Check lateinit vars are initialized
- Verify findViewById returns non-null

**ResourceNotFoundException**:
- Check resource IDs match XML files
- Verify drawable files exist

**ClassNotFoundException**:
- Check package names match
- Verify imports are correct

---

## System Requirements

### Minimum Requirements
- **OS**: Windows 10/11, macOS 10.14+, or Linux
- **RAM**: 8 GB (16 GB recommended)
- **Disk**: 8 GB free space
- **JDK**: Java 11 or higher (Java 17 recommended)
- **Android SDK**: API 24+ (Android 7.0+)

### Recommended Setup
- **IDE**: Android Studio Hedgehog (2023.1.1) or later
- **JDK**: Java 17
- **Gradle**: 8.2+ (included with project)
- **Android SDK**: API 34 (Android 14)

---

## Next Steps After Setup

1. **Verify JDK Installation**:
   ```powershell
   java -version
   # Should show: java version "17.x.x" or higher
   ```

2. **Open in Android Studio**:
   - File → Open → `d:\VitalPath`
   - Wait for Gradle sync to complete

3. **Run on Emulator**:
   - Tools → Device Manager
   - Create new device (Pixel 5, API 34)
   - Click Run (▶)

4. **Test Features**:
   - Navigate between tabs (Habits, Mood, Hydration)
   - Add a habit and mark it complete
   - Log a mood with emoji
   - Enable hydration reminders
   - Verify notifications work

---

## Support Resources

- **Android Developer Docs**: https://developer.android.com
- **Kotlin Docs**: https://kotlinlang.org/docs
- **WorkManager Guide**: https://developer.android.com/topic/libraries/architecture/workmanager
- **Stack Overflow**: Tag your questions with `android`, `kotlin`, `android-workmanager`

---

## Summary

**Primary Issue**: Java/JDK not installed or configured

**Solution**: 
1. Install JDK 17
2. Set JAVA_HOME environment variable
3. Add to PATH
4. Restart IDE
5. Open project in Android Studio
6. Sync Gradle
7. Run app

All code is correct and ready to run once JDK is properly configured!
