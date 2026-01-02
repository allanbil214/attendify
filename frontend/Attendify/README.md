# Attendify - Android Attendance Tracking App

Attendify is a modern Android application built with Kotlin and Jetpack Compose that provides a complete solution for employee attendance tracking. It integrates with a backend API to handle user authentication, location-based check-ins/check-outs, attendance history, and offline support.

## ✨ Features

- **Splash Screen**: Checks user authentication status and navigates to the appropriate screen.
- **Secure Authentication**: User registration and login with secure token storage using EncryptedSharedPreferences.
- **Home Dashboard**: Displays the user's profile, today's attendance status (Checked In, Checked Out, or pending), and provides quick access to primary actions.
- **Location-Aware Check-In**: 
    - Integrates OpenStreetMap (OSMDroid) to display the user's current location and nearby office locations.
    - Validates if the user is within the allowed radius of an office before allowing a check-in.
- **Photo Capture**: Uses CameraX to capture a selfie during both check-in and check-out for verification.
- **Image Upload & Compression**: Compresses captured images on the client-side before uploading them to the server to save bandwidth and storage.
- **Attendance History**: A paginated list of all past attendance records, which can be filtered and refreshed.
- **Offline Support**: 
    - Caches attendance and user data locally using the Room database.
    - Queues check-in/check-out records made offline and syncs them automatically when the network is available using WorkManager.
- **User Profile**: A dedicated screen to view user information and log out.

## 🛠️ Technology Stack & Architecture

This project follows modern Android development best practices, including a clean, multi-layered architecture.

- **Core Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 design.
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture (Presentation → Domain → Data layers).
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/) for managing dependencies across the app.
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://kotlinlang.org/docs/flow.html) for managing background tasks and data streams.
- **Networking**: [Retrofit](https://square.github.io/retrofit/) for type-safe HTTP requests and [OkHttp](https://square.github.io/okhttp/) as the underlying client.
- **Local Storage**: [Room](https://developer.android.com/training/data-storage/room) for robust, local database storage and offline caching.
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for navigating between screens.
- **Location Services**: Google Play Services' Fused Location Provider for accurate and efficient location tracking.
- **Mapping**: [OSMDroid](https://github.com/osmdroid/osmdroid) for a free and open-source map solution.
- **Camera**: [CameraX](https://developer.android.com/training/camerax) for a consistent and easy-to-use camera API.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for fast and efficient image loading in Compose.
- **Background Jobs**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for deferrable, guaranteed background tasks like data synchronization.
- **Security**: [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) for securely storing sensitive data like authentication tokens.

## 📂 Project Structure

The codebase is organized into three main layers to promote separation of concerns and maintainability:

- **`data`**: Contains the implementations of repositories, API service definitions (Retrofit), Room database DAOs and entities, and DTOs (Data Transfer Objects).
- **`domain`**: Holds the core business logic, including domain models, repository interfaces, and use cases (though use cases are implicitly handled in ViewModels in this implementation).
- **`ui`**: Includes all Jetpack Compose screens, ViewModels, UI state management, navigation graph, and theming.
- **`di`**: Hilt modules for providing dependencies.
- **`utils`**: Helper and utility classes, such as for image compression and location calculations.

## 🚀 Potential Improvements

While this app provides a robust MVP, there are several areas for future improvement:

#### Functionality
- **Full Offline Check-Out**: Implement a more robust offline check-out flow in the `SyncWorker`.
- **Forgot Password**: Add a complete "Forgot Password" flow.
- **Profile Picture Upload**: Allow users to upload and change their profile picture.
- **Advanced History Filtering**: Implement date range pickers and status filters on the attendance history screen.
- **Admin/Manager Panel**: Build out the "Locations" screen for admins to manage office locations.
- **Real-time Notifications**: Use FCM to send reminders for check-in/out or to confirm successful offline syncs.

#### Technical & Architectural
- **Comprehensive Testing**: Add unit tests for ViewModels, repositories, and utils. Implement UI tests for key user flows using Compose test APIs.
- **Paging3 Library**: Integrate the `Paging3` library for more efficient and feature-rich pagination on the History screen.
- **Modularization**: As the app grows, break down features (e.g., `:feature:auth`, `:feature:attendance`) into separate Gradle modules to improve build times and enforce separation.
- **CI/CD Pipeline**: Set up a continuous integration and deployment pipeline (e.g., using GitHub Actions) to automate builds and testing.
- **Advanced Error Handling**: Implement a more centralized error handling mechanism to display contextual error messages or retry options consistently.

#### UI/UX
- **Loading Skeletons**: Replace generic `CircularProgressIndicator`s with skeleton loaders for a smoother perceived performance.
- **Animations & Transitions**: Add meaningful animations for screen transitions and state changes.
- **Tablet & Foldable Support**: Create adaptive layouts to ensure a great user experience on large-screen devices.
