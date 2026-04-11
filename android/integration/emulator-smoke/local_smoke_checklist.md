# Local App Smoke Checklist

## Preconditions
- backend health check passes on `127.0.0.1:8081~8087`
- Android debug env points to `10.0.2.2:8081~8087`
- emulator/device is booted and visible in `adb devices`
- debug APK exists at `android/app/build/outputs/apk/debug/app-debug.apk`

## Execute
1. Install debug APK.
2. Launch `com.dramaflow.app.debug/com.dramaflow.app.MainActivity`.
3. Run feed/detail walkthrough.
4. Run playback walkthrough.
5. Run billing/entitlement walkthrough.
6. Run revoke/restore walkthrough.
7. Capture logcat, screenshots, and evidence summary.
