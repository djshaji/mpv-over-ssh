# Implement following features in the app:
1. Enter url to play media from the dashboard.
2. New buttons:
   - Play URL: Prompts the user to enter a media URL and sends the appropriate command to mpv.
   - Stop: Sends a command to stop playback.
   - Next/Previous: Commands to navigate through the playlist.
3. Seek slider: A slider to seek through the currently playing media. (no need to implement the actual seeking logic, just the UI component and command sending: seek to fixed percentage of the media length, e.g., 50% when the slider is at the middle).
4. Launch mpv: A button to start the mpv process on the remote host if it's not already running.
5. Display current media title: A text view that shows the title of the currently playing media, which can be retrieved using a command like `echo "get_title" | socat - /tmp/mpvsocket`.
6. Navigation improvements: Add a back button to the dashboard to return to the profile selection screen, and ensure that the app handles navigation correctly when the back button is pressed.
7. Error handling: Implement error handling for SSH command execution, such as displaying a toast message if a command fails or if the connection is lost.
8. UI enhancements: Improve the visual design of the dashboard with better spacing, icons for buttons, and a more polished layout using Material 3 components.
9. Navigate filesystem: Add a file picker to the dashboard that allows users to browse the remote filesystem and select media files to play with mpv. This can be implemented using a simple list of directories and files retrieved via SSH commands.
10. Connection status indicator: Add a visual indicator on the dashboard that shows the current connection status (e.g., connected, disconnected, connecting) to provide feedback to the user about the SSH connection state.
11. Command history: Implement a command history feature that allows users to see a list of previously executed commands and their outputs in the terminal view, with the ability to re-execute commands from the history.
12. Custom command input: Add a text input field on the dashboard where users can enter custom commands to be sent to the remote host, allowing for more advanced control and flexibility beyond the predefined buttons.
13. Multi-profile support: Enhance the profile management to allow users to switch between multiple SSH profiles easily from the dashboard, enabling quick access to different remote hosts without needing to return to the profile selection screen.
14. Dark mode support: Implement a dark theme for the app that can be toggled by the user, providing a more comfortable viewing experience in low-light environments. This can be achieved by defining a dark color scheme in the Material 3 theme setup and allowing users to switch between light and dark modes in the app settings.
15. Media metadata display: Extend the dashboard to show additional metadata about the currently playing media, such as duration, current playback time, and file size. This information can be retrieved using appropriate mpv commands and displayed in a user-friendly format on the dashboard.