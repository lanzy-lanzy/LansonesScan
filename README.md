# Lansones Disease Scanner

An Android application for detecting diseases in lansones fruit and leaves using AI-powered image analysis.

## Project Setup

This project is built with:
- **Kotlin** for Android development
- **Jetpack Compose** for modern UI
- **Room Database** for local data storage
- **CameraX** for camera functionality
- **OkHttp & Gson** for network operations
- **Navigation Compose** for app navigation
- **Material Design 3** for UI components

## Architecture

The project follows Clean Architecture principles with:
- **Presentation Layer**: Compose UI and ViewModels
- **Domain Layer**: Use cases and business logic
- **Data Layer**: Repositories, database, and API clients

## Project Structure

```
app/src/main/java/com/ml/lansonesscan/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # API clients and DTOs
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic use cases
├── presentation/
│   ├── dashboard/      # Dashboard screen
│   ├── analysis/       # Analysis screen
│   ├── history/        # History screen
│   ├── settings/       # Settings screen
│   └── navigation/     # Navigation setup
├── di/                 # Dependency injection (to be added)
└── util/               # Utility classes
```

## Configuration

### API Keys
Add your Gemini API key to `local.properties`:
```
GEMINI_API_KEY=your_actual_api_key_here
```

### Build Variants
- **Debug**: Development build with debugging enabled
- **Release**: Production build with ProGuard optimization

## Dependencies

Key dependencies include:
- Room Database for local storage
- CameraX for camera functionality
- Navigation Compose for navigation
- OkHttp for HTTP requests
- Gson for JSON parsing
- Accompanist Permissions for runtime permissions

## Getting Started

1. Clone the repository
2. Add your Gemini API key to `local.properties`
3. Build and run the project

## Next Steps

The project structure is now ready for implementation. Follow the tasks in `.kiro/specs/lansones-disease-scanner/tasks.md` to implement the features.