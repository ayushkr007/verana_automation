package com.verana.pages;

import com.verana.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;

/**
 * ManageDIDsPage
 *
 * Represents the Manage DIDs page (/dids).
 * Handles clicking "Add DID", filling the identifier, selecting the
 * registration period (1 year), and submitting the form.
 */
public class ManageDIDsPage {

    private final WebDriver driver;
    private final WaitUtils wait;
    private WebElement lastDidInput;

    // ---- Locators ----

    // "Add DID" button on the DIDs list page
    private final By addDIDButton = By.xpath(
            "//button[contains(., 'Add DID')] | " +
                    "//button[contains(., 'Register DID')] | " +
                    "//button[contains(., 'New DID')] | " +
                    "//button[contains(., 'Create DID')]");

    // DID identifier field in the Add DID form (strict to avoid search input)
    private final By didIdentifierInputStrict = By.xpath(
            "//label[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'did identifier')]/following::input[1]");

    // Add DID form containers to scope field/button lookup
    private final By addDidFormContainer = By.xpath(
            "//div[@role='dialog' or contains(@class,'modal') or contains(@class,'drawer')][.//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'did identifier')]] | "
                    +
                    "//form[.//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'did identifier')]]");

    // Registration/Extension Period field (years) inside modal
    private final By registrationPeriodInput = By.xpath(
            "//div[@role='dialog']//label[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'registration/extension period')]/following::input[1] | "
                    +
                    "//div[@role='dialog']//input[@name[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'period') or "
                    +
                    "contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'year') or "
                    +
                    "contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'duration')]] | "
                    +
                    "//div[@role='dialog']//input[@type='number'][1] | " +
                    "//label[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'registration/extension period')]/following::input[1] | "
                    +
                    "//input[@name[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'period') or " +
                    "contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'year') or " +
                    "contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'duration')]] | " +
                    "(//input[@type='number'])[1]");

    // Custom dropdown items (for non-native <select> dropdowns)
    private final By dropdownItems = By.xpath(
            "//ul[@role='listbox']//li | //div[@role='option'] | " +
                    "//div[contains(@class,'option')][contains(., 'year') or contains(., 'Year')]");

    // Submit / Confirm / Register button inside the form
    private final By submitButton = By.xpath(
            "//div[@role='dialog']//*[self::button or @role='button'][" +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'add did') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'submit') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'confirm') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'register') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'save')" +
                    "] | //div[@role='dialog']//button[@type='submit'] | " +
                    "//form//*[self::button or @role='button'][contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'add did') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'submit') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'confirm') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'register') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'save')] | " +
                    "//button[@type='submit']");

    // Success toast / confirmation message after DID is added
    private final By successMessage = By.xpath(
            "//div[contains(@class,'toast') or contains(@class,'success') or " +
                    "contains(@class,'notification') or contains(@class,'alert')]" +
                    "[contains(., 'success') or contains(., 'Success') or " +
                    " contains(., 'created') or contains(., 'registered') or contains(., 'added')]");

    // Transaction success indicators shown after wallet approval
    private final By transactionSuccessMessage = By.xpath(
            "//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'transaction successful') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'successful transaction') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'tx hash') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'did added') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'did created')]");

    public ManageDIDsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
    }

    /**
     * Clicks the "Add DID" button to open the form/modal.
     */
    public void clickAddDID() {
        System.out.println("[ManageDIDsPage] Looking for 'Add DID' button...");
        WebElement btn = wait.waitForClickable(addDIDButton, 8);
        scrollIntoView(btn);
        btn.click();
        System.out.println("[ManageDIDsPage] Clicked 'Add DID' button.");
        WaitUtils.sleep(40);
    }

    public boolean isAddDIDButtonVisible(int seconds) {
        return wait.isVisible(addDIDButton, seconds);
    }

    /**
     * Enters the DID identifier into the input field.
     * Tries the full DID string first; if validation errors then tries only the
     * suffix.
     */
    public void enterDIDIdentifier(String didIdentifier) {
        System.out.println("[ManageDIDsPage] Entering DID identifier: " + didIdentifier);
        WebElement input = findDidIdentifierInput();
        lastDidInput = input;
        setInputValueFast(input, didIdentifier);
        System.out.println("[ManageDIDsPage] DID identifier entered: " + didIdentifier);
    }

    /**
     * Selects the registration period for 1 year.
     * Handles both native <select> dropdowns and custom UI dropdowns.
     */
    public void selectOneYearRegistrationPeriod() {
        System.out.println("[ManageDIDsPage] Setting registration period to 1 year...");
        int implicitWaitSeconds = Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("implicit.wait.seconds", "2"));
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0));
        try {
            if (isRegistrationPeriodAlreadyOne())
                return;

            // Strategy -1: direct one-year selector buttons/chips if present.
            if (tryOneYearQuickSelect())
                return;

            // Strategy 0: Direct registration/extension period input inside modal
            if (tryRegistrationPeriodInput())
                return;

            // Strategy 1: Native <select> element
            if (tryNativeSelect())
                return;

            // Strategy 2: Number input field
            if (tryNumberInput())
                return;

            // Strategy 3: Custom dropdown (click to open, then pick option)
            if (tryCustomDropdown())
                return;

            System.out.println("[ManageDIDsPage] ⚠️  Could not find registration period field. " +
                    "The field may already default to 1 year, or have a different structure on the live page.");
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWaitSeconds));
        }
    }

    private boolean isRegistrationPeriodAlreadyOne() {
        try {
            List<WebElement> candidates = driver.findElements(registrationPeriodInput);
            for (WebElement input : candidates) {
                if (!input.isDisplayed()) {
                    continue;
                }
                String value = normalize(input.getAttribute("value"));
                if ("1".equals(value) || value.startsWith("1 ")) {
                    System.out.println("[ManageDIDsPage] Registration period already set to 1.");
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean tryOneYearQuickSelect() {
        try {
            WebElement root = resolveScopedRoot();
            List<WebElement> candidates = (root != null)
                    ? root.findElements(By.xpath(
                            ".//*[self::button or @role='button' or self::div][normalize-space()='1' or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '1 year')]"))
                    : driver.findElements(By.xpath(
                            "//*[self::button or @role='button' or self::div][normalize-space()='1' or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '1 year')]"));

            for (WebElement candidate : candidates) {
                if (!candidate.isDisplayed()) {
                    continue;
                }
                scrollIntoView(candidate);
                click(candidate);
                System.out.println("[ManageDIDsPage] Selected 1-year option via quick selector: " + candidate.getText());
                return true;
            }
        } catch (Exception e) {
            System.out.println("[ManageDIDsPage] 1-year quick selector not found.");
        }
        return false;
    }

    private boolean tryRegistrationPeriodInput() {
        try {
            List<WebElement> candidates = driver.findElements(registrationPeriodInput);
            for (WebElement input : candidates) {
                if (!input.isDisplayed()) {
                    continue;
                }
                scrollIntoView(input);
                input.clear();
                input.sendKeys("1");
                System.out.println("[ManageDIDsPage] Entered '1' in Registration/Extension Period field.");
                return true;
            }
        } catch (Exception e) {
            System.out.println("[ManageDIDsPage] Registration/Extension Period input strategy failed: " + e.getMessage());
        }
        return false;
    }

    private boolean tryNativeSelect() {
        try {
            List<WebElement> selects = driver.findElements(By.xpath("//div[@role='dialog']//select"));
            if (!selects.isEmpty()) {
                Select sel = new Select(selects.get(0));

                // Try to select by visible text containing "1 year"
                List<WebElement> opts = sel.getOptions();
                for (WebElement opt : opts) {
                    String text = opt.getText().toLowerCase();
                    if (text.contains("1") && text.contains("year")) {
                        sel.selectByVisibleText(opt.getText());
                        System.out.println("[ManageDIDsPage] Selected via native <select>: " + opt.getText());
                        return true;
                    }
                }

                // Fallback: select value "1" or first option that contains "year"
                try {
                    sel.selectByValue("1");
                    System.out.println("[ManageDIDsPage] Selected via native <select> value='1'.");
                    return true;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("[ManageDIDsPage] No native <select> found.");
        }
        return false;
    }

    private boolean tryNumberInput() {
        try {
            List<WebElement> inputs = driver.findElements(By.xpath("//div[@role='dialog']//input[@type='number']"));
            if (!inputs.isEmpty()) {
                WebElement input = inputs.get(0);
                scrollIntoView(input);
                input.clear();
                input.sendKeys("1");
                System.out.println("[ManageDIDsPage] Entered '1' in numeric input field for period.");
                return true;
            }
        } catch (Exception e) {
            System.out.println("[ManageDIDsPage] No numeric input field found.");
        }
        return false;
    }

    private boolean tryCustomDropdown() {
        try {
            // Common pattern: click a button/div that opens a dropdown list
            List<WebElement> triggerCandidates = driver.findElements(By.xpath(
                    "//div[@role='dialog']//button[contains(., 'year') or contains(., 'Year') or " +
                            "contains(., 'period') or contains(., 'Period')] | " +
                            "//div[@role='dialog']//div[@role='combobox']"));
            for (WebElement trigger : triggerCandidates) {
                if (!trigger.isDisplayed()) {
                    continue;
                }
                scrollIntoView(trigger);
                click(trigger);
                WaitUtils.sleep(120);

                // Now look for "1 year" option in the opened dropdown
                List<WebElement> items = driver.findElements(dropdownItems);
                for (WebElement item : items) {
                    String text = item.getText().toLowerCase();
                    if (text.contains("1") && text.contains("year")) {
                        click(item);
                        System.out.println("[ManageDIDsPage] Selected '1 year' from custom dropdown: " + item.getText());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[ManageDIDsPage] Custom dropdown approach failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Clicks the submit/confirm/register button to submit the DID form.
     */
    public void submitDIDForm() {
        System.out.println("[ManageDIDsPage] Submitting the DID form...");
        WebElement scopedRoot = resolveScopedRoot();

        // Fast path: try direct final Add DID immediately.
        if (scopedRoot != null && clickPrimaryAddDidInScope(scopedRoot)) {
            WaitUtils.sleep(60);
            System.out.println("[ManageDIDsPage] Submit clicked from fast path.");
            return;
        }

        // Strict manual-like flow in same form: Next (if present) -> final Add DID.
        if (scopedRoot != null && clickFinalAddDidSequence(scopedRoot)) {
            System.out.println("[ManageDIDsPage] Submit action clicked from strict scoped sequence.");
            return;
        }

        // Some UIs are step-based (Next -> Add DID). Handle that first in scope.
        if (scopedRoot != null) {
            boolean clickedNext = clickActionInScope(scopedRoot, "next");
            if (clickedNext) {
                WaitUtils.sleep(90);
            }

            if (clickPrimaryAddDidInScope(scopedRoot)
                    || clickActionInScope(scopedRoot, "submit")
                    || clickActionInScope(scopedRoot, "confirm")
                    || clickActionInScope(scopedRoot, "register")
                    || clickActionInScope(scopedRoot, "save")
                    || clickActionInScope(scopedRoot, "continue")) {
                System.out.println("[ManageDIDsPage] Submit action clicked from scoped form.");
                return;
            }
        }

        try {
            WebElement submit = wait.waitForVisible(submitButton, 6);
            scrollIntoView(submit);
            click(submit);
            System.out.println("[ManageDIDsPage] Submit button clicked.");
            return;
        } catch (Exception primaryFailure) {
            System.out.println("[ManageDIDsPage] Primary submit locator failed, trying scoped fallback...");
        }

        List<WebElement> candidates = (scopedRoot != null)
                ? scopedRoot.findElements(By.xpath(".//button | .//*[@role='button']"))
                : driver.findElements(By.xpath("//button | //*[@role='button']"));

        for (WebElement candidate : candidates) {
            if (candidate.isDisplayed()) {
                System.out.println("[ManageDIDsPage] Visible action candidate: " + candidate.getText().trim());
            }
        }

        // If wizard-style controls exist, move to final step first.
        WebElement next = findBottomMostAction(candidates, "next");
        if (next != null) {
            click(next);
            System.out.println("[ManageDIDsPage] Clicked fallback action: Next");
            WaitUtils.sleep(120);
            candidates = (scopedRoot != null)
                    ? scopedRoot.findElements(By.xpath(".//button | .//*[@role='button']"))
                    : driver.findElements(By.xpath("//button | //*[@role='button']"));
        }

        WebElement best = firstNonNull(
                (scopedRoot != null ? findBestAddDidAction(scopedRoot) : null),
                findBottomMostAction(candidates, "add did"),
                findBottomMostAction(candidates, "submit"),
                findBottomMostAction(candidates, "confirm"),
                findBottomMostAction(candidates, "register"),
                findBottomMostAction(candidates, "save"),
                findBottomMostAction(candidates, "continue"),
                findBottomMostAction(candidates, "add"));

        if (best == null) {
            WebElement disabledAddDid = findBottomMostVisibleAction(candidates, "add did");
            if (disabledAddDid != null && !disabledAddDid.isEnabled()) {
                System.out.println("[ManageDIDsPage] 'Add DID' is visible but disabled. Retrying year selection...");
                selectOneYearRegistrationPeriod();
                WaitUtils.sleep(80);

                candidates = (scopedRoot != null)
                        ? scopedRoot.findElements(By.xpath(".//button | .//*[@role='button']"))
                        : driver.findElements(By.xpath("//button | //*[@role='button']"));

                best = firstNonNull(
                        (scopedRoot != null ? findBestAddDidAction(scopedRoot) : null),
                        findBottomMostAction(candidates, "add did"),
                        findBottomMostAction(candidates, "submit"),
                        findBottomMostAction(candidates, "confirm"),
                        findBottomMostAction(candidates, "register"),
                        findBottomMostAction(candidates, "save"),
                        findBottomMostAction(candidates, "continue"),
                        findBottomMostAction(candidates, "add"));
            }
        }

        if (best == null) {
            throw new RuntimeException(
                    "Could not find enabled submit action in DID form. Check year field selection and visible candidates log.");
        }

        scrollIntoView(best);
        click(best);
        System.out.println("[ManageDIDsPage] Submit button clicked via fallback strategy: '" + safeLabel(best) + "'");
    }

    public boolean clickFinalAddDIDIfPresent() {
        WebElement scopedRoot = resolveScopedRoot();
        if (scopedRoot == null) {
            return false;
        }
        return clickPrimaryAddDidInScope(scopedRoot);
    }

    /**
     * Waits for a success message/toast after form submission.
     */
    public boolean waitForSuccessConfirmation() {
        System.out.println("[ManageDIDsPage] Waiting for success confirmation...");
        try {
            int toastWaitSeconds = Integer.parseInt(
                    com.verana.utils.DriverManager.getConfig().getProperty("tx.success.toast.wait.seconds", "4"));
            wait.waitForVisible(successMessage, toastWaitSeconds);
            System.out.println("[ManageDIDsPage] ✅ Success message visible: " +
                    driver.findElement(successMessage).getText());
            return true;
        } catch (Exception e) {
            // Some apps don't show a toast but redirect. In that case check URL/page
            // content.
            System.out.println("[ManageDIDsPage] No toast found, checking if DID appears in the list...");
            WaitUtils.sleep(100);
            return false;
        }
    }

    private WebElement findBottomMostAction(List<WebElement> candidates, String containsLower) {
        WebElement best = null;
        int bestY = Integer.MIN_VALUE;
        for (WebElement candidate : candidates) {
            if (!candidate.isDisplayed() || !candidate.isEnabled()) {
                continue;
            }
            String label = candidate.getText();
            if (label == null) {
                continue;
            }
            String lower = label.trim().toLowerCase();
            if (lower.isEmpty()) {
                continue;
            }
            if (!lower.contains(containsLower)) {
                continue;
            }
            if (lower.contains("cancel") || lower.contains("close") || lower.contains("copy")) {
                continue;
            }
            int y = candidate.getRect().getY();
            if (y >= bestY) {
                bestY = y;
                best = candidate;
            }
        }
        return best;
    }

    private WebElement findBottomMostVisibleAction(List<WebElement> candidates, String containsLower) {
        WebElement best = null;
        int bestY = Integer.MIN_VALUE;
        for (WebElement candidate : candidates) {
            if (!candidate.isDisplayed()) {
                continue;
            }
            String label = candidate.getText();
            if (label == null) {
                continue;
            }
            String lower = label.trim().toLowerCase();
            if (lower.isEmpty()) {
                continue;
            }
            if (!lower.contains(containsLower)) {
                continue;
            }
            if (lower.contains("cancel") || lower.contains("close") || lower.contains("copy")) {
                continue;
            }
            int y = candidate.getRect().getY();
            if (y >= bestY) {
                bestY = y;
                best = candidate;
            }
        }
        return best;
    }

    public boolean waitForTransactionSuccess(int seconds) {
        System.out.println("[ManageDIDsPage] Waiting for successful transaction confirmation...");
        try {
            WebElement success = wait.waitForVisible(transactionSuccessMessage, seconds);
            System.out.println("[ManageDIDsPage] ✅ Transaction success visible: " + success.getText());
            return true;
        } catch (Exception e) {
            System.out.println("[ManageDIDsPage] Transaction success message not found within timeout.");
            return waitForSuccessConfirmation();
        }
    }

    private WebElement firstNonNull(WebElement... elements) {
        for (WebElement element : elements) {
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    /**
     * Verifies the given DID identifier appears somewhere on the current page.
     * Can be used after form submission to confirm the DID was registered.
     */
    public boolean isDIDPresentInList(String didIdentifier) {
        try {
            // Look for the DID text anywhere on the page
            By didInList = By.xpath("//*[contains(text(),'" + didIdentifier + "')]");
            wait.waitForVisible(didInList, 10);
            System.out.println("[ManageDIDsPage] ✅ DID found in the list: " + didIdentifier);
            return true;
        } catch (Exception e) {
            System.out.println("[ManageDIDsPage] ⚠️  DID not found in visible list (may still have been created): "
                    + didIdentifier);
            return false;
        }
    }

    private void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        WaitUtils.sleep(30);
    }

    private void click(WebElement element) {
        try {
            element.click();
        } catch (Exception clickError) {
            try {
                new Actions(driver)
                        .moveToElement(element)
                        .pause(Duration.ofMillis(60))
                        .click()
                        .perform();
            } catch (Exception actionsError) {
                System.out.println("[ManageDIDsPage] Falling back to JS click for action: " + safeLabel(element));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            }
        }
    }

    private WebElement resolveScopedRoot() {
        // Fast path: do not block for long waits while locating the add-DID container.
        try {
            List<WebElement> roots = driver.findElements(addDidFormContainer);
            for (WebElement root : roots) {
                if (root.isDisplayed()) {
                    return root;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            return wait.waitForVisible(addDidFormContainer, 1);
        } catch (Exception ignored) {
        }

        if (lastDidInput == null) {
            return null;
        }
        try {
            // Build scope from the typed DID field to avoid selecting side-panel buttons.
            return lastDidInput.findElement(
                    By.xpath("./ancestor::*[.//*[self::button or @role='button'][contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'add did') or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'next')]][1]"));
        } catch (Exception ignored) {
        }

        try {
            return lastDidInput.findElement(
                    By.xpath("./ancestor::*[self::form or @role='dialog' or contains(@class,'modal')][1]"));
        } catch (Exception e) {
            return null;
        }
    }

    private WebElement findDidIdentifierInput() {
        // Fastest path: direct DID Identifier locator.
        try {
            WebElement direct = wait.waitForVisible(didIdentifierInputStrict, 1);
            scrollIntoView(direct);
            return direct;
        } catch (Exception ignored) {
        }

        WebElement scopedRoot = resolveScopedRoot();
        if (scopedRoot != null) {
            List<WebElement> scopedInputs = scopedRoot.findElements(
                    By.xpath(".//label[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'did identifier')]/following::input[1]"));
            for (WebElement input : scopedInputs) {
                if (input.isDisplayed()) {
                    scrollIntoView(input);
                    return input;
                }
            }
        }

        // strict global fallback; still tied to DID Identifier label only
        WebElement input = wait.waitForVisible(didIdentifierInputStrict, 1);
        scrollIntoView(input);
        return input;
    }

    private boolean clickActionInScope(WebElement scope, String labelContainsLower) {
        List<WebElement> actions = scope.findElements(By.xpath(".//button | .//*[@role='button']"));
        WebElement action = findBottomMostAction(actions, labelContainsLower);
        if (action == null) {
            return false;
        }
        scrollIntoView(action);
        click(action);
        System.out.println("[ManageDIDsPage] Clicked scoped action: " + safeLabel(action));
        return true;
    }

    private void setInputValueFast(WebElement input, String value) {
        try {
            input.click();
            input.sendKeys(Keys.chord(Keys.COMMAND, "a"));
            input.sendKeys(Keys.BACK_SPACE);
            input.sendKeys(value);
            WaitUtils.sleep(80);
            String current = normalize(input.getAttribute("value"));
            if (normalize(value).equals(current)) {
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            // Retry once with Action typing in case a controlled input ignored the first set.
            new Actions(driver)
                    .click(input)
                    .keyDown(Keys.COMMAND).sendKeys("a").keyUp(Keys.COMMAND)
                    .sendKeys(Keys.BACK_SPACE)
                    .sendKeys(value)
                    .perform();
            WaitUtils.sleep(80);
            String current = normalize(input.getAttribute("value"));
            if (normalize(value).equals(current)) {
                return;
            }
        } catch (Exception ignored) {
        }

        // Last fallback: JS set + events.
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].focus();" +
                            "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                    input, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set DID Identifier value.", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean clickFinalAddDidSequence(WebElement scope) {
        boolean clickedAny = false;

        // Optional wizard transition.
        if (clickActionInScope(scope, "next")) {
            WaitUtils.sleep(60);
            clickedAny = true;
        }

        // Primary submit click.
        if (clickPrimaryAddDidInScope(scope)) {
            WaitUtils.sleep(60);
            clickedAny = true;
        }

        // Some flows show a final confirmation step with another "Add DID" button.
        if (clickedAny && isElementStillVisible(scope) && hasEnabledAddDid(scope)) {
            if (clickPrimaryAddDidInScope(scope)) {
                System.out.println("[ManageDIDsPage] Clicked final confirmation 'Add DID' step.");
                WaitUtils.sleep(60);
            }
        }

        return clickedAny;
    }

    private boolean clickPrimaryAddDidInScope(WebElement scope) {
        WebElement bestAddDid = findBestAddDidAction(scope);
        if (bestAddDid == null) {
            return false;
        }
        scrollIntoView(bestAddDid);
        click(bestAddDid);
        System.out.println("[ManageDIDsPage] Clicked scoped action: " + safeLabel(bestAddDid));
        return true;
    }

    private WebElement findBestAddDidAction(WebElement scope) {
        List<WebElement> actions = scope.findElements(By.xpath(
                ".//button[not(@disabled)] | .//*[@role='button']"));

        WebElement best = null;
        int bestScore = Integer.MIN_VALUE;
        for (WebElement action : actions) {
            if (!action.isDisplayed() || !action.isEnabled()) {
                continue;
            }

            String label = safeLabel(action).toLowerCase();
            if (!label.contains("add did")) {
                continue;
            }
            if (label.contains("cancel") || label.contains("close")) {
                continue;
            }

            int score = action.getRect().getY();
            String type = action.getAttribute("type");
            if ("submit".equalsIgnoreCase(type)) {
                score += 10_000;
            }
            if ("add did".equals(label)) {
                score += 100;
            }

            if (score >= bestScore) {
                bestScore = score;
                best = action;
            }
        }
        return best;
    }

    private boolean hasEnabledAddDid(WebElement scope) {
        try {
            return findBestAddDidAction(scope) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isElementStillVisible(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private String safeLabel(WebElement element) {
        try {
            String text = element.getText();
            return text == null ? "<empty>" : text.trim();
        } catch (Exception e) {
            return "<unreadable>";
        }
    }
}
