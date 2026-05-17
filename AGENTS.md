# AGENTS.md

## Project at a glance
- Android app for controlling **mpv** on a remote Linux machine over SSH.
- Main flow: `MainActivity` launches a Navigation 3 shell that switches between profile management and the dashboard.
- Source of truth for remote hosts is Room + `SshProfile` (`app/src/main/java/.../data`).
- The historical plan in `.agent/plan.md` matches the implemented architecture; `README.md` now gives the quick-start view.

## Architecture to preserve
- `MpvOverSshApplication` builds shared singletons: `AppDatabase.getDatabase(this)` and `AppRepository(database.sshProfileDao(), SshManager())`.
- `AppRepository` is a thin wrapper around Room and SSH; do not bypass it from UI code.
- `MainActivity` uses `NavDisplay` with `Route.Profiles` and `Route.Dashboard(profileId)`; prefer extending `ui/navigation/Routes.kt` rather than introducing a second route model.
- `ProfilesScreen` uses `NavigableListDetailPaneScaffold` for adaptive list/detail editing; `DashboardScreen` shows command controls plus a terminal output panel.

## Data and SSH rules
- `SshProfile` is the Room entity (`tableName = "ssh_profiles"`); id `0L` means “new profile” and triggers insert in `ProfilesViewModel`.
- `AppDatabase` is named `mpv_over_ssh_database` and currently uses `fallbackToDestructiveMigration()`.
- `SshManager.executeCommand()` runs on `Dispatchers.IO`, configures JSch, optionally loads a private key/passphrase or password, and always clears identities in `finally`.
- The dashboard sends explicit mpv/socket shell commands such as `echo "seek -5" | socat - /tmp/mpvsocket` and `pkill mpv`.

## UI / Compose conventions
- Screens are Compose-first and Material 3-based; state is usually collected with `collectAsStateWithLifecycle()` and ViewModels are created with custom `Factory` classes.
- Profile editing fields are local Compose state in `ProfileDetailPane`; use `remember(profile)` to reset form state when the selected profile changes.
- `DashboardViewModel` owns terminal history and connection state; append command/output text instead of replacing the buffer.
- Theme setup is in `ui/theme/*`; `MpvOverSshTheme()` also forces edge-to-edge system bars.

## External dependencies and integration points
- Gradle versions are centralized in `gradle/libs.versions.toml`.
- Key integrations: Room, Navigation 3, Compose Material 3 adaptive layouts, JSch, Coil, Retrofit/Moshi/OkHttp, DataStore, Camera, and Play Services Location.
- Keep generated code in mind: Room and Moshi use KSP (`ksp` entries in `app/build.gradle.kts`).

## Build / test workflow
- Use the Gradle wrapper from the repo root.
- Common checks: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew connectedDebugAndroidTest`.
- When touching Room/KSP or serialization-backed types, re-run the relevant Gradle build rather than editing generated output.

## Codebase notes
- `ProfilesNavigation.kt` defines an alternate `ProfileRoute`, but `MainActivity` currently navigates with `ui/navigation/Routes.kt`; check usage before expanding navigation.
- The manifest already declares the application class and launcher activity; avoid duplicating permissions or entry points unless required.
- Prefer updating this file when project structure changes so agents keep a single, current source of guidance.


