# Solar Elevation Alarm
Native Android solar-elevation alarms with long-form audio playback, built on-device using Termux.
**Validated device:** Samsung Galaxy XCover6 Pro (Android SDK 36), tested build `v20261022T031609Z`. The app successfully fired an overnight solar alarm after approximately seven hours idle and played WavPack audio. This is a device-specific field test, not a guarantee of wakeup reliability across all Android devices.
## Features
- Solar elevation event angles stored as arbitrary signed decimal text; ascending, descending and both crossings
- Offline JPL DE440s kernel, downloaded on first use into private application storage and SHA-256-verified
- Location acquisition, last-known-location fallback and best-effort 24-hour background location requests
- Android exact alarm-clock scheduling; every delivered crossing schedules the next occurrence
- WavPack (`.wv`) decoding via pinned native WavPack 5.8.1, plus WAV and Android-supported media formats
- Foreground media playback with Bluetooth routing, system media volume and Stop-only controls; no playlist or seeking
- Pure black (`#000000`) and pure red (`#FF0000`) application-owned UI
- On-device diagnostics and clipboard export; compact cards and ARM/DISARM actions
## Directory scheme
Files in this repository deliberately follow the same type-based layout used in Android shared storage: `Documents/java`, `Documents/c`, `Documents/xml`, `Documents/sh`, `Documents/md`, `Documents/txt` and `Music/wv` for a short test tone. There is **no project folder in shared storage**. The compiler builds in Termux-private `$HOME/.solar_elevation_alarm_build`, so the user's shared-storage directory sorter can run normally.
## Building on Termux
The prerequisites used in testing are Termux packages `aapt2`, `openjdk-21`, `d8`, `apksigner`, `zip`, `clang`, `git`, `coreutils`, `unzip` and an Android SDK platform `android.jar` installed at `$HOME/android-sdk/platforms/android-35/android.jar`. AOSP-compatible Android platform tools must be available.
Clone this repo into **Termux private storage** and install its files to their shared-storage file-type directories:
```bash
pkg install git aapt2 openjdk-21 d8 apksigner zip clang coreutils unzip
cd "$HOME"
git clone https://github.com/ov3qpi/Solar-Elevation-Alarm.git
cd Solar-Elevation-Alarm
for type in java c xml sh md txt; do mkdir -p "/storage/emulated/0/Documents/$type"; cp -f "Documents/$type/"* "/storage/emulated/0/Documents/$type/"; done
bash /storage/emulated/0/Documents/sh/SolarElevationAlarm_build_v20261022T031609Z.sh --preflight
bash /storage/emulated/0/Documents/sh/SolarElevationAlarm_build_v20261022T031609Z.sh --install
```
The project reads the Android SDK location from `ANDROID_HOME`/`ANDROID_SDK_ROOT`, and can use `ANDROID_JAR` to override the SDK path. The build fetches upstream WavPack 5.8.1 at the pinned revision and checks it before compiling. It writes a signed APK to `/storage/emulated/0/Documents/apk` and build logs to `/storage/emulated/0/Documents/log`.
**Important:** The build keystore is created and kept only in `$HOME/.solar_elevation_alarm_build`. Preserve it if you want to update an existing installed app. Other people building from source generate different signing keys and therefore cannot install their APK over one signed by the original developer without uninstalling first. Never commit keystores, signing passwords, GPS dumps or user-selected music files.
## Usage
Grant precise foreground location and, for background location polling, Allow all the time. Grant exact alarm permission as appropriate for Android. Use the system document picker to choose an on-device audio file, enter a signed solar elevation angle and direction, inspect the next-crossing ISO timestamp, then arm the event. Disarm without deleting; Start can audition any event. The media playback notification offers terminal Stop behavior. Set the app to Unrestricted battery use on Samsung if available, and test it with a backup alarm before relying on it to wake up.
## Important limitations
- Android may defer the periodic GPS job; there is no guarantee of a fresh background fix exactly every 24 hours.
- The geometric Sun-center elevation model uses approximate precession/nutation and fixed TAI-UTC handling and lacks Earth orientation, refraction and terrain corrections. DE440s precision does not eliminate these uncertainties.
- Android force-stop, battery loss, changing file-provider availability, revoked permissions, Do Not Disturb, muted media volume and vendor power policies can prevent audible playback.
- The original author has field-tested on one XCover6 Pro; additional devices and long-term reliability require independent testing.
- JPL DE440s is downloaded from NASA NAIF at run time; the approximately 31 MB kernel is not shipped in this repository or the APK.
## Licensing
The app-specific source files and repository documentation are dedicated under **CC0-1.0 Universal**; see [LICENSE](LICENSE). WavPack 5.8.1 is **BSD-3-Clause**, copyright David Bryant and contributors, and is downloaded at build time. Its complete upstream license is bundled inside the APK at `assets/wavpack_license.txt`, and is not superseded by CC0. See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
## Tested baseline
Version `v20261022T031609Z` is the first version known to have fired correctly after overnight inactivity on the development phone. This repository intentionally contains the validated v0.5 source, not untested UI or alarm scheduling changes.
