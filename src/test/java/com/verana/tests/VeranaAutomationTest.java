package com.verana.tests;

import com.verana.pages.ManageDIDsPage;
import com.verana.pages.WalletModalPage;
import com.verana.utils.DIDGenerator;
import com.verana.utils.DriverManager;
import com.verana.utils.WaitUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple Verana DID flow after user signs in to Keplr manually.
 */
public class VeranaAutomationTest {

    private WebDriver driver;
    private ManageDIDsPage manageDIDsPage;
    private WalletModalPage walletModalPage;

    private static final String DID_PAGE_URL = DriverManager.getConfig()
            .getProperty("did.page.url", "https://app.testnet.verana.network/did");
    private static final int TX_SUCCESS_WAIT_SECONDS = Integer.parseInt(
            DriverManager.getConfig().getProperty("tx.success.wait.seconds", "90"));
    private static final boolean CLOSE_BROWSER_ON_FINISH = Boolean.parseBoolean(
            DriverManager.getConfig().getProperty("close.browser.on.finish", "false"));

    private String generatedDID;

    @BeforeMethod
    public void setUp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Verana Automation - Starting Test");
        System.out.println("=".repeat(60));

        driver = DriverManager.getDriver();
        manageDIDsPage = new ManageDIDsPage(driver);
        walletModalPage = new WalletModalPage(driver);

        generatedDID = DIDGenerator.generateSimpleDID();
        Assert.assertTrue(DIDGenerator.matchesAddDidRule(generatedDID),
                "Generated DID does not match Verana add_did.go DID regex.");
        System.out.println("  DID for this run: " + generatedDID);
        System.out.println("=".repeat(60) + "\n");
    }

    @Test(description = "Simple flow: open DID page, fill form, add DID, screenshot, exit")
    public void testConnectWalletAndAddDID() {
        System.out.println("[Test] STEP 1: Opening Manage DIDs page after sign-up/sign-in...");
        openDidPage();
        WaitUtils.sleep(50);
        System.out.println("[Test] ✅ STEP 1 PASSED: Current URL: " + driver.getCurrentUrl());

        System.out.println("[Test] STEP 2: Clicking 'Add DID'...");
        manageDIDsPage.clickAddDID();
        System.out.println("[Test] ✅ STEP 2 PASSED: Add DID form/modal opened.");

        System.out.println("[Test] STEP 3: Entering DID identifier: " + generatedDID);
        manageDIDsPage.enterDIDIdentifier(generatedDID);
        System.out.println("[Test] ✅ STEP 3 PASSED: DID identifier entered.");

        System.out.println("[Test] STEP 4: Setting Registration/Extension Period (years) = 1...");
        manageDIDsPage.selectOneYearRegistrationPeriod();
        System.out.println("[Test] ✅ STEP 4 PASSED: Registration period set.");

        System.out.println("[Test] STEP 5: Clicking 'Add DID' on popup...");
        manageDIDsPage.submitDIDForm();
        boolean approvalHandled = walletModalPage.tryApproveTransactionRequestQuick();
        boolean success = manageDIDsPage.waitForTransactionSuccess(Math.max(2, TX_SUCCESS_WAIT_SECONDS / 2));

        if (!success) {
            if (!approvalHandled) {
                System.out.println("[Test] Keplr approval not detected in quick check. Retrying final Add DID click once...");
                manageDIDsPage.clickFinalAddDIDIfPresent();
            }

            // Reliable fallback path when fast path misses delayed wallet prompt.
            try {
                walletModalPage.approveTransactionRequest();
            } catch (RuntimeException e) {
                System.out.println(
                        "[Test] Keplr auto-approve not detected in fallback. Waiting for transaction success in case approval popup is external.");
            }
            success = manageDIDsPage.waitForTransactionSuccess(TX_SUCCESS_WAIT_SECONDS);
        }

        Assert.assertTrue(success, "DID transaction did not show successful confirmation.");
        System.out.println("[Test] ✅ STEP 5 PASSED: Add DID submitted and transaction confirmed.");

        System.out.println("[Test] STEP 6: Taking screenshot and exiting...");
        Path screenshotPath = captureScreenshot("did-added");
        Assert.assertTrue(Files.exists(screenshotPath), "Screenshot file was not created.");
        System.out.println("[Test] ✅ STEP 6 PASSED: Screenshot saved at " + screenshotPath);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  ✅ TEST PASSED!");
        System.out.println("  DID submitted with identifier: " + generatedDID);
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
        WaitUtils.sleep(50);

        if (!manageDIDsPage.isAddDIDButtonVisible(12)) {
            throw new AssertionError(
                    "Add DID button is not visible on " + DID_PAGE_URL
                            + ". Complete sign-up/sign-in and wallet connection first, then rerun.");
        }
    }

    private Path captureScreenshot(String prefix) {
        try {
            Path screenshotDir = Paths.get("target", "screenshots");
            Files.createDirectories(screenshotDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path destination = screenshotDir.resolve(prefix + "-" + timestamp + ".png");

            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), destination);
            return destination.toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save screenshot: " + e.getMessage(), e);
        }
    }
}
