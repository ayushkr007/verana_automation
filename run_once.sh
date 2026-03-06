#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$ROOT_DIR/config.properties"
cd "$ROOT_DIR"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing config file: $CONFIG_FILE"
  exit 1
fi

get_prop() {
  local key="$1"
  sed -n "s/^${key}=//p" "$CONFIG_FILE" | head -n 1
}

ATTACH_MODE="$(get_prop "chrome.attach.to.running")"
DEBUGGER_ADDRESS="$(get_prop "chrome.debugger.address")"
AUTO_UNLOCK_MODE="$(get_prop "keplr.auto.unlock.enabled")"
KEPLR_PASSWORD_ENV_VAR="$(get_prop "keplr.password.env.var")"
KEPLR_PASSWORD_KEYCHAIN_SERVICE="$(get_prop "keplr.password.keychain.service")"

if [[ -z "${KEPLR_PASSWORD_ENV_VAR}" ]]; then
  KEPLR_PASSWORD_ENV_VAR="KEPLR_PASSWORD"
fi
if [[ -z "${KEPLR_PASSWORD_KEYCHAIN_SERVICE}" ]]; then
  KEPLR_PASSWORD_KEYCHAIN_SERVICE="verana_keplr_password"
fi

if [[ "${AUTO_UNLOCK_MODE}" == "true" ]]; then
  current_password="${!KEPLR_PASSWORD_ENV_VAR:-}"
  if [[ -z "${current_password}" ]] && command -v security >/dev/null 2>&1; then
    keychain_password="$(security find-generic-password -a "$USER" -s "$KEPLR_PASSWORD_KEYCHAIN_SERVICE" -w 2>/dev/null || true)"
    if [[ -n "${keychain_password}" ]]; then
      printf -v "$KEPLR_PASSWORD_ENV_VAR" '%s' "$keychain_password"
      export "$KEPLR_PASSWORD_ENV_VAR"
      echo "Keplr auto-unlock password loaded from macOS Keychain service: ${KEPLR_PASSWORD_KEYCHAIN_SERVICE}"
    fi
  fi
fi

echo "Running Verana DID automation test..."
if [[ "${ATTACH_MODE}" == "true" ]]; then
  echo "Attach mode is ON. Keep Chrome running at ${DEBUGGER_ADDRESS} (start once via ./start_keplr_session.sh)."
else
  echo "Attach mode is OFF. Ensure Chrome is fully closed before this run (profile lock prevention)."
fi
if [[ "${AUTO_UNLOCK_MODE}" == "true" ]]; then
  if [[ -n "${!KEPLR_PASSWORD_ENV_VAR:-}" ]]; then
    echo "Keplr auto-unlock is ON (password source: env ${KEPLR_PASSWORD_ENV_VAR})."
  else
    echo "Keplr auto-unlock is ON but password is not set (env ${KEPLR_PASSWORD_ENV_VAR} / Keychain)."
  fi
fi
echo

mvn -Dtest=VeranaAutomationTest test
