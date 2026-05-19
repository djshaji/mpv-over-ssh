# mpv over ssh

Android app for managing SSH profiles and sending mpv control commands to a remote Linux machine.

## What it does
- Stores SSH host profiles in Room (`SshProfile` / `AppDatabase`).
- Uses SSH/JSch to execute commands on the remote host.
- Provides a Compose UI with:
  - adaptive profile management (`ProfilesScreen`)
  - mpv control dashboard + terminal output (`DashboardScreen`)
  - local HTTP streaming for selected media files (`DashboardScreen`)

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

## Recent changes
- **Scrollable dashboard**: The mpv control screen now supports smooth vertical scrolling to accommodate all controls and terminal output on devices with limited screen space.
- **Terminal toggle button**: Added a "Show/Hide" button above the terminal output for hiding the terminal panel and focusing on controls.
- **Symlink support in file browser**: The remote file browser now follows symbolic links when browsing the file system, allowing access to linked files and directories.

## Setting up local HTTP streaming
Local media streaming now uses a simple HTTP server inside the Android app.

1. **On the Android device**
   - No foreground service is required for local streaming in the current flow.
   - The app serves the selected file directly over HTTP while the dashboard is open.

2. **On the remote Linux machine**
   - `mpv` must be able to reach the Android device over the network.
   - The phone and remote machine should be on the same LAN or otherwise routable.

### How it works:
1. The Android app picks a local media file from device storage.
2. The app starts a small HTTP server and serves that file from the phone.
3. The dashboard sends an SSH command to the remote Linux machine.
4. The remote machine opens the HTTP URL in `mpv`.
5. Playback stops when `mpv` finishes or when the user cancels the stream.

**Important notes:**
- Keep the dashboard open while streaming, since the server now runs inside the app process.
- The remote machine does not need FFmpeg for this HTTP mode.
- If the phone is not reachable from the remote host, streaming will fail even though the file is selected.
- `mpv` and SSH still need to be available on the remote Linux machine for playback control.

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

