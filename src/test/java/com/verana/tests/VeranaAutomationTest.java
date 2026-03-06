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
 * Simple Verana DID flow after user signs in to Keplr manually.
 */
public class VeranaAutomationTest {

    private WebDriver driver;
    private DashboardPage dashboardPage;
    private ManageDIDsPage manageDIDsPage;
    private WalletModalPage walletModalPage;

    private static final String DASHBOARD_URL = DriverManager.getConfig()
            .getProperty("dashboard.url", "https://app.testnet.verana.network/dashboard");
    private static final String DID_PAGE_URL = DriverManager.getConfig()
            .getProperty("did.page.url", "https://app.testnet.verana.network/did");
    private static final int TX_SUCCESS_WAIT_SECONDS = Integer.parseInt(
            DriverManager.getConfig().getProperty("tx.success.wait.seconds", "90"));
    private static final int DID_ADD_BUTTON_WAIT_SECONDS = Integer.parseInt(
            DriverManager.getConfig().getProperty("did.add.button.wait.seconds", "6"));
    private static final int KEPLR_UNLOCK_PREOPEN_SECONDS = Integer.parseInt(
            DriverManager.getConfig().getProperty("keplr.unlock.preopen.seconds", "4"));
    private static final boolean CLOSE_BROWSER_ON_FINISH = Boolean.parseBoolean(
            DriverManager.getConfig().getProperty("close.browser.on.finish", "false"));

    private String generatedDID;

    @BeforeMethod
    public void setUp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Verana Automation - Starting Test");
        System.out.println("=".repeat(60));

        driver = DriverManager.getDriver();
        dashboardPage = new DashboardPage(driver);
        manageDIDsPage = new ManageDIDsPage(driver);
        walletModalPage = new WalletModalPage(driver);

        // Step 1: Wait for Keplr extension to load, then unlock it
        WaitUtils.sleep(3000); // Give extension time to initialize
        String keplrExtId = DriverManager.getConfig()
                .getProperty("keplr.extension.id", "dmkamcknogkgcdfhhbddcghachkejeap").trim();
        String keplrPopupUrl = "chrome-extension://" + keplrExtId + "/popup.html";
        System.out.println("[setUp] STEP 1: Opening Keplr popup to unlock: " + keplrPopupUrl);
        driver.get(keplrPopupUrl);
        WaitUtils.sleep(2000);
        walletModalPage.tryAutoUnlockInOpenContexts(KEPLR_UNLOCK_PREOPEN_SECONDS);
        System.out.println("[setUp] STEP 1 DONE: Keplr unlock attempted.");

        // Step 2: Navigate to dashboard
        System.out.println("[setUp] STEP 2: Navigating to dashboard...");
        driver.get(DASHBOARD_URL);
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
                // Open Keplr in browser tab to approve connection (up to 3 actions: chain add, connect, approve)
                int connected = walletModalPage.approveInKeplrPopup(3, 20);
                System.out.println("[setUp] Keplr connection actions approved in browser tab: " + connected);
                dashboardPage.assertWalletConnected();
                System.out.println("[setUp] STEP 3 DONE: Wallet connected via Keplr.");
            } else {
                System.out.println("[setUp] STEP 3 DONE: Connect button not found — wallet likely already connected.");
            }
        } else {
            System.out.println("[setUp] STEP 3 DONE: Wallet already connected.");
        }

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
        openDidPage();
        WaitUtils.sleep(1000);
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
        WaitUtils.sleep(1500);
        System.out.println("[Test] STEP 5 DONE: Form submitted.");

        // STEP 6: Click Keplr extension icon in toolbar, then click Approve.
        //
        // Keplr v0.13.x shows "Confirm Transaction" in the browser-action popup
        // (the dropdown when you click the Keplr icon in the toolbar corner).
        // This popup is NOT a browser window — Selenium can't access it.
        // We use Robot (OS-level mouse) to click the icon, then click Approve.
        System.out.println("[Test] STEP 6: Clicking Keplr icon → Approve transaction...");
        String didPageHandle = driver.getWindowHandle();
        boolean approvalHandled = walletModalPage.approveKeplrTransaction(didPageHandle, 30);
        System.out.println("[Test] STEP 6 DONE: Keplr approval " + (approvalHandled ? "clicked." : "not detected."));

        // STEP 7: Wait for transaction to confirm on-chain
        System.out.println("[Test] STEP 7: Waiting for transaction success confirmation...");
        boolean success = manageDIDsPage.waitForTransactionSuccess(TX_SUCCESS_WAIT_SECONDS);
        if (!success && !approvalHandled) {
            manageDIDsPage.clickFinalAddDIDIfPresent();
            success = manageDIDsPage.waitForTransactionSuccess(TX_SUCCESS_WAIT_SECONDS);
        }
        Assert.assertTrue(success, "DID transaction did not show successful confirmation.");
        System.out.println("[Test] STEP 7 DONE: Transaction confirmed.");

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  TEST PASSED!  DID: " + generatedDID);
        System.out.println("=".repeat(60) + "\n");
    }

    @AfterMethod
    public void tearDown() {
        if (CLOSE_BROWSER_ON_FINISH) {
            System.out.println("[Test] Tearing down — closing browser.");
            DriverManager.quitDriver();
        } else {
            System.out.println("[Test] Tearing down — keeping browser open (close.browser.on.finish=false).");
        }
    }

    private void openDidPage() {
        driver.get(DID_PAGE_URL);
        if (!manageDIDsPage.isAddDIDButtonVisible(DID_ADD_BUTTON_WAIT_SECONDS)) {
            throw new AssertionError(
                    "Add DID button is not visible on " + DID_PAGE_URL
                            + ". Complete sign-up/sign-in and wallet connection first, then rerun.");
        }
    }
}
