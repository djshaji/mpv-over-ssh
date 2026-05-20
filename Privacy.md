# Privacy Policy for mpv over ssh

Last updated: 2026-05-20

This Privacy Policy explains how **mpv over ssh** (the "App") handles information when you use it.

## 1) Data we process

The App processes the following data to provide its features:

- SSH profile data you create (for example: profile name, host, port, username, and authentication details you enter).
- Local media file references/URIs you select or share to the App (`audio/*`, `video/*`, `image/*`).
- Command and terminal output history shown inside the dashboard.
- App settings (for example, theme preference).

## 2) Where data is stored

- Data is stored locally on your device (for example, in app local storage/database).
- The developer does **not** provide a cloud backend for account storage, analytics, or syncing for this App.

## 3) Network use

The App uses network access to:

- Connect to remote hosts that **you configure** over SSH.
- Send your requested control/playback commands to those hosts.
- Optionally serve selected local media over a temporary local HTTP URL so your remote mpv instance can play it.

The App does not intentionally send your personal data to the developer's servers.

## 4) Data sharing

The App may share data only in these functional ways:

- With remote SSH hosts you configured, as required to execute commands.
- With apps/providers that supply shared files or content URIs to the App.

The App does not sell personal data.

## 5) Permissions

The App currently requests:

- `INTERNET`: required for SSH communication and local HTTP streaming functionality.

The App does not request advertising ID access.

## 6) Retention and deletion

- SSH profiles and command history remain on your device until you edit/delete them or uninstall the App.
- You can delete SSH profiles in the profile management screen.
- Uninstalling the App removes app-local data according to Android behavior.

## 7) Security

- The App uses SSH for remote command transport.
- No method of transmission or storage is 100% secure; use trusted networks and hosts.
- You are responsible for securing your remote systems and credentials.

## 8) Contact

For privacy questions or requests:

- Open an issue: <https://github.com/djshaji/mpv-over-ssh/issues>
- Project page: <https://github.com/djshaji/mpv-over-ssh>

