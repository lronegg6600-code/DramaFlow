# Local App Smoke Report

- current state: `blocked before app launch`
- blocker: `no ready Android emulator/device`
- backend health: `ready`
- local debug env: `ready`
- debug APK: `built`
- script status: `all pass`
- App-level smoke: `not executed`

## Why Blocked
- `adb devices` returned no booted device
- all available AVDs point at missing `android-35/google_apis_playstore/x86_64` system images
- the local `sdkmanager.bat` runtime cannot auto-repair the missing image under the current Java toolchain
