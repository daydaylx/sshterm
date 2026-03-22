# Manual Device Checklist

## Session Basics
- Connect to a password-based host.
- Connect to a private-key host.
- Verify unknown host key prompt appears and `Trust always` persists.

## Clipboard
- Long-press in the terminal and drag across a single line.
- Copy selected text and verify clipboard contents.
- Paste clipboard text back into the active shell.
- Verify selection clears on copy, cancel, resize, reconnect and disconnect.

## tmux
- Enable `Auto-attach tmux` with an empty session name and verify the default session `main` is used.
- Set a custom tmux session name and verify reconnect returns to that session.
- Connect to a host without `tmux` installed and verify shell fallback plus visible status.

## Background and Reconnect
- Put the app in background and return to the same session.
- Swipe the task away and confirm grace-period notification is shown.
- Return during grace period and verify the session survives.
- Trigger network loss and confirm reconnect or explicit failure state is shown.
- Use notification `Reconnect` and `Disconnect` actions.

## Tailscale
- Connect via Tailnet IP.
- Connect via MagicDNS hostname.
- Disable Tailscale on the device and verify failure messaging is understandable.
