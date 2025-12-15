# Alarmly - Charger-Dismiss Alarm App

## Overview
A unique Android alarm app built with Jetpack Compose that can only be dismissed by connecting the device to a charger. No swipe-to-dismiss functionality exists, making it impossible to snooze or dismiss without plugging in the charger.

## Features Implemented

### 1. **Data Layer**
- **Room Database** with AlarmEntity for persistence
- **AlarmDao** for CRUD operations
- **AlarmRepository** for data access
- **Type Converters** for storing repeat days as a list

### 2. **Alarm Scheduling System**
- **AlarmScheduler**: Uses AlarmManager to schedule exact alarms
- **AlarmReceiver**: BroadcastReceiver that triggers when alarm time is reached
- **AlarmService**: Foreground service that:
  - Plays alarm sound (default or custom)
  - Vibrates device
  - Holds wake lock to keep device awake
  - **Registers ChargerReceiver** to listen for power connection
  - **Automatically dismisses** when charger is connected
- **BootReceiver**: Reschedules all enabled alarms after device reboot

### 3. **Charger Detection**
- Dynamic BroadcastReceiver registered in AlarmService
- Listens for `ACTION_POWER_CONNECTED` intent
- Automatically stops alarm sound, vibration, and service when charger connects

### 4. **ViewModels**
- **AlarmListViewModel**: Manages alarm list, toggle, delete operations
- **AlarmDetailViewModel**: Manages alarm creation/editing with state management

### 5. **UI Screens (Jetpack Compose)**

#### Screen 1: Alarm Ringing Screen
- **Full-screen black background**
- **"Good Night" text** with current time display
- **Custom-drawn moon and clouds** illustration using Canvas
- **Instruction text**: "Connect charger to dismiss"
- **NO swipe-to-dismiss button** (as per design requirement)

#### Screen 2: Alarm List Screen
- Shows all scheduled alarms
- Each alarm displays:
  - Time (12:00 am format)
  - Date/repeat info
  - Countdown (e.g., "6h 25m from now")
  - Toggle switch to enable/disable
- Floating Action Button (+) to add new alarm
- Empty state message when no alarms exist

#### Screen 3: Alarm Detail/Creation Screen
- **Interactive Analog Clock** with draggable hour and minute hands
- Digital time display
- **Repeat days selector** with circular day buttons (S M T W T F S)
- **Alarm Sound** setting card
- **Vibration** setting card  
- **Snooze** duration setting card
- Save button to create/update alarm

### 6. **Navigation**
- Navigation Compose setup with 3 screens:
  - `alarm_list` - Main screen
  - `alarm_detail/{alarmId}` - Create/edit screen
  - `alarm_ringing/{alarmId}` - Ringing screen
- MainActivity handles deep linking from notification to show ringing screen

### 7. **Permissions & Manifest**
Configured permissions for:
- `SCHEDULE_EXACT_ALARM` - Schedule precise alarms
- `USE_EXACT_ALARM` - Alternative for exact alarms
- `WAKE_LOCK` - Keep device awake during alarm
- `VIBRATE` - Vibrate device
- `RECEIVE_BOOT_COMPLETED` - Reschedule after reboot
- `FOREGROUND_SERVICE` - Run alarm service in foreground
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Media playback type service
- `POST_NOTIFICATIONS` - Show alarm notifications

## Architecture
- **MVVM Pattern**: ViewModels for business logic, Compose for UI
- **Repository Pattern**: Single source of truth for data
- **Foreground Service**: Reliable alarm playback even when app is in background
- **Room Database**: Persistent storage for alarm data

## Dependencies Added
- Room (database)
- Navigation Compose
- ViewModel Compose
- WorkManager
- Material Icons Extended
- KSP (Kotlin Symbol Processing for Room)

## How It Works

1. **User creates an alarm** in AlarmDetailScreen with time, repeat days, sound, etc.
2. **AlarmScheduler schedules** the alarm using AlarmManager
3. **At alarm time**, AlarmReceiver triggers and starts AlarmService
4. **AlarmService**:
   - Shows foreground notification
   - Plays alarm sound in a loop
   - Vibrates device
   - Registers ChargerReceiver to listen for power connection
5. **When user connects charger**, ChargerReceiver in AlarmService receives ACTION_POWER_CONNECTED
6. **Alarm automatically stops**: sound, vibration, and service terminate
7. **For repeating alarms**, the next occurrence is automatically scheduled
8. **For one-time alarms**, the alarm is disabled after ringing

## Unique Selling Point
Unlike traditional alarms that can be swiped away or use math problems, this alarm ONLY stops when you physically connect your device to a charger. This forces you to get out of bed and plug in your phone, ensuring you're awake!

## Files Created/Modified

### Data Layer
- `data/local/AlarmEntity.kt`
- `data/local/AlarmDao.kt`
- `data/local/AlarmDatabase.kt`
- `data/local/Converters.kt`
- `data/repository/AlarmRepository.kt`

### Alarm System
- `alarm/AlarmScheduler.kt`
- `alarm/AlarmReceiver.kt`
- `alarm/AlarmService.kt`
- `alarm/BootReceiver.kt`

### ViewModels
- `ui/viewmodel/AlarmListViewModel.kt`
- `ui/viewmodel/AlarmDetailViewModel.kt`

### UI Screens
- `ui/screens/AlarmRingingScreen.kt`
- `ui/screens/AlarmListScreen.kt`
- `ui/screens/AlarmDetailScreen.kt`

### Navigation
- `ui/navigation/Screen.kt`
- `ui/navigation/AlarmlyNavHost.kt`

### Configuration
- `app/build.gradle.kts` - Added dependencies
- `gradle/libs.versions.toml` - Added version catalogs
- `AndroidManifest.xml` - Added permissions and components
- `MainActivity.kt` - Navigation setup

## Next Steps (Optional Enhancements)
1. Add custom alarm sound picker
2. Add snooze duration picker dialog
3. Add alarm label/name feature
4. Add alarm history/statistics
5. Add different dismiss methods (e.g., shake, NFC tag)
6. Add gradual volume increase
7. Add weather integration for display
8. Add sleep statistics

## Testing the App
1. Create an alarm for 1-2 minutes in the future
2. Wait for the alarm to trigger
3. You'll see the ringing screen with moon/clouds
4. Try swiping or pressing back (it won't dismiss)
5. Connect your phone to a charger
6. The alarm will automatically stop!

