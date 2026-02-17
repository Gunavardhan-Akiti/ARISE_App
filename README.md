# ARISE_App

Android application built with Gradle, Jetpack Compose, and Kotlin.

## Project Structure

This is a modern Android application using:
- **Gradle 8.7** - Build automation tool
- **Kotlin 1.9.22** - Programming language
- **Jetpack Compose** - Modern UI toolkit
- **Android Gradle Plugin 8.3.0** - Android build system
- **Material Design 3** - UI components

## Project Setup

### Prerequisites
- JDK 17 or higher
- Android SDK with API Level 34
- Android Studio (recommended) or command-line tools

### Build Configuration

The project uses Kotlin DSL for Gradle configuration:
- `build.gradle.kts` - Root project configuration
- `app/build.gradle.kts` - App module configuration
- `settings.gradle.kts` - Project settings
- `gradle.properties` - Gradle properties

### Key Dependencies

```kotlin
// AndroidX Core
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.6.2
androidx.activity:activity-compose:1.8.1

// Jetpack Compose
androidx.compose:compose-bom:2023.10.01
androidx.compose.ui:ui
androidx.compose.material3:material3

// Testing
junit:junit:4.13.2
androidx.test.ext:junit:1.1.5
androidx.test.espresso:espresso-core:3.5.1
```

## Building the Project

### Using Gradle Wrapper (Recommended)

```bash
# Build the project
./gradlew build

# Assemble debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

### Using Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the project directory
4. Wait for Gradle sync to complete
5. Click Run or use Shift+F10

## Project Structure

```
ARISE_App/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/arise/app/
│   │       │   ├── MainActivity.kt          # Main activity with Compose
│   │       │   └── ui/theme/                # Theme configuration
│   │       │       ├── Color.kt             # Color definitions
│   │       │       ├── Theme.kt             # App theme
│   │       │       └── Type.kt              # Typography
│   │       ├── res/                         # Resources
│   │       │   ├── values/
│   │       │   │   ├── strings.xml          # String resources
│   │       │   │   ├── colors.xml           # Color resources
│   │       │   │   └── themes.xml           # Theme resources
│   │       │   └── drawable/                # Drawable resources
│   │       └── AndroidManifest.xml          # App manifest
│   ├── build.gradle.kts                     # App module build config
│   └── proguard-rules.pro                   # ProGuard rules
├── gradle/
│   └── wrapper/                             # Gradle wrapper files
├── build.gradle.kts                         # Root build config
├── settings.gradle.kts                      # Project settings
└── gradle.properties                        # Gradle properties
```

## Features

### MainActivity
The main entry point of the application featuring:
- Jetpack Compose UI
- Material Design 3 theming
- Dark/Light theme support
- Dynamic color support (Android 12+)

### Compose UI
- Modern declarative UI
- Preview support for rapid development
- Material 3 components
- Responsive layouts

## Development

### Adding Dependencies

Add dependencies in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("androidx.your-library:version")
}
```

### Creating Composables

Create reusable UI components:

```kotlin
@Composable
fun MyComponent() {
    Text(text = "Hello, Compose!")
}
```

### Themes and Styling

Customize the theme in `ui/theme/Theme.kt`:
- Colors: Modify color schemes in `Color.kt`
- Typography: Update text styles in `Type.kt`

## Testing

The project includes:
- Unit tests with JUnit
- Instrumented tests with Espresso
- Compose UI tests

Run tests:
```bash
./gradlew test                 # Unit tests
./gradlew connectedAndroidTest # Instrumented tests
```

## License

See [LICENSE](LICENSE) file for details.

## Requirements

- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Compile SDK: 34

## Note

On first build, Gradle will download required dependencies from Maven Central and Google's Maven repository. Ensure you have internet connectivity for the initial setup.