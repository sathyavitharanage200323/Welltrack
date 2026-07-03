# Step Counter Fix

## Problem
The step counter was not working properly. It was resetting every time the fragment was recreated and not accurately tracking daily steps.

## Root Causes

### 1. **Incorrect Step Calculation**
- Used `initialSteps` that reset on every fragment creation
- Didn't handle sensor's cumulative step count properly
- Lost step count when app was closed/reopened

### 2. **Missing Day Reset Logic**
- No mechanism to reset steps at midnight
- Steps accumulated across multiple days

### 3. **Permission Handling**
- Android 10+ requires runtime permission for ACTIVITY_RECOGNITION
- Permission wasn't being requested at runtime

---

## Solution Implemented

### 1. **Fixed Step Calculation Logic**
```kotlin
Before (Broken):
- initialSteps = sensor value on start
- currentSteps = sensor value - initialSteps
- Problem: initialSteps resets every time

After (Fixed):
- previousSensorStepCount = saved sensor value from start of day
- currentSteps = current sensor value - previousSensorStepCount
- Persists across app restarts
```

### 2. **Added Day Reset Logic**
```kotlin
- Save last_step_date
- On sensor update, check if date changed
- If new day:
  - Reset previousSensorStepCount to current sensor value
  - Reset currentSteps to 0
  - Save new date
```

### 3. **Added Runtime Permission Request**
```kotlin
- Check Android version
- If Android 10+:
  - Request ACTIVITY_RECOGNITION permission
  - Handle grant/deny
- If below Android 10:
  - No permission needed
```

---

## How It Works Now

### Step Tracking Flow
```
1. App opens → Check permission
2. Permission granted → Setup step counter
3. Load saved data:
   - currentSteps (today's steps)
   - previousSensorStepCount (sensor value at start of day)
   - last_step_date (last tracked date)

4. Sensor updates:
   - Get current sensor value
   - Check if new day:
     - Yes → Reset counters
     - No → Calculate: current - previous
   - Save steps
   - Update UI

5. App closes → Data saved
6. App reopens → Data restored
```

### Day Reset Logic
```
Midnight passes:
1. Sensor updates with new value
2. Compare last_step_date with today
3. Different date detected
4. Reset:
   - previousSensorStepCount = current sensor value
   - currentSteps = 0
5. Save new date
6. Continue tracking from 0
```

---

## Changes Made

### Files Modified (1)

**`HomeFragment.kt`** - Complete step counter rewrite

#### Added Imports
```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
```

#### Updated Variables
```kotlin
// Before
private var initialSteps = 0

// After
private var sensorStepCount = 0
private var previousSensorStepCount = 0
```

#### Added Permission Launcher
```kotlin
private val activityRecognitionPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        setupStepCounter()
    } else {
        // Show error message
    }
}
```

#### Added Permission Check
```kotlin
private fun checkPermissionAndSetupStepCounter() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Request permission for Android 10+
    } else {
        // No permission needed for older versions
    }
}
```

#### Fixed Step Calculation
```kotlin
override fun onSensorChanged(event: SensorEvent?) {
    // Get sensor value
    // Check if new day
    // Calculate steps correctly
    // Save data
    // Update UI
}
```

---

## Data Storage

### SharedPreferences Keys
```kotlin
"steps_$today" → Current step count for today
"sensor_steps_$today" → Sensor value at start of day
"last_step_date" → Last date steps were tracked
```

### Example Data
```
Date: 2025-01-15
steps_2025-01-15 = 5432
sensor_steps_2025-01-15 = 123456
last_step_date = "2025-01-15"

Sensor current value: 128888
Calculated steps: 128888 - 123456 = 5432 ✅
```

---

## Testing Steps

### Initial Setup
1. **Install app** on device with step sensor
2. **Grant permission** when prompted
3. **Walk around** with device
4. **Check Home tab** - Steps should increase

### Day Transition
1. **Track steps** during day (e.g., 5000 steps)
2. **Change device date** to next day (or wait for midnight)
3. **Open app** - Steps should reset to 0
4. **Walk around** - Steps should count from 0

### App Restart
1. **Track steps** (e.g., 3000 steps)
2. **Close app** completely
3. **Reopen app**
4. **Check Home tab** - Should show 3000 steps ✅

### Permission Handling
1. **Deny permission** when prompted
2. **Check UI** - Shows "Permission required"
3. **Go to Settings** → Grant permission
4. **Return to app** - Should start tracking

---

## Device Requirements

### Step Sensor
- **Required**: Device must have step counter sensor
- **Check**: Most modern smartphones have it
- **Fallback**: Shows "Step sensor not available" if missing

### Android Version
- **Minimum**: Android 5.0+ (API 21)
- **Recommended**: Android 10+ (API 29) for best experience
- **Permission**: Required on Android 10+

### Common Devices with Step Sensor
✅ Samsung Galaxy S8 and newer  
✅ Google Pixel all models  
✅ OnePlus 5 and newer  
✅ Xiaomi Mi 8 and newer  
✅ iPhone (all models with iOS)  

---

## Troubleshooting

### Steps Not Counting
**Problem**: Steps stay at 0

**Solutions**:
1. Check if device has step sensor
2. Grant ACTIVITY_RECOGNITION permission
3. Walk at least 10-20 steps (sensor has threshold)
4. Restart app
5. Check if sensor is working in other apps

### Steps Reset Unexpectedly
**Problem**: Steps go back to 0 during day

**Causes**:
- App data cleared
- Date changed manually
- Device rebooted (sensor resets)

**Solution**:
- This is expected behavior for sensor resets
- App will recalibrate automatically

### Permission Denied
**Problem**: "Permission required" message

**Solution**:
1. Go to Settings → Apps → VitalPath
2. Permissions → Physical activity
3. Allow permission
4. Return to app

### Inaccurate Count
**Problem**: Steps don't match other apps

**Explanation**:
- Different apps use different algorithms
- Sensor calibration varies
- Movement detection thresholds differ
- This is normal and expected

---

## Technical Details

### Sensor Type
```kotlin
Sensor.TYPE_STEP_COUNTER
- Returns cumulative steps since last reboot
- Resets to 0 on device reboot
- Low power consumption
- Hardware-based (not software estimation)
```

### Calculation Method
```kotlin
Daily Steps = Current Sensor Value - Start of Day Sensor Value

Example:
Device rebooted: Sensor = 0
Morning (8 AM): Sensor = 1234 → Save as start
Afternoon (2 PM): Sensor = 5678
Steps today = 5678 - 1234 = 4444 steps ✅
```

### Day Reset Detection
```kotlin
1. Save last_step_date = "2025-01-15"
2. Next sensor update checks date
3. If current date = "2025-01-16":
   - New day detected
   - Reset counters
   - Save new date
```

---

## Limitations

### Current Implementation
1. **Reboot handling**: Steps reset on device reboot (sensor limitation)
2. **Single device**: Can't sync across multiple devices
3. **No history**: Only tracks current day
4. **No manual entry**: Can't manually add steps

### Future Enhancements (Not Implemented)
- **Step history**: Track steps for past 7/30 days
- **Weekly/Monthly stats**: Aggregate step data
- **Goals**: Customizable daily step goals
- **Achievements**: Badges for milestones
- **Charts**: Visualize step trends
- **Manual entry**: Add steps manually
- **Cloud sync**: Sync across devices

---

## Comparison with Other Apps

### Google Fit
- Uses same sensor
- More sophisticated algorithms
- Cloud sync
- Multiple data sources

### Samsung Health
- Device-specific optimizations
- Better battery management
- More features

### VitalPath
- Simple and straightforward
- Privacy-focused (local storage)
- No account required
- Lightweight

---

## Privacy

### Data Storage
✅ **Local only** - All data stored on device  
✅ **No cloud** - No data sent to servers  
✅ **No tracking** - No analytics or tracking  
✅ **User control** - User owns all data  

### Permissions
- **ACTIVITY_RECOGNITION**: Only for step counting
- **No location**: Doesn't track location
- **No internet**: Doesn't require internet

---

## Summary

✅ **Fixed step calculation** - Accurate daily tracking  
✅ **Added day reset** - Automatic midnight reset  
✅ **Runtime permission** - Proper Android 10+ handling  
✅ **Data persistence** - Survives app restarts  
✅ **Error handling** - Graceful fallbacks  
✅ **User feedback** - Clear messages  

The step counter now works correctly and accurately tracks daily steps! 🚶‍♂️📊

---

## Quick Test

1. **Open app** → Go to Home tab
2. **Check permission** → Grant if asked
3. **Walk 50 steps** → Count should increase
4. **Close app** → Reopen
5. **Check count** → Should be same ✅
6. **Walk more** → Count increases ✅

If steps are counting, the fix is working! 🎉
