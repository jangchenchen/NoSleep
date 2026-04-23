# Repository Guidelines

## Project Structure & Module Organization

This repository is a single-module Android app. The root Gradle files live in `build.gradle.kts`, `settings.gradle.kts`, and `gradle.properties`. App code is under `app/src/main/java/com/qicheng/workbenchkeeper/`.

- `MainActivity.kt` contains the Compose home screen, URL validation, presets, and launch flow.
- `WorkbenchActivity.kt` hosts the WebView, keep-awake behavior, secure window flag, and navigation controls.
- `data/PreferenceStore.kt` persists local settings and user-saved presets with DataStore.
- `model/` contains serializable app state models.
- `app/src/main/res/` contains manifest resources, theme files, network config, and launcher icons.

There are currently no test directories. Use Android defaults when adding tests: `app/src/test/` for JVM tests and `app/src/androidTest/` for instrumentation tests.

## Build, Test, and Development Commands

Use Android Studio's bundled JDK to avoid incompatibility with newer system JDKs:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
GRADLE_USER_HOME="$PWD/.gradle-home" \
./gradlew :app:assembleDebug --no-daemon
```

- `./gradlew :app:assembleDebug` builds the debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew :app:assembleRelease` builds an obfuscated release APK; configure signing before distribution.
- `./gradlew :app:clean` removes generated build outputs.
- `./gradlew :app:testDebugUnitTest` runs JVM unit tests once tests exist.

## Coding Style & Naming Conventions

Use Kotlin with 4-space indentation and trailing commas where already used. Keep Compose functions small and named by UI role, for example `HomeScreen`, `PresetCard`, and `KeepAwakeDurationCard`. Data models should stay in `model/` and use clear immutable `data class` definitions. Avoid adding company-specific URLs, credentials, or business identifiers to source code.

## Testing Guidelines

No automated tests are currently present. For new logic, add JVM tests under `app/src/test/` with names like `NormalizeAccessUrlTest`. Prefer testing pure validation and persistence behavior outside Compose. For WebView and Activity behavior, use instrumentation tests under `app/src/androidTest/`.

## Commit & Pull Request Guidelines

This directory is not currently a Git repository, so no existing commit convention is available. Use short imperative commit messages such as `Add custom URL validation` or `Remove credential storage`. Pull requests should include a concise description, test/build results, screenshots for UI changes, and notes about any persistence or security-impacting changes.

## Security & Configuration Tips

The public app should remain a generic URL keep-awake tool. Do not reintroduce built-in target URLs, username/password fields, hidden tracking, or content logging. `local.properties` is machine-specific and should not be committed in a shared repository.

Release builds support optional signature verification through `officialSigningCertSha256`; see `RELEASE.md`.
