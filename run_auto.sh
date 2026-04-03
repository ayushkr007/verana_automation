#!/usr/bin/env bash
set -euo pipefail

# =============================================================
# Verana Automation - One-Command Runner
#
# Usage:  ./run_auto.sh
#
# Selenium launches Chrome with Keplr loaded directly.
# No manual Chrome setup needed (after initial ./launch_chrome.sh).
# =============================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$ROOT_DIR/config.properties"
cd "$ROOT_DIR"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "ERROR: Missing config file: $CONFIG_FILE"
  exit 1
fi

get_prop() {
  local key="$1"
  sed -n "s/^${key}=//p" "$CONFIG_FILE" | head -n 1
}

# Resolve ~ to home directory
resolve_path() {
  local p="$1"
  echo "${p/#\~/$HOME}"
}

KEPLR_PASSWORD_ENV_VAR="$(get_prop "keplr.password.env.var")"
KEPLR_PASSWORD_KEYCHAIN_SERVICE="$(get_prop "keplr.password.keychain.service")"
AUTO_UNLOCK_MODE="$(get_prop "keplr.auto.unlock.enabled")"

[[ -z "$KEPLR_PASSWORD_ENV_VAR" ]] && KEPLR_PASSWORD_ENV_VAR="KEPLR_PASSWORD"
[[ -z "$KEPLR_PASSWORD_KEYCHAIN_SERVICE" ]] && KEPLR_PASSWORD_KEYCHAIN_SERVICE="verana_keplr_password"

echo "============================================================"
echo "  Verana Automation - One-Command Runner"
echo "============================================================"
echo

# ── Pre-flight check: does the profile exist? ──
USER_DATA_DIR="$(resolve_path "$(get_prop "chrome.user.data.dir")")"
PROFILE_DIR="$(get_prop "chrome.profile.directory")"
[[ -z "$PROFILE_DIR" ]] && PROFILE_DIR="Default"

if [[ -n "$USER_DATA_DIR" && ! -d "$USER_DATA_DIR/$PROFILE_DIR" ]]; then
  echo "WARNING: Chrome profile not found at: $USER_DATA_DIR/$PROFILE_DIR"
  echo "Run ./launch_chrome.sh first to create the profile and install Keplr."
  echo
fi

# ── Step 1: Load Keplr password ──
if [[ "${AUTO_UNLOCK_MODE}" == "true" ]]; then
  current_password="${!KEPLR_PASSWORD_ENV_VAR:-}"

  if [[ -z "${current_password}" ]] && command -v security >/dev/null 2>&1; then
    # macOS Keychain
    keychain_password="$(security find-generic-password -a "$USER" -s "$KEPLR_PASSWORD_KEYCHAIN_SERVICE" -w 2>/dev/null || true)"
    if [[ -n "${keychain_password}" ]]; then
      printf -v "$KEPLR_PASSWORD_ENV_VAR" '%s' "$keychain_password"
      export "$KEPLR_PASSWORD_ENV_VAR"
      echo "[1/2] Keplr password loaded from macOS Keychain."
    fi
  fi

  # Final check
  final_password="${!KEPLR_PASSWORD_ENV_VAR:-}"
  if [[ -z "${final_password}" ]]; then
    echo "WARNING: No Keplr password found. Auto-unlock will fail."
    echo "Set it with:  export KEPLR_PASSWORD=YourPasswordHere"
    echo "Or on macOS:  ./save_keplr_password.sh"
    echo
  fi
fi

# ── Step 2: Run the test (Selenium launches Chrome automatically) ──
echo "[2/2] Running Verana DID automation test..."
echo "      Selenium will launch Chrome with Keplr extension loaded."
echo

mvn -Dtest=VeranaAutomationTest test

echo
echo "============================================================"
echo "  Automation complete."
echo "============================================================"
