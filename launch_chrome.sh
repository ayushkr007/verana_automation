#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$ROOT_DIR/config.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing config file: $CONFIG_FILE"
  exit 1
fi

get_prop() {
  local key="$1"
  sed -n "s/^${key}=//p" "$CONFIG_FILE" | head -n 1
}

USER_DATA_DIR="$(get_prop "chrome.user.data.dir")"
PROFILE_DIR="$(get_prop "chrome.profile.directory")"
CHROME_BIN="$(get_prop "chrome.binary.path")"

if [[ -z "$USER_DATA_DIR" ]]; then
  echo "chrome.user.data.dir is empty in config.properties"
  exit 1
fi
if [[ -z "$PROFILE_DIR" ]]; then
  PROFILE_DIR="Default"
fi
if [[ -z "$CHROME_BIN" ]]; then
  CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
fi

mkdir -p "$USER_DATA_DIR"

cat <<EOF
Launching Chrome with your Selenium profile:
  user-data-dir: $USER_DATA_DIR
  profile      : $PROFILE_DIR

Use this for one-time setup:
1) Install Keplr extension.
2) Import/unlock your wallet.
3) Verify VNA balance on Verana network.
4) Close Chrome before running mvn test.
EOF

"$CHROME_BIN" \
  --user-data-dir="$USER_DATA_DIR" \
  --profile-directory="$PROFILE_DIR" \
  --no-first-run \
  --no-default-browser-check
