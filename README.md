# Verana DID Automation

Selenium Java automation for registering DIDs on the Verana testnet.

## What it does

1. Launches Chrome with the Keplr wallet extension
2. Auto-unlocks Keplr with saved password
3. Navigates to the Verana dashboard, connects the wallet
4. Opens the DID page (`/did`)
5. Clicks **Add DID** and fills the form:
   - **DID Identifier** — unique `did:verana:<random>` generated each run
   - **Registration Period** — 1 year
6. Submits the form, approves the Keplr transaction
7. Waits for on-chain transaction confirmation

---

## Quick Start

### Prerequisites

- **Java 11+**
- **Maven 3.6+**
- **Google Chrome**

### Installing prerequisites

#### macOS

```bash
brew install openjdk@11 maven
echo 'export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Ubuntu / Debian

```bash
sudo apt update
sudo apt install openjdk-11-jdk maven google-chrome-stable
```

#### Windows

1. Download and install [JDK 11](https://adoptium.net/)
2. Download and install [Maven](https://maven.apache.org/download.cgi) and add it to your `PATH`
3. Download and install [Google Chrome](https://www.google.com/chrome/)

---

### Step 1: Clone the project

```bash
git clone https://github.com/ayushkr007/verana_automation.git
cd verana_automation
```

### Step 2: Run the setup script (one-time)

```bash
chmod +x setup.sh
./setup.sh
```

The script will:
1. Ask for your Keplr wallet password (hidden input — no one can see it)
2. Save the password to your shell profile (`~/.zshrc` or `~/.bashrc`)
3. Open Chrome so you can install Keplr and import/create your wallet
4. Tell you the next step when done

### Step 3: Run the tests

```bash
mvn test
```

### Step 4: View the test report

After the tests finish, open the HTML report:

```bash
open target/surefire-reports/emailable-report.html    # macOS
xdg-open target/surefire-reports/emailable-report.html  # Linux
```

Or just open `target/surefire-reports/emailable-report.html` in any browser.

---

## Manual Setup (without setup.sh)

```bash
cp config.properties.example config.properties
chmod +x launch_chrome.sh
./launch_chrome.sh    # set up Keplr wallet, then close Chrome
```

Set your Keplr password:

```bash
echo 'export KEPLR_PASSWORD="YourKeplrPassword"' >> ~/.zshrc
source ~/.zshrc
```

Run:

```bash
mvn test
```

---

## Project Structure

```
verana_automation/
├── config.properties.example    # Config template (copy to config.properties)
├── setup.sh                     # One-time setup script
├── launch_chrome.sh             # Opens Chrome for Keplr wallet setup
├── pom.xml                      # Maven project config
├── src/test/java/com/verana/
│   ├── tests/
│   │   └── VeranaAutomationTest.java   # Main test flow
│   ├── pages/
│   │   ├── DashboardPage.java          # Dashboard page object
│   │   ├── ManageDIDsPage.java         # DID page object (form fill + submit)
│   │   └── WalletModalPage.java        # Keplr wallet interactions
│   └── utils/
│       ├── DriverManager.java          # Chrome + Keplr extension setup
│       ├── DIDGenerator.java           # Unique DID generation
│       └── WaitUtils.java              # Selenium wait helpers
└── src/test/resources/
    └── testng.xml                      # TestNG suite config
```

---

## Troubleshooting

### "config.properties not found"

Run the setup script: `./setup.sh`

Or manually:
```bash
cp config.properties.example config.properties
```

### "Failed to launch Chrome" / "user-data-dir is already in use"

Close **all** Chrome windows:

```bash
# macOS
pkill -f "Google Chrome"

# Linux
pkill chrome

# Windows (PowerShell)
Stop-Process -Name chrome -Force
```

### Keplr is locked / password not working

Make sure `KEPLR_PASSWORD` is set:
```bash
echo $KEPLR_PASSWORD
```

If empty, set it:
```bash
export KEPLR_PASSWORD="YourKeplrPassword"
```

### Test reports not generated

Reports are saved to `./target/surefire-reports/` after the test run. If the directory is empty, the tests may not have reached the execution phase — check the terminal output for errors.

### Keplr extension not found

Run `./launch_chrome.sh`, install Keplr from the Chrome Web Store, set up your wallet, then close Chrome and retry.
