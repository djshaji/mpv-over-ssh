# mpv over ssh

Android app for managing SSH profiles and sending mpv control commands to a remote Linux machine.

## What it does
- Stores SSH host profiles in Room (`SshProfile` / `AppDatabase`).
- Uses SSH/JSch to execute commands on the remote host.
- Provides a Compose UI with:
  - adaptive profile management (`ProfilesScreen`)
  - mpv control dashboard + terminal output (`DashboardScreen`)

## Main architecture
- `MpvOverSshApplication` creates the shared `AppDatabase` and `AppRepository`.
- `MainActivity` hosts the app shell with Navigation 3.
- `ui/navigation/Routes.kt` defines the active app routes.
- `data/` contains Room persistence and the repository wrapper.
- `ssh/SshManager.kt` performs asynchronous SSH command execution with JSch.

## Build and test
Run from the project root with the Gradle wrapper:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
```

## Notable conventions
- `SshProfile.id == 0L` means a new profile and triggers insert logic.
- Dashboard commands are explicit shell snippets like `echo "seek -5" | socat - /tmp/mpvsocket`.
- `MpvOverSshTheme()` enables edge-to-edge system bars.

## Key files
- `app/src/main/java/org/acoustixaudio/opiqo/mpvoverssh/MainActivity.kt`
- `app/src/main/java/org/acoustixaudio/opiqo/mpvoverssh/data/AppRepository.kt`
- `app/src/main/java/org/acoustixaudio/opiqo/mpvoverssh/ssh/SshManager.kt`
- `app/src/main/java/org/acoustixaudio/opiqo/mpvoverssh/ui/profiles/ProfilesScreen.kt`
- `app/src/main/java/org/acoustixaudio/opiqo/mpvoverssh/ui/dashboard/DashboardScreen.kt`

