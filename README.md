# Shopping List Application

A collaborative household shopping list Android application built with Kotlin, Clean Architecture, MVVM, and Firebase.

## Features
- Real-time shared shopping list
- Firebase Authentication (Email/Password)
- Offline-first approach with Room caching
- WorkManager for background sync
- Android Home Screen Widgets (2x2 and 4x4)
- Swipe to delete and mark as bought

## Architecture
This app follows Clean Architecture principles:
- **Domain:** Models, Repository interfaces, UseCases
- **Data:** Room Database, Firebase implementations, Repository implementations
- **Presentation:** ViewModels, Activities/Fragments, Adapters

## Setup Instructions
1. Open this project in Android Studio. Let Gradle sync.
2. Go to [Firebase Console](https://console.firebase.google.com/) and create a new project.
3. Enable **Authentication** (Email/Password provider).
4. Enable **Firestore Database** in test mode or with proper rules.
5. Add an Android app in Firebase settings using the package name `com.example.shoppinglist`.
6. Download the `google-services.json` file and place it in the `app/` directory of this project.
7. Build and run the app.
