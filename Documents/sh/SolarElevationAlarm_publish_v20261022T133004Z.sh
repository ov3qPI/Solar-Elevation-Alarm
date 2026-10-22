#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
SCRIPT_ID='SolarElevationAlarm_publish_v20261022T133004Z'
ROOT='/storage/emulated/0'
ZIP_NAME='SolarElevationAlarm_GitHub_v20261022T133004Z.zip'
OWNER='ov3qpi'
REPO='Solar-Elevation-Alarm'
WORK="$HOME/.solar_elevation_alarm_publish_v20261022T133004Z"
fail(){ printf 'FAILED: %s\n' "$*" >&2; exit 1; }
for command in git gh unzip; do command -v "$command" >/dev/null 2>&1 || fail "Missing $command. Run: pkg install git gh unzip"; done
ARCHIVE="$(find "$ROOT/Documents/zip" "$ROOT/Download" -maxdepth 1 -type f -name "$ZIP_NAME" -print -quit 2>/dev/null || true)"
[[ -f "$ARCHIVE" ]] || fail "Download $ZIP_NAME into Documents/zip or Download"
MODE="${1:---publish}"
[[ "$MODE" == '--publish' || "$MODE" == '--prepare' ]] || fail "Usage: bash $SCRIPT_ID.sh [--prepare|--publish]"
if [[ "$MODE" == '--publish' ]]; then
  LOGIN="$(gh api user --jq .login 2>/dev/null || true)"
  [[ "${LOGIN,,}" == "${OWNER,,}" ]] || fail "GitHub CLI authenticated as '${LOGIN:-none}', not $OWNER. Use 'gh auth login --hostname github.com --git-protocol https --web' and 'gh auth switch --user $OWNER' before publishing."
  if gh repo view "$OWNER/$REPO" --json name >/dev/null 2>&1; then fail "Repository $OWNER/$REPO already exists. Refusing to overwrite it."; fi
fi
[[ "$WORK" == "$HOME/.solar_elevation_alarm_publish_v20261022T133004Z" ]] || fail 'Unsafe work path'
rm -rf "$WORK"
mkdir -p "$WORK"
unzip -q "$ARCHIVE" -d "$WORK"
[[ -f "$WORK/LICENSE" && -f "$WORK/README.md" && -f "$WORK/Documents/sh/SolarElevationAlarm_build_v20261022T031609Z.sh" ]] || fail 'Repository archive is incomplete'
git -C "$WORK" init -q -b main
git -C "$WORK" add --all
git -C "$WORK" -c user.name="${GIT_AUTHOR_NAME:-ov3qpi}" -c user.email="${GIT_AUTHOR_EMAIL:-ov3qpi@users.noreply.github.com}" commit -qm 'Initial public source: Solar Elevation Alarm v20261022T031609Z'
printf 'Local repository prepared: %s\n' "$WORK"
printf 'Tracked files: %s\n' "$(git -C "$WORK" ls-files | wc -l)"
if [[ "$MODE" == '--prepare' ]]; then exit 0; fi
gh auth setup-git
cd "$WORK"
gh repo create "$OWNER/$REPO" --public --description 'Termux-built Android solar-elevation alarms with long-form WavPack audio, CC0' --source . --remote origin --push
printf 'PUBLISHED: https://github.com/%s/%s\n' "$OWNER" "$REPO"
