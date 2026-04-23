# TimeSince

An Android app for tracking recurring tasks by how long it has been since you last did them. Each task has a frequency (e.g. "every 2 weeks"), and the app surfaces what's overdue, sends a notification when items slip past their window, and keeps an optional backup on Google Drive.

Built with **Kotlin Multiplatform** and **Compose Multiplatform**. The `:shared` module targets both Android and JVM so domain, data, and presentation code (including ViewModels) can be reused if additional platforms are added later.

## Features

- Recurring tasks with custom frequencies (days / weeks / months)
- "Time since last done" and overdue status at a glance
- WorkManager-driven notifications for overdue tasks, re-scheduled on reboot
- Local-first storage via Room + bundled SQLite
- Google Drive sync for backup / restore across devices
- Markdown import and export for data portability

## Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin 2.3 |
| UI | Compose Multiplatform (Material 3) |
| Architecture | Clean architecture — domain / data / presentation in `:shared` |
| DI | Koin |
| Persistence | Room (KSP) + SQLite bundled |
| Async / state | Coroutines + Flow |
| Background work | WorkManager |
| Auth | Google Play Services Auth (Drive sync) |
| Dates / time | `kotlinx-datetime` with an injected `Clock` |
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
```

## Getting started

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

### Google Drive sync (optional)

Sign in with a Google account from the Settings screen. Drive sync uses Google Play Services Auth and writes to the app's Drive `appDataFolder`, so no scopes for the user's general Drive contents are requested.

## Repository layout notes

- Version pins live in [gradle/libs.versions.toml](gradle/libs.versions.toml) — add new dependencies there rather than inline
- Room schemas are exported to [shared/schemas/](shared/schemas/) and checked into the repo
- [CLAUDE.md](CLAUDE.md) contains orientation notes intended for AI coding assistants
