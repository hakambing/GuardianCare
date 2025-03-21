# GuardianCare Mobile Application

A modern Android application designed to enhance elderly care through real-time fall detection, caretaker notifications, and comprehensive health management. Built with Jetpack Compose and modern Android architecture.

## Features

### For Elderly Users
- **Fall Detection**: Real-time fall monitoring via Bluetooth Low Energy (BLE) connected to M5StickC Plus smartwatch
- **Check-In System**: Simple daily check-in to notify caretakers of well-being
- **Medical Information**: Track and manage medical history, medications, and health metrics
- **Emergency Calls**: Quick access to emergency contacts and services

### For Caretakers
- **Fall Notifications**: Instant alerts when falls are detected
- **Elderly Management**: Monitor multiple elderly users under your care

## Technology Stack

- **UI**: Jetpack Compose with Material 3 design system
- **Architecture**: MVVM with clean architecture principles
- **Networking**: Retrofit for API communication with authentication
- **Local Storage**: DataStore for preferences and session management
- **Concurrency**: Kotlin Coroutines and Flow for asynchronous operations
- **Bluetooth**: BLE integration for smartwatch communication
- **Navigation**: Compose Navigation with drawer-based navigation system
- **Notifications**: Firebase Cloud Messaging for push notifications

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Minimum Android API level 26 (Android 8.0)
- JDK 17
- Kotlin 1.9+

### Installation
1. Clone the repository
```bash
git clone https://github.com/hakambing/GuardianCare.git
cd GuardianCare/GuardianCare_FrontEnd
```

2. Open the project in Android Studio

3. Configure the API endpoint in the app by editing the appropriate configuration file:
```kotlin
// Update the BASE_URL in RetrofitClient.kt
private val BASE_URL = "http://your-api-endpoint/"
```

4. Set up Firebase for push notifications:
   - Create a Firebase project
   - Add your Android app to the Firebase project
   - Download the google-services.json file (contact project owner for details)
   - Place the file in the app/ directory

5. Build and run the application on an emulator or physical device

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/guardiancare/
│   │   │   ├── data/                      # Data layer
│   │   │   │   ├── api/                   # API services and Retrofit client
│   │   │   │   ├── models/                # Data models for API and app
│   │   │   │   └── session/               # Session management
│   │   │   ├── design_system/             # Reusable UI components
│   │   │   ├── navigation/                # Navigation routes and graph
│   │   │   ├── ui/                        # UI layer
│   │   │   │   ├── screens/               # Screen composables
│   │   │   │   ├── theme/                 # App theme and styling
│   │   │   │   └── viewmodel/             # ViewModels for screens
│   │   │   └── utils/                     # Utility classes
│   │   └── res/                           # Resources (drawables, strings, etc.)
│   └── test/                              # Unit and instrumented tests
└── build.gradle.kts                       # App-level build configurations
```

## API Integration

The app connects to the GuardianCare backend API for authentication and data management:

- **Authentication**: Login, register, and token management
- **Profile Management**: View and update user profiles
- **Health Data**: Medical history, medication tracking, and health metrics
- **Notifications**: Push notification handling

## Bluetooth Integration

The app connects to M5StickC Plus devices via Bluetooth Low Energy (BLE) for:

- Fall detection monitoring
- Voice recording for check-ins
- Device configuration

## Multi-language Support

The app supports multiple languages:
- English
- Chinese (Simplified)
- Malay
- Hindi

Language can be changed in the settings screen.

## Testing Accounts

For testing purposes, you can use the following accounts:

Elderly Account:
- Email: elderly@example.com
- Password: password123

Caretaker Account:
- Email: caretaker@example.com
- Password: password123

## Acknowledgments

- Icons and design resources from Material Design
- Open source libraries and tools used in the project
