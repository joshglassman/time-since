# TimeSince

An Android app for tracking recurring tasks by how long it has been since you last did them. Each task has a frequency (e.g. "every 2 weeks"), and the app surfaces what's overdue, sends a notification when items slip past their window, and optionally keeps data in sync across devices via Google Drive.

Built with **Kotlin Multiplatform** and **Compose Multiplatform**. The `:shared` module targets both Android and JVM so domain, data, and presentation code (including ViewModels) can be reused if additional platforms are added later.

## Features

- Recurring tasks with custom frequencies (hours / days / weeks / months / years)
- Calendar-correct deadlines computed in your time zone (a "1 month" task lands on the same day next month; DST-aware)
- Time since last done and overdue status at a glance, with a heat-gradient progress bar that fills from just-completed toward the deadline
- **Snooze** a task to push its deadline out until the next completion, with a 💤 indicator on the list
- **Undo** an accidental completion or snooze (from a Snackbar or the edit screen)
- **Pause** a task to freeze its countdown, with a ⏸ indicator on the list, then resume right where it left off — no false overdue for time spent paused
- **Archive** tasks to park them out of the active list (and bring them back unchanged)
- **Categories** with a custom color and an emoji icon (picked from the system emoji picker), shown as a corner badge on each task; filter the list by category, paused, or archived from a dropdown
- **Home-screen widget** (Glance) mirroring the active task list; tapping a task opens the app and scrolls to it
- WorkManager-driven notifications for overdue tasks
- Local-first storage via Room + bundled SQLite
- Two-way Google Drive sync with conflict resolution
- Markdown import and export for data portability

## Google Drive sync (optional)

Sign in with a Google account from the Settings screen. Sign-in goes through Credential Manager (Google ID). Drive access is granted via Identity Services authorization scoped to the app's Drive `appDataFolder`. No scopes for the user's general Drive contents are requested.

## Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin |
| Architecture | Compose Multiplatform (Material 3) |
| Persistence | Room (KSP) + SQLite bundled, Google Drive |
| Auth | Credential Manager + Google ID (sign-in), Identity Services Authorization (Drive scope) |
| Testing | `kotlin-test`, `kotlinx-coroutines-test`, Turbine |

## Project structure

```
shared/       Kotlin Multiplatform module (android + jvm targets)
  commonMain/   domain, data, presentation (ViewModels), Koin module
  commonTest/   use-case and repository tests (run on JVM)
androidApp/   Android application
  ui/           Compose screens + navigation
  di/           Android Koin bindings (Room DB, ViewModels, sync, notifications)
  notification/ OverdueCheckWorker + BootReceiver
  sync/         Google Drive sync implementation
  widget/       Glance home-screen widget
```

## Dev environment setup

### Requirements

- JDK 25 (the Gradle toolchain will fetch one via the foojay resolver if needed)
- Android SDK with `compileSdk` 36 / `minSdk` 26
- Android Studio (recommended) or any IDE with Kotlin + Gradle support

### Build & run

```bash
# Install debug build to a connected device / emulator
./gradlew :androidApp:installDebug

# Assemble a debug APK without installing
./gradlew :androidApp:assembleDebug
```

### Tests

```bash
# Fast host-side tests (most tests live here)
./gradlew :shared:jvmTest

# All shared-module tests (android host + jvm)
./gradlew :shared:test
```

## Repository layout notes

- Version pins live in [gradle/libs.versions.toml](gradle/libs.versions.toml), add new dependencies there rather than inline
