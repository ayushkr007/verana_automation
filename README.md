# Verana DID Automation (Selenium + Java + Keplr)

This project automates the full Verana DID flow in one run:
1. Open Verana dashboard.
2. Connect Keplr wallet (if not already connected).
3. Handle Keplr popups (connect/approve/sign).
4. Open Manage DIDs.
5. Create DID with unique identifier.

The setup is built to avoid your old issue where Selenium used a temporary Chrome without Keplr.

## Why this works

Selenium starts Chrome with a **persistent profile path** from `config.properties`:
- `chrome.user.data.dir`
- `chrome.profile.directory`

Keplr is installed one time in that profile and remains available in every automation run.

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

### 3. Run the automation

```bash
cd /Users/ayushkumar2109/Desktop/verana_automation
./run_once.sh
```

Alternative:

```bash
mvn -Dtest=VeranaAutomationTest test
```

## Main files

- `config.properties`: runtime config
- `launch_chrome.sh`: opens Chrome with Selenium profile for one-time Keplr setup
- `run_once.sh`: executes the test
- `src/test/java/com/verana/tests/VeranaAutomationTest.java`: end-to-end flow
- `src/test/java/com/verana/pages/WalletModalPage.java`: Keplr popup approvals
- `src/test/java/com/verana/utils/DriverManager.java`: Chrome profile-based driver startup

## Flow details

During run:
1. `DashboardPage.assertKeplrInjected()` checks extension is present (`window.keplr`).
2. If wallet is disconnected, script clicks `Connect Wallet` and chooses `Keplr`.
3. If Keplr is locked, enter password in popup; script waits for unlock screen to clear and continues.
4. Keplr popup handler clicks safe positive actions only (`Unlock`, `Next`, `Approve`, `Connect`, `Confirm`, `Sign`).
5. After form fill, script takes screenshot and exits (no DID submit).

## Troubleshooting

- `user data directory is already in use`:
  Close all Chrome windows, then rerun.
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
