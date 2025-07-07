# Reachability
A Kotlin Multiplatform Mobile (KMM) library for monitoring network connectivity and reachability across iOS and Android platforms.

## Features

- 🌐 Cross-platform network reachability monitoring
- 📱 Native iOS and Android implementations
- 🔄 Real-time connectivity status updates
- 📡 Connection type detection (WiFi, Cellular, etc.)
- 🎯 Reactive API with Kotlin Coroutines and Flow
- 🧩 Easy integration with existing KMM projects

## Installation

### Gradle (Kotlin DSL)

Add the following to your `build.gradle.kts`:

```kotlin
commonMain {
    dependencies {
        implementation("io.github.hipporasy:reachability:1.0.0")
    }
}
```

### Gradle (Groovy DSL)

```groovy
commonMain {
    dependencies {
        implementation 'io.github.hipporasy:reachability:1.0.0'
    }
}
```
