# Phase 6 RTMP Design (Feature 13)

## Goal
Allow users to pick local media on Android and play it remotely in `mpv` by streaming to the Linux host over RTMP.

## Scope for this phase
- Produce a validated design and rollout plan.
- Do **not** implement full streaming in this phase.
- Keep existing architecture: `UI -> ViewModel -> AppRepository -> SshManager`.

## Stack decision
- **Selected stack:** FFmpeg-based RTMP publishing.
- **Reason:** fastest MVP path, stable RTMP behavior, and lower integration risk than building a MediaCodec + RTMP pipeline first.
- **Deferred:** MediaCodec-native pipeline is an optimization candidate after MVP stabilizes.

## End-to-end flow
1. User taps `Play Local Media` in dashboard.
2. Android opens system document picker (`ACTION_OPEN_DOCUMENT`) and stores URI permission.
3. App starts local stream session (foreground service) and reads media from selected URI.
4. App sends stream to remote RTMP endpoint (`rtmp://<host>:1935/live/<streamKey>`).
5. App triggers remote `mpv` command over SSH to open the same RTMP URL.
6. Dashboard shows stream state and terminal output; user can stop stream and remote playback.

## Architecture proposal
- **UI layer (`DashboardScreen`)**
  - New actions: `Play Local Media`, `Stop Local Stream`.
  - Stream status card: `Idle`, `Preparing`, `Streaming`, `Error`.
- **ViewModel (`DashboardViewModel`)**
  - Holds stream state and user-facing error text.
  - Starts/stops stream service and sends `mpv` open/stop commands through existing repository path.
- **Streaming layer (new)**
  - `LocalMediaStreamService` (foreground service) for long-running upload.
  - `RtmpStreamer` interface with implementation wrapper for chosen encoder/RTMP library.
- **SSH layer (existing)**
  - Reuse `sendCommand()` for remote lifecycle commands.

## Android-side design details
### Media selection
- Use `ActivityResultContracts.OpenDocument()` with MIME filter `video/*` and `audio/*`.
- Persist URI permission via `takePersistableUriPermission()`.

### Foreground execution
- Use foreground service + persistent notification while streaming.
- Add cancellation from notification action.

### FFmpeg dependency plan
- Add FFmpeg Android wrapper dependency (FFmpeg-kit package variant chosen by codec needs).
- Start with the smallest package that supports RTMP + H.264/AAC path used by target devices.
- Keep FFmpeg interaction behind `RtmpStreamer` to allow future replacement.

### Proposed stream command shape
- Input: `content://...` from SAF URI.
- Output URL: `rtmp://<host>:1935/live/<streamKey>`.
- Tune for stable playback over quality in MVP (moderate bitrate, keyframe interval tuned for seek/start).

### MVP FFmpeg command template
```
ffmpeg -re -i <inputUri>
  -c:v libx264 -preset veryfast -tune zerolatency -pix_fmt yuv420p
  -c:a aac -b:a 128k -ar 44100
  -g 60 -keyint_min 60
  -f flv rtmp://<host>:1935/live/<streamKey>
```
- Notes:
  - `-re` keeps send rate near real-time for local files.
  - GOP/keyframe values are chosen for predictable startup latency.
  - For unsupported device codec paths, fall back to FFmpeg software encoding profile.

### Streaming wrapper contract (new)
- `RtmpStreamer.start(inputUri: Uri, publishUrl: String, options: StreamOptions): Flow<StreamEvent>`
- `RtmpStreamer.stop()`
- `StreamEvent`: `Preparing`, `Running`, `Stats(bytesSent, bitrateKbps)`, `Retrying(attempt)`, `Failed(message)`, `Stopped`

## Remote host requirements
- Run RTMP ingest server (recommended: NGINX + RTMP module) on target host.
- Required endpoint pattern:
  - Publish: `rtmp://0.0.0.0:1935/live/<streamKey>`
  - Play: `rtmp://127.0.0.1:1935/live/<streamKey>`
- Minimal host setup checklist:
  1. Install and enable RTMP service.
  2. Open firewall for TCP 1935 only from trusted network.
  3. Configure stream key policy (static key per profile initially).
  4. Validate with `ffplay` or `mpv` locally before app integration.

## SSH command integration
- Start playback command example:
  - `nohup mpv --force-window --input-ipc-server=/tmp/mpvsocket "rtmp://127.0.0.1:1935/live/<streamKey>" >/tmp/mpv.log 2>&1 &`
- Stop playback command example:
  - `echo "stop" | socat - /tmp/mpvsocket`
- Optional cleanup:
  - Stop app-side stream first, then stop remote playback.

## Security and privacy notes
- Never store raw passwords in logs/terminal output.
- Treat stream key like a credential; do not echo it in UI history.
- Prefer LAN/VPN usage for MVP; TLS RTMPS can be Phase 6.2 hardening.

## Reliability checklist (required deliverable)
- Network interruption handling: retry with backoff, max retry cap.
- Stream health watchdog: bytes sent, recent frame timestamp, reconnect attempts.
- User cancellation path: immediate stream stop + service teardown.
- App lifecycle handling: survive background state while foreground service is active.
- Remote mismatch handling: if RTMP server unavailable, show actionable error in snackbar + terminal.

## Rollout milestones
### Milestone 6.1 (Design validation)
- Finalize FFmpeg package variant and dependency pin.
- Validate one manual RTMP publish and remote `mpv` playback on sample host.

### Milestone 6.2 (MVP implementation)
- Add local media picker + foreground stream service + dashboard stream state.
- Start/stop streaming integrated with SSH playback commands.
- Add runtime permission and notification handling.
- Add minimal stream retry policy (bounded attempts + backoff).

### Milestone 6.3 (Hardening)
- Add retries, richer telemetry, and safer redaction in terminal/history.
- Add optional RTMPS and stream key rotation support.

## Acceptance criteria for Phase 6 completion
- User can select a local media file and start remote playback via RTMP.
- Stream state is visible in dashboard and errors are user-actionable.
- Stop action ends both local upload and remote playback.
- Streaming survives temporary app backgrounding while foreground service is active.
- No credential/stream key leakage in terminal history.


