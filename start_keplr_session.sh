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
DEBUGGER_ADDRESS="$(get_prop "chrome.debugger.address")"
DEBUG_START_WAIT_SECONDS="$(get_prop "chrome.debug.start.wait.seconds")"

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
if [[ -z "$DEBUGGER_ADDRESS" ]]; then
  DEBUGGER_ADDRESS="127.0.0.1:9222"
fi
if [[ -z "$DEBUG_START_WAIT_SECONDS" ]]; then
  DEBUG_START_WAIT_SECONDS="1"
fi

DEBUG_PORT="${DEBUGGER_ADDRESS##*:}"
if [[ -z "$DEBUG_PORT" ]]; then
  DEBUG_PORT="9222"
fi

if lsof -i "tcp:${DEBUG_PORT}" -sTCP:LISTEN -n -P >/dev/null 2>&1; then
  echo "Debug Chrome session already running at ${DEBUGGER_ADDRESS}."
  exit 0
fi

mkdir -p "$USER_DATA_DIR"

cat <<EOF
Starting Chrome session for reusable Keplr login:
  user-data-dir     : $USER_DATA_DIR
  profile           : $PROFILE_DIR
  debugger-address  : $DEBUGGER_ADDRESS

Do this once:
1) Unlock Keplr with password.
2) Keep this Chrome window open.
3) Run tests via ./run_once.sh (it will attach, not relaunch Chrome).
EOF

LOG_FILE="/tmp/verana_chrome_debug.log"

"$CHROME_BIN" \
  --remote-debugging-port="$DEBUG_PORT" \
  --user-data-dir="$USER_DATA_DIR" \
  --profile-directory="$PROFILE_DIR" \
  --no-first-run \
  --no-default-browser-check \
  "about:blank" >"$LOG_FILE" 2>&1 &

CHROME_PID=$!
echo "Chrome started in background (pid: $CHROME_PID)."

# Quick readiness check so command returns fast.
deadline=$((SECONDS + DEBUG_START_WAIT_SECONDS))
while (( SECONDS <= deadline )); do
  if lsof -i "tcp:${DEBUG_PORT}" -sTCP:LISTEN -n -P >/dev/null 2>&1; then
    echo "Debug port is ready at ${DEBUGGER_ADDRESS}."
    exit 0
  fi
  sleep 0.1
done

echo "Chrome launched, but debug port may still be starting: ${DEBUGGER_ADDRESS}"
exit 0
