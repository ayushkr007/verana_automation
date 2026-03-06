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

KEYCHAIN_SERVICE="$(get_prop "keplr.password.keychain.service")"
if [[ -z "$KEYCHAIN_SERVICE" ]]; then
  KEYCHAIN_SERVICE="verana_keplr_password"
fi

if ! command -v security >/dev/null 2>&1; then
  echo "macOS 'security' command is not available."
  exit 1
fi

read -r -s -p "Enter Keplr password to store in Keychain: " KEPLR_PASSWORD
echo
if [[ -z "${KEPLR_PASSWORD}" ]]; then
  echo "Password was empty. Nothing saved."
  exit 1
fi

security add-generic-password \
  -a "$USER" \
  -s "$KEYCHAIN_SERVICE" \
  -w "$KEPLR_PASSWORD" \
  -U >/dev/null

unset KEPLR_PASSWORD
echo "Saved Keplr password in macOS Keychain service: $KEYCHAIN_SERVICE"
