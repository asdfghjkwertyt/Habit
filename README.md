# Habit Tracker (Android)

Production-grade Habit Tracker app built with Kotlin, Jetpack Compose, MVVM, Room, and Hilt.

## Tech Stack
- Kotlin
- Jetpack Compose + Navigation Compose
- MVVM architecture
- Room database (local persistence)
- Hilt dependency injection
- AlarmManager notifications/reminders

## Features
- Add, edit, delete habits
- Daily completion tracking
- Streaks and statistics
- Monthly interactive calendar with completion highlights and history
- Reminder scheduling per habit (enable/disable, daily time, custom message)
- In-form "Send Test Reminder Now" action for quick notification verification
- Statistics dashboard with total completions, current/longest streak, weekly bar chart, and monthly line chart

<!-- Workflow trigger -->
- Dark mode support (Material 3 + dynamic color, System/Light/Dark manual toggle)

## Home Screen
- Built with Jetpack Compose + Material 3
- Responsive list/grid layout (`LazyColumn` on phones, adaptive `LazyVerticalGrid` on larger screens)
- User-selectable layout mode toggle (Auto, List, Grid)
- Layout mode preference is persisted across app restarts using DataStore
- State-hoisted callbacks for add, toggle-complete, refresh, and retry
- Pull-to-refresh support and smooth card transitions
- Streak count visible per habit item
- Floating Action Button for adding habits

## Navigation State
- Selected bottom tab is persisted across app restarts using DataStore
- Calendar selected habit is persisted across app restarts using DataStore

## Navigation
- Navigation Compose graph includes: Home, Calendar, Statistics, Add Habit, Edit Habit
- Edit Habit is accessible from Home habit cards
- Bottom-tab navigation preserves state and avoids duplicate destinations (`launchSingleTop` + `restoreState`)
- Add/Edit back navigation uses `navigateUp()` with fallback to Home for robust back-stack handling

## Project Structure
- `data`
  - `local/entity`
  - `local/dao`
  - `local/AppDatabase.kt`
  - `repository`
- `domain`
  - `model`
  - `repository`
  - `usecase`
- `ui`
  - `screens`
  - `components`
  - `viewmodel`
  - `navigation`
  - `theme`
- `di`
- `notifications`
- `util`

## Build
1. Open this workspace in Android Studio or VS Code with Android SDK configured.
2. Ensure local Android SDK paths are present in `local.properties`.
3. Run Gradle build:
   - `./gradlew assembleDebug`

## VS Code One-Click Launch
1. Start an Android emulator or connect a physical device with USB debugging enabled.
2. Run the VS Code launch profile `Android: One-Click Install + Launch`.
3. Optional: run `Android: Filtered Logcat` to stream app/runtime logs.

The one-click flow runs these tasks in sequence:
- `Android: Check Device`
- `Android: Install Debug`
- `Android: Launch App`

## Notes
- `BootReceiver` reschedules enabled reminders after reboot.
- To keep the starter clean, notification click deep-link handling is not yet included.
