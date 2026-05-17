# Project Plan

An Android app called "mpv over ssh" that allows users to connect to a Linux PC via SSH using private key-based authentication. The app should be able to execute shell commands (intended for controlling mpv) and display the terminal output in a Jetpack Compose UI. It should use a modern SSH library like 'com.github.mwiede:jsch' or Apache MINA SSHD and handle connections asynchronously using Coroutines.

## Project Brief

# Project Brief: mpv over ssh

An Android application designed to remotely control the **mpv** media player on a Linux machine via a secure SSH connection.
 The app provides a bridge between mobile convenience and desktop media power, featuring a responsive interface and real-time feedback.

### Features
*   **Secure SSH Connection**: Establish encrypted connections using private key authentication to remote Linux hosts.
*   **Integrated Terminal Output**: A live terminal view within the app to monitor command execution results and mpv process logs.
*   **Media Control Interface**: A dedicated control dashboard to send common mpv commands (play/pause, seek, volume, playlist management).
*   **Adaptive Profile Management**: Save and manage multiple SSH server profiles with an interface that adapts seamlessly across phone and tablet form factors.

### High-Level Technical Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Navigation**: Jetpack Navigation 3 (State-driven)
*   **Adaptive Strategy**: Compose Material Adaptive Library (supporting List-Detail or Supporting Pane layouts)
*   **Concurrency**: Kotlin Coroutines for non-blocking SSH operations
*   **SSH Protocol**: JSch (com.github.mwiede:jsch) for robust SSH2 implementation and private key support

## Implementation Steps

### Task_1_Setup_And_SSH_Core: Configure project dependencies and implement the core SSH logic. Add JSch to the project, set up a Room database for SSH profiles, and create a service to handle SSH connections and command execution using Coroutines.
- **Status:** COMPLETED
- **Updates:** - Added JSch dependency ('com.github.mwiede:jsch:0.2.22') and Room dependencies.
- **Acceptance Criteria:**
  - JSch library integrated successfully
  - Room database for SshProfile is functional
  - SshManager can connect and execute a basic command asynchronously
  - Project builds successfully

### Task_2_Profile_Management_UI: Develop the UI for managing SSH server profiles. Use Material 3 and the Compose Adaptive library to create a List-Detail view for adding, editing, and selecting server profiles.
- **Status:** COMPLETED
- **Updates:** - Implemented adaptive List-Detail UI for profile management using NavigableListDetailPaneScaffold.
- **Acceptance Criteria:**
  - Adaptive List-Detail UI implemented for profiles
  - User can create, update, and delete SSH profiles
  - Navigation between profile list and profile details works using Navigation 3
  - UI follows Material 3 guidelines

### Task_3_Control_Dashboard_And_Terminal: Create the main dashboard for mpv control and the terminal output view. Implement buttons for common mpv commands and a scrollable text area to display real-time terminal output from the SSH connection.
- **Status:** COMPLETED
- **Updates:** - Created the Control Dashboard with buttons for Play/Pause, Stop, Seek, and Volume.
- **Acceptance Criteria:**
  - Control dashboard with play/pause, volume, and seek buttons implemented
  - Terminal output view correctly displays command results
  - SSH commands are triggered by UI actions and results are shown in the terminal

### Task_4_Theme_Assets_And_Verification: Finalize the app's visual design and verify functionality. Implement a vibrant Material 3 color scheme, ensure full edge-to-edge display, create an adaptive app icon, and perform a final run-through to ensure stability.
- **Status:** COMPLETED
- **Updates:** - Implemented a vibrant Material 3 color scheme with dynamic color support.
- Finalized full edge-to-edge display with transparent system bars.
- Generated an adaptive app icon representing SSH and media control.
- Performed final code cleanup and stability checks.
- Verified build success.
- All acceptance criteria for the final task and the project have been met.
- **Acceptance Criteria:**
  - Material 3 vibrant color scheme and dynamic color support implemented
  - Full edge-to-edge display active on all screens
  - Adaptive app icon generated and functional
  - Final application builds and runs without crashes on emulator/device
  - All requirements met
- **Duration:** N/A

