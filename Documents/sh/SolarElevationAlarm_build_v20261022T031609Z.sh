#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
SCRIPT_ID='SolarElevationAlarm_build_v20261022T031609Z'
BASE='/storage/emulated/0'
SOURCE="$BASE/Documents/java"
MANIFEST="$BASE/Documents/xml/SolarAlarm_AndroidManifest.xml"
BRIDGE="$BASE/Documents/c/SolarAlarmWavPackBridge_v20261022T015350Z.c"
WORK="$HOME/.solar_elevation_alarm_build"
VENDOR="$WORK/vendor/WavPack-5.8.1"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
APK="$BASE/Documents/apk/SolarElevationAlarm_v${STAMP}.apk"
LOG="$BASE/Documents/log/SolarElevationAlarm_build_v${STAMP}.log"
KEY="$WORK/SolarElevationAlarm_signing.keystore"
SECRET="$WORK/SolarElevationAlarm_signing.pass"
fail(){ printf 'FAILED: %s\n' "$*" >&2;exit 1; }
if [[ "${1:-}" == '--help' ]];then printf 'Usage: bash %s [--preflight|--install]\n' "$BASE/Documents/sh/${SCRIPT_ID}.sh";exit 0;fi
for cmd in aapt2 javac d8 apksigner keytool zip od tr sha256sum clang git tee;do command -v "$cmd" >/dev/null 2>&1 || fail "Missing executable: $cmd (pkg install aapt2 openjdk-21 d8 apksigner zip coreutils clang git)";done
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
JAR="${ANDROID_JAR:-}"
if [[ -z "$JAR" ]];then JAR="$(find "$SDK/platforms" -maxdepth 2 -type f -name android.jar 2>/dev/null | sort -V | tail -n 1 || true)";fi
[[ -f "$JAR" ]] || fail "No Android SDK platform android.jar. Set ANDROID_JAR or install $SDK/platforms/android-35"
for name in SolarAlarmAudio SolarAlarmEphemeris SolarAlarmMainActivity SolarAlarmPlaybackService SolarAlarmReceiver SolarAlarmSchedule SolarAlarmSolar SolarAlarmStore SolarAlarmSystemReceiver SolarAlarmWavPack SolarAlarmWvPlayback SolarAlarmMaintenanceJob SolarAlarmLocation;do [[ -f "$SOURCE/$name.java" ]] || fail "Missing $SOURCE/$name.java";done
[[ -f "$MANIFEST" && -f "$BRIDGE" ]] || fail "Missing $MANIFEST or $BRIDGE"
grep -q "android.permission.ACCESS_BACKGROUND_LOCATION" "$MANIFEST" || fail "Manifest must declare background location"
[[ -f "$BASE/Documents/xml/SolarAlarm_styles.xml" && -f "$BASE/Documents/xml/SolarAlarm_launcher.xml" ]] || fail 'Missing styles or launcher resources'
JAVA_BIN="$(readlink -f "$(command -v javac)")"
JDK="${JAVA_HOME:-$(dirname "$(dirname "$JAVA_BIN")")}" 
[[ -f "$JDK/include/jni.h" ]] || fail "JNI header not found: $JDK/include/jni.h"
printf '%s\n' "Script: $SCRIPT_ID" "Device SDK: $(getprop ro.build.version.sdk 2>/dev/null || true)" "Java platform: $JAR" "JDK: $JDK" "Work: $WORK" "Output: $APK" "Native vendor: WavPack 5.8.1 (pinned commit 4827b98)"
if [[ "${1:-}" == '--preflight' ]];then exit 0;fi
mkdir -p "$WORK/src/org/solarelevation/alarm" "$WORK/res/values" "$WORK/res/drawable" "$WORK/vendor" "$BASE/Documents/apk" "$BASE/Documents/log" "$BASE/Documents/txt"
exec > >(tee "$LOG") 2>&1
trap 'printf "Build failed at shell line %s. See %s\n" "$LINENO" "$LOG" >&2' ERR
if [[ ! -f "$VENDOR/include/wavpack.h" ]];then rm -rf "$VENDOR";printf '%s\n' 'Downloading official WavPack 5.8.1 source';git clone --depth 1 --branch 5.8.1 https://github.com/dbry/WavPack.git "$VENDOR";fi
[[ -d "$VENDOR/.git" ]] || fail "WavPack source is not a Git checkout: $VENDOR"
VENDOR_REV="$(git -C "$VENDOR" rev-parse --short=7 HEAD)"
[[ "$VENDOR_REV" == 4827b98 ]] || fail "WavPack source revision mismatch: expected 4827b98, got $VENDOR_REV"
[[ -f "$VENDOR/license.txt" || -f "$VENDOR/LICENSE" ]] || fail 'Missing upstream WavPack BSD license'
rm -rf "$WORK/classes" "$WORK/dex" "$WORK/compiled" "$WORK/lib"
mkdir -p "$WORK/classes" "$WORK/dex" "$WORK/compiled" "$WORK/lib/arm64-v8a"
cp -f "$SOURCE"/SolarAlarm*.java "$WORK/src/org/solarelevation/alarm/"
cp -f "$MANIFEST" "$WORK/AndroidManifest.xml"
cp -f "$BASE/Documents/xml/SolarAlarm_styles.xml" "$WORK/res/values/styles.xml"
cp -f "$BASE/Documents/xml/SolarAlarm_launcher.xml" "$WORK/res/drawable/alarm_launcher.xml"
printf '%s\n' 'Compiling pinned WavPack 5.8.1 and JNI bridge for ARM64 Android'
NATIVE_SRCS=(common_utils decorr_utils entropy_utils extra1 extra2 open_utils open_filename open_legacy open_raw pack pack_dns pack_floats pack_utils read_words tags tag_utils unpack unpack_floats unpack_seek unpack_utils write_words)
NATIVE_FILES=()
for item in "${NATIVE_SRCS[@]}";do [[ -f "$VENDOR/src/$item.c" ]] || fail "Missing vendor source: $item.c";NATIVE_FILES+=("$VENDOR/src/$item.c");done
clang --target=aarch64-linux-android31 -std=c11 -D_GNU_SOURCE -D_FILE_OFFSET_BITS=64 -DHAVE___BUILTIN_CLZ=1 -O2 -fPIC -fvisibility=hidden -ffunction-sections -fdata-sections -shared -Wl,--gc-sections -Wl,--no-undefined -I"$VENDOR/include" -I"$VENDOR/src" -I"$JDK/include" -I"$JDK/include/linux" "$BRIDGE" "${NATIVE_FILES[@]}" -lm -o "$WORK/lib/arm64-v8a/libsolarwavpack.so"
[[ -s "$WORK/lib/arm64-v8a/libsolarwavpack.so" ]] || fail 'Native shared library is missing'
if command -v llvm-readelf >/dev/null 2>&1;then llvm-readelf -d "$WORK/lib/arm64-v8a/libsolarwavpack.so" | grep NEEDED || true;fi
printf '%s\n' 'Compiling Java sources'
javac --release 8 -encoding UTF-8 -Xlint:-options -classpath "$JAR" -d "$WORK/classes" "$WORK/src/org/solarelevation/alarm/"*.java
printf '%s\n' 'Converting Java classes to Android DEX'
mapfile -d '' CLASS_FILES < <(find "$WORK/classes" -name '*.class' -type f -print0)
d8 --min-api 31 --lib "$JAR" --output "$WORK/dex" "${CLASS_FILES[@]}"
[[ -s "$WORK/dex/classes.dex" ]] || fail 'D8 produced no classes.dex'
printf '%s\n' 'Compiling resources and manifest'
aapt2 compile --dir "$WORK/res" -o "$WORK/compiled"
aapt2 link -I "$JAR" --manifest "$WORK/AndroidManifest.xml" --min-sdk-version 31 --target-sdk-version 35 -o "$WORK/base.apk" "$WORK/compiled/"*.flat
cp "$WORK/dex/classes.dex" "$WORK/classes.dex"
mkdir -p "$WORK/assets"
if [[ -f "$VENDOR/license.txt" ]];then cp "$VENDOR/license.txt" "$WORK/assets/wavpack_license.txt";else cp "$VENDOR/LICENSE" "$WORK/assets/wavpack_license.txt";fi
(cd "$WORK" && zip -q -u base.apk classes.dex lib/arm64-v8a/libsolarwavpack.so assets/wavpack_license.txt)
unzip -l "$WORK/base.apk" | grep -F 'lib/arm64-v8a/libsolarwavpack.so' || fail 'APK is missing native library'
INPUT="$WORK/base.apk"
if command -v zipalign >/dev/null 2>&1;then zipalign -f -p 4 "$INPUT" "$WORK/aligned.apk";INPUT="$WORK/aligned.apk";fi
if [[ ! -f "$KEY" ]];then umask 077;od -An -N24 -tx1 /dev/urandom | tr -d '[:space:]' > "$SECRET";keytool -genkeypair -noprompt -storetype PKCS12 -keystore "$KEY" -alias solarelevation -keyalg RSA -keysize 3072 -validity 10000 -storepass "$(cat "$SECRET")" -keypass "$(cat "$SECRET")" -dname 'CN=Solar Elevation Alarm, O=Personal Open Source, C=US';fi
[[ -f "$SECRET" ]] || fail "Signing secret missing: $SECRET; do not regenerate your existing keystore"
printf '%s\n' 'Signing standalone APK'
apksigner sign --ks "$KEY" --ks-key-alias solarelevation --ks-pass "file:$SECRET" --out "$APK" "$INPUT"
apksigner verify --verbose "$APK"
sha256sum "$APK" > "$BASE/Documents/txt/SolarElevationAlarm_apk_sha256_v${STAMP}.txt"
printf 'SUCCESS: %s\n' "$APK"
printf 'SHA256: %s\n' "$(sha256sum "$APK")"
if [[ "${1:-}" == '--install' ]];then termux-open "$APK";fi
