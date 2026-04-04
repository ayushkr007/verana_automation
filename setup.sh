#!/bin/bash
# ============================================================
# Verana DID Automation — One-Time Setup
# ============================================================
# This script does everything needed before running the tests:
#   1. Asks for your Keplr wallet password
#   2. Saves it to your shell profile
#   3. Launches Chrome so you can set up your Keplr wallet
#
# After this script, just run: mvn test
# ============================================================

echo ""
echo "============================================================"
echo "  Verana DID Automation — Setup"
echo "============================================================"
echo ""

# ---------- Step 0: Copy config if needed ----------
if [ ! -f "config.properties" ] && [ -f "config.properties.example" ]; then
    cp config.properties.example config.properties
    echo "  Created config.properties from config.properties.example"
    echo ""
fi

# ---------- Step 1: Keplr password ----------
CURRENT_PASSWORD="${KEPLR_PASSWORD:-}"
if [ -n "$CURRENT_PASSWORD" ]; then
    echo "  KEPLR_PASSWORD is already set in your environment."
    read -p "  Do you want to change it? (y/N): " CHANGE_PW
    if [ "$CHANGE_PW" != "y" ] && [ "$CHANGE_PW" != "Y" ]; then
        echo "  Keeping existing password."
        echo ""
    else
        CURRENT_PASSWORD=""
    fi
fi

if [ -z "$CURRENT_PASSWORD" ]; then
    echo "  Enter your Keplr wallet password."
    echo "  (This is the password you use to unlock Keplr in Chrome)"
    echo ""
    while true; do
        read -s -p "  Keplr password: " KEPLR_PASSWORD
        echo ""
        if [ -z "$KEPLR_PASSWORD" ]; then
            echo "  Password cannot be empty. Try again."
        else
            break
        fi
    done

    # Detect shell profile
    SHELL_NAME="$(basename "$SHELL")"
    if [ "$SHELL_NAME" = "zsh" ]; then
        PROFILE="$HOME/.zshrc"
    else
        PROFILE="$HOME/.bashrc"
    fi

    # Remove old KEPLR_PASSWORD export if present, then add new one
    if grep -q "export KEPLR_PASSWORD=" "$PROFILE" 2>/dev/null; then
        # Use a temp file to avoid sed -i portability issues
        grep -v "export KEPLR_PASSWORD=" "$PROFILE" > "$PROFILE.tmp"
        mv "$PROFILE.tmp" "$PROFILE"
    fi
    echo "export KEPLR_PASSWORD='$KEPLR_PASSWORD'" >> "$"

    # Also export for this session
    export KEPLR_PASSWORD

    echo ""
    echo "  Password saved to $PROFILE"
    echo "  Run: source $PROFILE (or open a new terminal)"
    echo ""
fi

# ---------- Step 2: Keplr wallet setup ----------
USER_DATA_DIR="$HOME/selenium-keplr-profile"
PROFILE_DIR="Profile 1"
KEPLR_EXT_DIR="$USER_DATA_DIR/$PROFILE_DIR/Extensions/dmkamcknogkgcdfhhbddcghachkejeap"

if [ -d "$KEPLR_EXT_DIR" ]; then
    echo "  Keplr wallet profile already exists."
    read -p "  Do you want to set up Keplr again? (y/N): " SETUP_AGAIN
    if [ "$SETUP_AGAIN" != "y" ] && [ "$SETUP_AGAIN" != "Y" ]; then
        echo ""
        echo "============================================================"
        echo "  Setup complete! Run:"
        echo "    mvn test"
        echo "============================================================"
        echo ""
        exit 0
    fi
fi

# Auto-detect Chrome binary
OS="$(uname -s)"
case "$OS" in
  Darwin)
    CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    ;;
  Linux)
    CHROME="$(command -v google-chrome || command -v google-chrome-stable || command -v chromium-browser || echo "google-chrome")"
    ;;
  *)
    echo "  Unsupported OS: $OS"
    echo "  Please set up Keplr manually — see README.md"
    exit 1
    ;;
esac

if [ ! -f "$CHROME" ] && [ "$OS" = "Darwin" ]; then
    echo ""
    echo "  ERROR: Google Chrome not found."
    echo "  Please install Chrome from: https://www.google.com/chrome/"
    exit 1
fi

# Auto-detect Keplr extension if already installed
if [ -d "$KEPLR_EXT_DIR" ]; then
    KEPLR_VERSION=$(ls "$KEPLR_EXT_DIR" | sort -V | tail -1)
    EXT_FLAG="--load-extension=$KEPLR_EXT_DIR/$KEPLR_VERSION"
    echo "  Loading existing Keplr extension."
else
    EXT_FLAG=""
fi

echo ""
echo "============================================================"
echo "  Chrome is opening now."
echo ""
echo "  What to do:"
echo "    1. Install the Keplr extension (if not already installed)"
echo "    2. Import or create your Keplr wallet"
echo "    3. Set a password (use the same one you entered above)"
echo "    4. Close Chrome when you're done"
echo "============================================================"
echo ""

# Launch Chrome
"$CHROME" \
  --user-data-dir="$USER_DATA_DIR" \
  --profile-directory="$PROFILE_DIR" \
  $EXT_FLAG \
  --no-first-run \
  --no-default-browser-check \
  --disable-default-apps \
  "https://app.testnet.verana.network/dashboard" 2>/dev/null

# Chrome has closed
echo ""
echo "============================================================"
echo "  Setup complete! Run:"
echo "    mvn test"
echo ""
echo "  Test reports will be saved to:"
echo "    ./target/surefire-reports/emailable-report.html"
echo "============================================================"
echo ""
