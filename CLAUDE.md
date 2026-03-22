# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PDR is an Android application built with Kotlin and modern Android architecture components.

## Configuration

- **Language**: Kotlin
- **Minimum SDK**: API 29 (Android 10)
- **Target SDK**: API 34
- **Architecture**: MVVM + Jetpack

### Package Name

The package name can be modified in `app/build.gradle.kts`:
- `namespace`
- `applicationId`

## Project Structure

```
PDR/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/pdr/
│   │   │   │   ├── App.kt                 # Application class
│   │   │   │   ├── MainActivity.kt        # Main Activity
│   │   │   │   ├── ui/                    # UI layer (Fragments, ViewModels)
│   │   │   │   ├── data/                  # Data layer (models, repositories)
│   │   │   │   └── di/                    # Dependency injection
│   │   │   ├── res/                       # Android resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                          # Unit tests
│   │   └── androidTest/                   # Instrumented tests
│   └── build.gradle.kts
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml                 # Version catalog
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Build Commands

### Initial Setup

Before building, download the Gradle wrapper JAR:

```bash
# Option 1: If Gradle is installed globally
gradle wrapper

# Option 2: Download manually
mkdir -p gradle/wrapper
curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar
```

### Build

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Test

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Lint

```bash
# Run lint checks
./gradlew lint
```

### Clean

```bash
# Clean build artifacts
./gradlew clean
```

## Key Dependencies

| Component | Library |
|-----------|---------|
| ViewModel | androidx.lifecycle:lifecycle-viewmodel-ktx |
| LiveData | androidx.lifecycle:lifecycle-livedata-ktx |
| Navigation | androidx.navigation:navigation-fragment-ktx |
| Database | androidx.room:room-runtime, room-ktx |

## Architecture

The project follows MVVM (Model-View-ViewModel) architecture:

- **View**: Activities and Fragments observe LiveData from ViewModels
- **ViewModel**: Manages UI state and business logic, survives configuration changes
- **Model**: Data layer with repositories and Room database

## Conventions

- Use ViewBinding for view access (enabled in build.gradle.kts)
- Observe LiveData in Fragments, update UI reactively
- Keep business logic in ViewModels, not Activities/Fragments
- Use Kotlin coroutines for asynchronous operations