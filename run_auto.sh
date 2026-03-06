#!/usr/bin/env bash
set -euo pipefail

# =============================================================
# Verana Automation - One-Command Runner
#
# Usage:  ./run_auto.sh
#
# Selenium launches Chrome with Keplr loaded directly.
# No manual Chrome setup needed.
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

KEPLR_PASSWORD_ENV_VAR="$(get_prop "keplr.password.env.var")"
KEPLR_PASSWORD_KEYCHAIN_SERVICE="$(get_prop "keplr.password.keychain.service")"
AUTO_UNLOCK_MODE="$(get_prop "keplr.auto.unlock.enabled")"

[[ -z "$KEPLR_PASSWORD_ENV_VAR" ]] && KEPLR_PASSWORD_ENV_VAR="KEPLR_PASSWORD"
[[ -z "$KEPLR_PASSWORD_KEYCHAIN_SERVICE" ]] && KEPLR_PASSWORD_KEYCHAIN_SERVICE="verana_keplr_password"

echo "============================================================"
echo "  Verana Automation - One-Command Runner"
echo "============================================================"
echo

# ── Step 1: Load Keplr password ──
if [[ "${AUTO_UNLOCK_MODE}" == "true" ]]; then
  current_password="${!KEPLR_PASSWORD_ENV_VAR:-}"
  if [[ -z "${current_password}" ]] && command -v security >/dev/null 2>&1; then
    keychain_password="$(security find-generic-password -a "$USER" -s "$KEPLR_PASSWORD_KEYCHAIN_SERVICE" -w 2>/dev/null || true)"
    if [[ -n "${keychain_password}" ]]; then
      printf -v "$KEPLR_PASSWORD_ENV_VAR" '%s' "$keychain_password"
      export "$KEPLR_PASSWORD_ENV_VAR"
      echo "[1/2] Keplr password loaded from macOS Keychain."
    fi
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
