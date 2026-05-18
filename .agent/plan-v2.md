# Project Plan v2 - Dashboard and UX Expansion

## Scope split
- **MVP in this milestone:** items 1-12.
- **Separate epic:** item 13 (Play Local Media via RTMP), due to streaming complexity and infrastructure needs.

## Constraints to preserve
- Keep architecture: `UI -> ViewModel -> AppRepository -> SshManager`.
- Keep routing model in `ui/navigation/Routes.kt` and `MainActivity`.
- Keep terminal output append-only in `DashboardViewModel` (do not replace whole buffer).

## Phase 1 - Core dashboard controls
**Features:** 1, 2, 3, 4, 11

### Tasks
1. Add URL input field and `Play URL` action.
2. Add control buttons: `Stop`, `Next`, `Previous`, `Launch mpv`.
3. Add seek slider (`0..100`) that sends a fixed-percentage seek command.
4. Add custom command input and send action.
5. Ensure every action appends command + output to terminal panel.

### Acceptance criteria
- User can paste URL and trigger playback.
- All new buttons send the expected remote commands.
- Slider dispatches seek command using current percentage value.
- Custom command field sends raw command and shows output.

## Phase 2 - Navigation, status, and UI polish
**Features:** 5, 7, 9

### Tasks
1. Add dashboard top app bar with back action to profile list.
2. Ensure system back behaves consistently (`Dashboard -> Profiles`).
3. Add connection status indicator (`Connecting`, `Connected`, `Disconnected`).
4. Improve dashboard spacing, icons, grouping, and Material 3 hierarchy.

### Acceptance criteria
- Back button and system back always return to profile selection from dashboard.
- Connection state is visible and updates during command lifecycle.
- Layout is visually improved without breaking existing functionality.

## Phase 3 - Error handling and command history
**Features:** 6, 10

### Tasks
1. Add explicit command result handling in ViewModel for success/failure.
2. Show toast/snackbar when command fails or connection is lost.
3. Append readable error lines to terminal output.
4. Add command history list with re-execute action.

### Acceptance criteria
- Failures produce user-visible feedback and terminal error output.
- Connection-loss state is reflected in status indicator.
- User can re-run a previous command from history.

## Phase 4 - Remote filesystem picker
**Feature:** 8

### Tasks
1. Add remote browser state: current path, entries, loading, error.
2. Retrieve directory/file listing via SSH command and parse it safely.
3. Add a simple picker UI (dialog/sheet) for navigation and file select.
4. Selecting a media file sends mpv play command and logs output.

### Acceptance criteria
- User can browse remote directories and select a file.
- Selected file starts playback command on remote host.

## Phase 5 - Dark mode support
**Feature:** 12

### Tasks
1. Add user theme preference (`System`, `Light`, `Dark`) via DataStore.
2. Apply preference in `MpvOverSshTheme` and preserve edge-to-edge behavior.
3. Add UI toggle entry in settings/dashboard overflow.

### Acceptance criteria
- Theme mode changes immediately and persists app restarts.

## Phase 6 - Separate design epic: Play Local Media via RTMP
**Feature:** 13

### Deliverables
1. Technical design doc for Android-side media selection + streaming pipeline.
2. Remote host requirements (receiver, ingestion command, lifecycle handling).
3. Reliability checklist: bandwidth, reconnect, latency, cleanup, failure UX.

## Validation checklist
1. `./gradlew assembleDebug`
2. `./gradlew test`
3. Manual verification for each phase acceptance criterion above.
