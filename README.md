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

## Recent changes
- **Scrollable dashboard**: The mpv control screen now supports smooth vertical scrolling to accommodate all controls and terminal output on devices with limited screen space.
- **Terminal toggle button**: Added a "Show/Hide" button above the terminal output for hiding the terminal panel and focusing on controls.
- **Symlink support in file browser**: The remote file browser now follows symbolic links when browsing the file system, allowing access to linked files and directories.

## Setting up FFmpeg (for local media streaming)
Local media streaming requires FFmpeg in two places:

1. **On the Android device** (to encode local media as RTMP stream)
   - For most devices, FFmpeg is not included by default
   - See methods below to install FFmpeg on your Android device

2. **On the remote Linux machine** (to receive and handle the RTMP stream and connect to mpv)
   - This is the primary requirement for streaming to work

### Installing FFmpeg on Android device:

#### Method 1: Using Termux (Recommended for non-rooted devices)
1. Install [Termux](https://termux.com/) from F-Droid or Play Store
2. Open Termux and run:
   ```bash
   pkg update
   pkg install ffmpeg
   ```
3. Grant storage permissions when prompted
4. Verify installation:
   ```bash
   ffmpeg -version
   ```
5. The app will auto-detect FFmpeg from Termux when available

#### Method 2: Using Magisk modules (For rooted devices)
1. Install [Magisk](https://magisk.me/) on your rooted Android device
2. Search for FFmpeg module in Magisk Manager
3. Install the module and reboot
4. Verify installation after reboot

#### Method 3: Pre-compiled binaries (Advanced)
1. Download FFmpeg binaries for Android ARM64 from:
   - [BtbN FFmpeg Android builds](https://github.com/BtbN/FFmpeg-Builds)
   - [Static FFmpeg builds](https://johnvansickle.com/ffmpeg/)
2. Extract to a location accessible to the app (e.g., `/data/local/tmp/ffmpeg`)
3. Make executable:
   ```bash
   adb push ffmpeg /data/local/tmp/ffmpeg
   adb shell chmod +x /data/local/tmp/ffmpeg
   ```

### Installing FFmpeg on the remote Linux machine:

#### On Ubuntu/Debian:
```bash
sudo apt-get update
sudo apt-get install ffmpeg
```

#### On Fedora/RHEL/CentOS:
```bash
sudo dnf install ffmpeg
```

#### On Arch Linux:
```bash
sudo pacman -S ffmpeg
```

#### Verify installation:
```bash
ffmpeg -version
```

### How it works:
1. The Android app picks a local media file from device storage
2. FFmpeg on the Android device encodes it as an RTMP stream
3. The RTMP stream is sent to the remote Linux machine (e.g., `rtmp://hostname:1935/live/stream-key`)
4. FFmpeg on the Linux machine receives the RTMP stream and feeds it to mpv
5. mpv plays the stream and outputs to the display

**Important notes:**
- If FFmpeg is unavailable on the Android device, the app will display a warning in the "Local Media Stream" card
- The remote machine also requires FFmpeg for the mpv integration to work properly
- Termux is the easiest method for most non-rooted Android devices
- Make sure both devices have FFmpeg for local media streaming to function

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

