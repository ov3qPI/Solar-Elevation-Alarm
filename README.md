# Solar Elevation Alarm
**Release status:** [`v20261022T151029Z`](https://github.com/ov3qPI/Solar-Elevation-Alarm/releases/tag/v20261022T151029Z) is a **public prerelease** containing the reboot/Stop-button and playback-priority fixes. The reboot UI test passed on the development device; interruption of an active manual audition by a scheduled alarm still needs an on-device test. [`v20261022T031609Z`](https://github.com/ov3qPI/Solar-Elevation-Alarm/releases/tag/v20261022T031609Z) remains the latest stable release.
**Known issue in `v20261022T031609Z`:** When an alarm triggers during another playback session, the incoming alarm can be suppressed. Until the overlap repair in `v20261022T151029Z` is validated, avoid manual auditions that overlap armed alarms and retain a backup alarm.
A native Android alarm clock that triggers long-form audio when the Sun crosses a chosen **geometric solar-center elevation angle** at your location. Built on-device in [Termux](https://termux.dev/) with a deliberately simple **pure black (`#000000`) / pure red (`#FF0000`)** interface.
**First tested release:** `v20261022T031609Z` · Android 12+ (API 31+) · ARM64 (`arm64-v8a`) · Field-tested on a Samsung Galaxy XCover6 Pro (Android SDK 36).
## Download and install
**[Download the latest signed APK from GitHub Releases](https://github.com/ov3qPI/Solar-Elevation-Alarm/releases/latest)**. For the first field-tested build, see **[v20261022T031609Z](https://github.com/ov3qPI/Solar-Elevation-Alarm/releases/tag/v20261022T031609Z)**.
1. Download the `.apk` asset and the accompanying SHA-256 text file. Verify the APK before installation; in Termux, run `sha256sum /path/to/downloaded.apk` and compare its hash against the published checksum.
2. Open the APK and allow installation from the chosen source when Android prompts. Only install APKs you trust.
3. Open Solar Elevation Alarm. Grant **precise location** to calculate crossings and **exact-alarm authorization** if requested. For automatic location refresh while the app is closed, additionally grant **Location → Allow all the time** in Android app settings. Background location is optional if you only use saved or manually refreshed coordinates.
4. Download the JPL DE440s kernel in the app, then use **Verify Kernel**. The download is approximately 31 MB and is stored privately on the device; this requires an internet connection once.
5. Select **Create Solar Event**, choose an audio file, enter any signed decimal solar elevation angle, and choose Ascending, Descending, or Both. The live preview displays the next crossing as a local ISO timestamp when available. Arm the event.
6. Use **DISARM** to cancel a scheduled alarm without deleting its configuration. **START** auditions the audio; the notification's single playback control stops it. The same event is rearmed automatically for its next crossing after each delivered alarm.
**Wake-up safety:** Use a backup alarm until you have tested reliability on your own phone. Android vendor power restrictions, force-stop, muted volume, missing audio files, and revoked permissions can prevent playback. On Samsung, setting the app's battery usage to **Unrestricted** may help.
## What it does
- Calculates rising and setting crossings for arbitrary signed decimal elevation angles using a local downloaded JPL DE440s ephemeris.
- Schedules one-shot Android alarm-clock events and automatically calculates the next valid crossing after delivery.
- Plays selected local audio through the phone's standard media audio routing, including Bluetooth. Supports WavPack (`.wv`) through bundled native WavPack 5.8.1, plus WAV and formats supported by Android's media decoders.
- Offers a minimal terminal **STOP** control, not a seeking interface or playlist. Long-form audio continues playing if the app is swiped out of the Recents overview.
- Maintains last-known position and requests best-effort background location updates around every 24 hours, rescheduling enabled events when a changed position is accepted.
- Lists events in solar traversal order (ascending from low to high angles, then descending from high to low) rather than chronological order; events configured for Both sort by their next resolved direction.
- Provides per-event and full copyable technical diagnostics. **The copied report can contain precise GPS coordinates and file URIs**; review before sharing.
## Permissions and privacy
| Permission or capability | Purpose |
| --- | --- |
| Precise/approximate location | Determine the observer coordinates for solar calculations |
| Background location (optional) | Best-effort location refresh while the app is not open |
| Exact alarms | Deliver solar-crossing triggers as Android alarm-clock events |
| Foreground media service and wake lock | Keep long-form playback active as needed |
| Media file picker | Let the user choose audio without granting broad unrestricted access to storage |
| Internet | Download the pinned JPL DE440s ephemeris if it is not yet present |
The app stores selected events, coordinates, and the ephemeris in application-private storage. It has no account registration, advertising SDK, or telemetry endpoint in the published app source. The ephemeris request contacts a NASA NAIF server; your network provider and the server can observe a download connection. Nothing is uploaded to the project author by the app. There is no built-in data export except diagnostics copied by the user.
## Tested baseline and limits
**v20261022T031609Z successfully fired a scheduled overnight alarm after roughly seven hours of inactivity** on the development Galaxy XCover6 Pro. WavPack long-form playback, Bluetooth routing, media volume, and continued audio after swiping the app away were also checked on that phone. This is **one-device field testing**, not a general reliability guarantee; unattended background GPS refresh remains to be independently observed.
- Only Android 12 or newer is supported. The APK contains an ARM64 WavPack library; no x86 or 32-bit library is included. Android's own decoders may support other formats on different devices.
- A force-stopped app generally will not receive scheduled alarms until it is opened again. Doze, power-management policy, battery depletion, revoked permissions, inaccessible audio, and volume settings can also interfere.
- A scheduled daily background GPS job can be delayed by Android and cannot guarantee an exact 24-hour update.
- The solar model calculates **unrefracted geometric solar-disk-center elevation**, with approximate precession/nutation and fixed 37-second TAI–UTC; it does not apply terrain, atmospheric refraction, or Earth-orientation parameter corrections. DE440s does not make the full timing model subsecond-accurate in all circumstances.
- The event editor searches up to approximately 370 days for a crossing; “Never occurs” means none was found within that interval, not necessarily never in the mathematical sense.
- A rebuilt-from-source APK has a different signing identity unless the original signing keystore is available, and therefore will not install as an update over the signed GitHub release without an uninstall.
## Build from source using Termux
**The public releases are the easiest way to install.** The following is for developers who wish to compile the code locally. The build has been exercised on ARM64 Termux with Android SDK platform `android-35/android.jar`, OpenJDK 21, Android build tools, and Clang. Dependencies must already be available or installed separately, and the JAR path can be overridden using `ANDROID_JAR`.
The source deliberately follows the creator's shared-storage **file-type directory scheme**: `Documents/java`, `Documents/c`, `Documents/xml`, `Documents/sh`, `Documents/md`, and `Documents/txt`. The compilation workspace is Termux-private `$HOME/.solar_elevation_alarm_build` and is not redistributed.
```bash
pkg install git aapt2 openjdk-21 d8 apksigner zip clang coreutils unzip
cd "$HOME"
git clone https://github.com/ov3qPI/Solar-Elevation-Alarm.git
cd Solar-Elevation-Alarm
for type in java c xml sh md txt; do mkdir -p "/storage/emulated/0/Documents/$type"; cp -f "Documents/$type/"* "/storage/emulated/0/Documents/$type/"; done
bash /storage/emulated/0/Documents/sh/SolarElevationAlarm_build_v20261022T151029Z.sh --preflight
bash /storage/emulated/0/Documents/sh/SolarElevationAlarm_build_v20261022T151029Z.sh --install
```
The builder fetches WavPack 5.8.1 at pinned revision `4827b98`, compiles JNI for ARM64, and produces a signed APK in `Documents/apk`, logs in `Documents/log`, and a checksum in `Documents/txt`. It creates a local signing keystore in `$HOME/.solar_elevation_alarm_build`; **keep that keystore and its password private and backed up**. Never commit keystores, signing secrets, copied diagnostics, user audio libraries, or private GPS traces.
## Licenses
Application-specific source and repository documentation are dedicated to the public domain under [CC0-1.0 Universal](LICENSE). Upstream [WavPack 5.8.1](https://github.com/dbry/WavPack) is **BSD-3-Clause**, copyright David Bryant and contributors, not CC0; its upstream license is included in the APK at `assets/wavpack_license.txt`. JPL DE440s is downloaded from [NASA NAIF](https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp), not redistributed with the APK. See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
