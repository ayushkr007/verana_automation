package com.verana.tests;

import com.verana.pages.DashboardPage;
import com.verana.pages.ManageDIDsPage;
import com.verana.pages.WalletModalPage;
import com.verana.utils.DIDGenerator;
import com.verana.utils.DriverManager;
import com.verana.utils.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


/**
 * Verana DID registration automation.
 *
 * Flow:
 * 1. Go to dashboard, sign in with Keplr
 * 2. Navigate to DID page (/did)
 * 3. Click "Add DID"
 * 4. Fill DID identifier (unique each run), set 1 year period
 * 5. Submit, approve Keplr transaction, wait for confirmation
 */
public class VeranaAutomationTest {

    private WebDriver driver;
    private DashboardPage dashboardPage;
    private ManageDIDsPage manageDIDsPage;
    private WalletModalPage walletModalPage;

    private String dashboardUrl;
    private String didPageUrl;
    private int keplrUnlockPreopenSeconds;
    private boolean closeBrowserOnFinish;
    private int txSuccessWaitSeconds;
    private int didAddButtonWaitSeconds;

    private String generatedDID;

    @BeforeMethod
    public void setUp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Verana DID Automation - Starting Test");
        System.out.println("=".repeat(60));

        // Load config lazily to provide clear errors on missing config
        java.util.Properties cfg = DriverManager.getConfig();
        dashboardUrl = cfg.getProperty("dashboard.url", "https://app.testnet.verana.network/dashboard");
        didPageUrl = cfg.getProperty("did.page.url", "https://app.testnet.verana.network/did");
        keplrUnlockPreopenSeconds = Integer.parseInt(cfg.getProperty("keplr.unlock.preopen.seconds", "4"));
        closeBrowserOnFinish = Boolean.parseBoolean(cfg.getProperty("close.browser.on.finish", "false"));
        txSuccessWaitSeconds = Integer.parseInt(cfg.getProperty("tx.success.wait.seconds", "60"));
        didAddButtonWaitSeconds = Integer.parseInt(cfg.getProperty("did.add.button.wait.seconds", "6"));

        driver = DriverManager.getDriver();
        dashboardPage = new DashboardPage(driver);
        manageDIDsPage = new ManageDIDsPage(driver);
        walletModalPage = new WalletModalPage(driver);

        // Step 1: Wait for Keplr extension to load, then unlock it
        WaitUtils.sleep(3000);
        String keplrExtId = cfg.getProperty("keplr.extension.id", "dmkamcknogkgcdfhhbddcghachkejeap").trim();
        String keplrPopupUrl = "chrome-extension://" + keplrExtId + "/popup.html";
        System.out.println("[setUp] STEP 1: Opening Keplr popup to unlock: " + keplrPopupUrl);
        driver.get(keplrPopupUrl);
        // Wait for Keplr popup to fully render (extension pages can be slow)
        WaitUtils.sleep(3000);

        // Try to unlock — use a generous timeout since Keplr may need time to load
        int unlockTimeout = Math.max(keplrUnlockPreopenSeconds, 8);
        boolean unlocked = walletModalPage.tryAutoUnlockInOpenContexts(unlockTimeout);
        if (!unlocked) {
            System.out.println("[setUp] WARNING: Keplr auto-unlock may have failed. Trying once more...");
            WaitUtils.sleep(2000);
            unlocked = walletModalPage.tryAutoUnlockInOpenContexts(unlockTimeout);
        }
        System.out.println("[setUp] STEP 1 DONE: Keplr unlock " + (unlocked ? "succeeded." : "may have failed — check logs."));

        // Step 2: Navigate to dashboard
        System.out.println("[setUp] STEP 2: Navigating to dashboard...");
        driver.get(dashboardUrl);
        WaitUtils.sleep(2000);
        System.out.println("[setUp] STEP 2 DONE: On dashboard.");

        // Step 3: Connect wallet if not already connected
        System.out.println("[setUp] STEP 3: Checking wallet connection...");
        if (!dashboardPage.isWalletConnected()) {
            System.out.println("[setUp] Wallet not connected. Attempting to click Connect Wallet...");
            boolean clicked = dashboardPage.clickConnectWallet();
            if (clicked) {
                walletModalPage.selectKeplr();
                WaitUtils.sleep(2000);
                // Approve connection (up to 3 actions: chain add, connect, approve)
                int connected = walletModalPage.approveInKeplrPopup(3, 20);
                System.out.println("[setUp] Keplr connection actions approved: " + connected);
                dashboardPage.assertWalletConnected();
                System.out.println("[setUp] STEP 3 DONE: Wallet connected via Keplr.");
            } else {
                System.out.println("[setUp] STEP 3 DONE: Connect button not found — wallet likely already connected.");
            }
        } else {
            System.out.println("[setUp] STEP 3 DONE: Wallet already connected.");
        }

        // Generate unique DID for this run
        generatedDID = DIDGenerator.generateSimpleDID();
        Assert.assertTrue(DIDGenerator.matchesAddDidRule(generatedDID),
                "Generated DID does not match Verana add_did.go DID regex.");
        System.out.println("  DID for this run: " + generatedDID);
        System.out.println("=".repeat(60) + "\n");
    }

    @Test(description = "DID page: Add DID with unique identifier, approve Keplr, confirm transaction")
    public void testConnectWalletAndAddDID() {

        // STEP 1: Open DID page
        System.out.println("[Test] STEP 1: Opening DID page...");
        driver.get(didPageUrl);
        if (!manageDIDsPage.isAddDIDButtonVisible(didAddButtonWaitSeconds)) {
            throw new AssertionError(
                    "Add DID button is not visible on " + didPageUrl
                            + ". Complete sign-up/sign-in and wallet connection first, then rerun.");
        }
        System.out.println("[Test] STEP 1 DONE: " + driver.getCurrentUrl());

        // STEP 2: Click "Add DID" to open the form
        System.out.println("[Test] STEP 2: Clicking 'Add DID' button...");
        manageDIDsPage.clickAddDID();
        WaitUtils.sleep(1500);
        System.out.println("[Test] STEP 2 DONE: Add DID form opened.");

        // STEP 3: Enter unique DID identifier (UUID-based — different on every run)
        System.out.println("[Test] STEP 3: Entering DID identifier: " + generatedDID);
        manageDIDsPage.enterDIDIdentifier(generatedDID);
        WaitUtils.sleep(1000);
        System.out.println("[Test] STEP 3 DONE: DID identifier entered.");

        // STEP 4: Set registration/extension period = 1 year
        System.out.println("[Test] STEP 4: Setting year to 1...");
        manageDIDsPage.selectOneYearRegistrationPeriod();
        WaitUtils.sleep(1000);
        System.out.println("[Test] STEP 4 DONE: Year set to 1.");

        // STEP 5: Submit the form (clicks the inner "Add DID" confirm button)
        System.out.println("[Test] STEP 5: Submitting Add DID form...");
        manageDIDsPage.submitDIDForm();
        WaitUtils.sleep(1000);
        System.out.println("[Test] STEP 5 DONE: Form submitted.");

        // STEP 6: Approve Keplr transaction
        System.out.println("[Test] STEP 6: Waiting for Keplr popup and clicking Approve...");
        WaitUtils.sleep(3000); // Give Keplr time to register the transaction
        boolean approved = walletModalPage.approveKeplrTransaction(15);
        Assert.assertTrue(approved, "Keplr transaction approval failed — popup may not have appeared or Approve button was not found.");
        System.out.println("[Test] STEP 6 DONE: Keplr approval succeeded.");

        // STEP 7: Wait for transaction success
        System.out.println("[Test] STEP 7: Waiting for transaction success...");
        boolean success = manageDIDsPage.waitForTransactionSuccess(txSuccessWaitSeconds);
        Assert.assertTrue(success, "Transaction success message was not detected within " + txSuccessWaitSeconds + " seconds.");
        System.out.println("[Test] STEP 7 DONE: Transaction confirmed.");

        // STEP 8: Navigate back to DID page
        System.out.println("[Test] STEP 8: Navigating back to DID page...");
        driver.get(didPageUrl);
        WaitUtils.sleep(2000);
        System.out.println("[Test] STEP 8 DONE: On DID page: " + driver.getCurrentUrl());

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  TEST PASSED!");
        System.out.println("  DID: " + generatedDID);
        System.out.println("=".repeat(60) + "\n");
    }

    @AfterMethod
    public void tearDown() {
        if (closeBrowserOnFinish) {
            System.out.println("[Test] Tearing down — closing browser.");
            DriverManager.quitDriver();
        } else {
            System.out.println("[Test] Tearing down — keeping browser open (close.browser.on.finish=false).");
        }
    }
}
