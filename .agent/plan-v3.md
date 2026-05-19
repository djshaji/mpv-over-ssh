# Prepare for upload on Play Store

## Phase 1: Product feature baseline (done)
- [x] Open files from other apps (e.g. file manager, gallery) and share to the app for streaming.
- [x] Add a "Share to Stream" entry in the system share sheet for supported media types.
- [x] Handle incoming shared content URIs and start the streaming flow directly from the shared file.
- [x] Ask before deleting a profile with a confirmation dialog.
- [x] Add about page with app version, author info, and links to source code and issue tracker.
- [x] Add a "Report an Issue" button that opens the user's email client with a pre-filled email template for bug reports and feedback.

## Phase 2: Listing and policy baseline (done)
- [x] Create high-quality screenshots and promotional graphics.
- [x] Set up Play Store listing with appropriate categories and contact info.
- [x] Ensure all app content complies with Play Store policies.

## Phase 3: Final verification before submission
- [x] Implement `ACTION_SEND` handling in `MainActivity` and auto-start local streaming when launched from shared media.
- [x] Show a profile picker for shared media when multiple profiles exist and no dashboard session is active; reuse active dashboard profile without prompting.
- [x] Implement About dialog entry with version/author and quick links (Source, Issues, Report).
- [x] Implement graceful fallback message when "Report an Issue" mail intent cannot be handled.
- [x] Implement delete confirmation dialog before removing a profile.
- [x] Add button to close remote mpv process.
- [x] Add button to disconnect SSH and reset session state.
- [ ] Verify share-to-stream flow for `video/*`, `audio/*`, and `image/*` from Files and Gallery.
- [ ] Verify incoming shared `content://` URIs still stream after app process restart.
- [ ] Verify local media picker supports device file system images in addition to audio/video.
- [ ] Verify delete-profile confirmation dialog blocks accidental deletes and handles cancel safely.
- [ ] Verify About page links open correctly and app version matches Gradle version.
- [ ] Verify "Report an Issue" intent works when no mail app is installed (graceful fallback message).

## Phase 4: Compliance and release readiness
- [ ] Re-check Play Data Safety answers against current app behavior (SSH profiles, network access, logs).
- [ ] Confirm privacy policy URL is active and matches actual app behavior.
- [ ] Confirm no unnecessary permissions are declared in manifest.
- [ ] Prepare release notes for version `1.0`.
- [ ] Build release artifact and verify signing config in Play Console upload flow.

## Phase 5: QA gate and rollout
- [ ] Run release smoke tests on at least one phone and one tablet/emulator.
- [ ] Run `./gradlew assembleDebug` and `./gradlew test` before upload.
- [ ] Run `./gradlew connectedDebugAndroidTest` on a device if available.
- [ ] Start staged rollout (10% -> 50% -> 100%) and monitor crashes/ANRs.
- [ ] Track first-user feedback for share-to-stream and local media streaming stability.

