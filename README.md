# Verana DID Automation (Selenium + Java + Keplr)

This project automates the Verana DID flow in one run:
1. Open Manage DIDs page.
2. Click Add DID.
3. Fill DID identifier and year.
4. Submit and approve transaction in Keplr.
5. Capture screenshot.

The setup is built to avoid your old issue where Selenium used a temporary Chrome without Keplr.

## Why this works

Selenium uses a **persistent Chrome profile** from `config.properties`:
- `chrome.user.data.dir`
- `chrome.profile.directory`

Keplr is installed one time in that profile and remains available in every automation run.

For repeated runs without entering password each time, Selenium can attach to an already-open Chrome session:
- `chrome.attach.to.running=true`
- `chrome.debugger.address=127.0.0.1:9222`
- `keplr.auto.unlock.enabled=true`

## Prerequisites

- Java 11+
- Maven 3.6+
- Google Chrome
- Keplr wallet + VNA tokens

Quick checks:

```bash
java -version
mvn -version
```

## From scratch setup

### 1. Configure your profile path

Open `config.properties` and confirm these:

```properties
chrome.user.data.dir=/Users/ayushkumar2109/selenium-keplr-profile
chrome.profile.directory=Default
chrome.binary.path=/Applications/Google Chrome.app/Contents/MacOS/Google Chrome
```

Use a dedicated folder for Selenium. Do not use your everyday Chrome profile.

### 2. One-time Keplr setup in Selenium profile

```bash
cd /Users/ayushkumar2109/Desktop/verana_automation
./launch_chrome.sh
```

Inside that Chrome window:
1. Install Keplr extension.
2. Import/unlock wallet.
3. Ensure wallet has VNA on the Verana network you need.
4. Open Verana app once and complete first connect approval.
5. Close Chrome fully.

### 3. No-password repeated runs (recommended)

Start reusable Chrome once:

```bash
cd /Users/ayushkumar2109/Desktop/verana_automation
./start_keplr_session.sh
```

Store Keplr password once in macOS Keychain:

```bash
cd /Users/ayushkumar2109/Desktop/verana_automation
./save_keplr_password.sh
```

In that Chrome:
1. Open Keplr and unlock once.
2. Keep Chrome open.
3. Optional: in Keplr settings, set auto-lock timeout to a high value.

Now run automation any number of times (it attaches to same Chrome session):

```bash
cd /Users/ayushkumar2109/Desktop/verana_automation
./run_once.sh
```

### 4. Single-run mode (fresh Chrome each run)

If you want the old behavior, set:

```properties
chrome.attach.to.running=false
```

Then run:

```bash
cd /Users/ayushkumar2109/Desktop/verana_automation
./run_once.sh
```

## Main files

- `config.properties`: runtime config
- `launch_chrome.sh`: opens Chrome with Selenium profile for one-time Keplr setup
- `run_once.sh`: executes the test
- `start_keplr_session.sh`: starts reusable Chrome session for no-password repeated runs
- `save_keplr_password.sh`: saves Keplr password in macOS Keychain (one-time)
- `src/test/java/com/verana/tests/VeranaAutomationTest.java`: end-to-end flow
- `src/test/java/com/verana/pages/WalletModalPage.java`: Keplr popup approvals
- `src/test/java/com/verana/utils/DriverManager.java`: Chrome profile-based driver startup

## Flow details

During run:
1. Opens `https://app.testnet.verana.network/did` after your sign-in flow.
2. Clicks `Add DID`, fills DID identifier and registration period `1`.
3. Submits Add DID and attempts Keplr approval actions (`Approve`, `Confirm`, `Sign`, etc.).
4. If Keplr is locked, script auto-fills password from env/Keychain and clicks Unlock.
5. Waits for transaction success confirmation.
6. Captures screenshot at `target/screenshots`.

## Troubleshooting

- `Unable to attach to running Chrome at 127.0.0.1:9222`:
  Start `./start_keplr_session.sh` first and keep that Chrome open.
- Keplr still asks password:
  Set `keplr.auto.unlock.enabled=true`, save password once via `./save_keplr_password.sh`, then rerun.
- Verana page feels slow to open:
  Keep `page.load.strategy=eager` and tune `did.add.button.wait.seconds` in `config.properties`.
- `user data directory is already in use`:
  You started another Chrome with the same profile. Keep one session only.
- Keplr popup not found:
  Re-open `./launch_chrome.sh`, verify Keplr exists in that exact profile path.
- `Keplr was not injected`:
  Wrong profile path or Keplr not installed in that profile.
- Stops after password entry:
  Increase `keplr.unlock.wait.seconds` in `config.properties` and ensure you click `Unlock` in popup.
- DID form selectors fail:
  Verana UI changed; update locators in `ManageDIDsPage.java`.

## Security note

Never hardcode seed phrase/private key in test code or config.
