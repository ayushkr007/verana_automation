package com.verana.pages;

import com.verana.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.time.Duration;

/**
 * WalletModalPage
 *
 * Handles the "Select your wallet" modal that appears after clicking "Connect
 * Wallet".
 * Selects Keplr and then handles one or more Keplr approval popups.
 */
public class WalletModalPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    // ---- Locators ----

    // Modal title to confirm the modal is open
    private final By modalTitle = By.xpath(
            "//h2[contains(text(),'Select your wallet')] | //div[contains(text(),'Select your wallet')]");

    // Keplr button inside the modal
    private final By keplrButton = By.xpath(
            "//button[@title='Keplr'] | //button[.//span[contains(text(),'Keplr')]] | " +
                    "//div[contains(@class,'wallet')][.//span[contains(text(),'Keplr')]]");

    // Positive Keplr actions we are willing to click.
    // Intentionally excludes reject/cancel.
    private final By keplrActionButton = By.xpath(
            "//button[not(@disabled) and " +
                    "not(contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'reject')) and " +
                    "not(contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'cancel')) and " +
                    "(" +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'unlock') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'next') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'approve') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'connect') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'confirm') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'sign') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'allow') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'grant')" +
                    ")]");
    // Keplr confirm transaction context for DID add flow.
    private final By keplrConfirmTxContext = By.xpath(
            "//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'confirm transaction') and " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'msgadddid')]");
    private final By keplrApproveTextButton = By.xpath(
            "//*[self::button or @role='button' or self::div][not(@disabled) and " +
                    "not(contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'reject')) and " +
                    "not(contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'cancel')) and " +
                    "(" +
                    "normalize-space()='Approve' or " +
                    ".//*[normalize-space()='Approve'] or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), ' approve')" +
                    ")]");
    // User-provided Keplr approve footer container selector.
    private final By keplrApproveContainerByClass = By.cssSelector("div.sc-gUAEMC.jxcvNv");
    private final By keplrApproveButtonInContainer = By.xpath(
            ".//*[self::button or @role='button' or self::div]" +
                    "[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'approve')]");
    private final By keplrPasswordInput = By.xpath(
            "//input[@type='password' or contains(translate(@placeholder, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'password')]");

    public WalletModalPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
    }

    /**
     * Waits for the wallet selection modal to appear, then clicks Keplr.
     */
    public void selectKeplr() {
        System.out.println("[WalletModalPage] Waiting for wallet selection modal...");
        try {
            wait.waitForVisible(modalTitle, 15);
            System.out.println("[WalletModalPage] Wallet modal is visible.");
        } catch (Exception e) {
            System.out.println("[WalletModalPage] Modal title not found, looking for Keplr button directly...");
        }

        WebElement keplr = wait.waitForClickable(keplrButton, 15);
        keplr.click();
        System.out.println("[WalletModalPage] Clicked 'Keplr' wallet option.");
    }

    /**
     * Handles required Keplr popups for initial wallet connection.
     */
    public void approveConnectionAndTransactionRequests() {
        // Initial connect flows often open 1-3 popups (chain add, connect, approve).
        handleKeplrPopups(true, 3, Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("keplr.connection.popup.wait.seconds", "20")), 2);
    }

    /**
     * Handles optional Keplr popup after submitting DID creation transaction.
     */
    public void approveTransactionRequestIfPresent() {
        handleKeplrPopups(false, 2, Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("keplr.optional.popup.wait.seconds", "2")), 2);
    }

    /**
     * Fast optional check for a transaction approval popup.
     * Returns true if at least one approval action was clicked.
     */
    public boolean tryApproveTransactionRequestQuick() {
        int quickWait = Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("keplr.transaction.quick.popup.wait.seconds", "2"));

        // First try direct Selenium contexts (same-window Keplr style or extension window).
        if (tryApproveInAnySeleniumContext(quickWait)) {
            return true;
        }

        // First try native Keplr window flow (same style as wallet sign-in popup).
        if (attemptNativeApproveFallback(quickWait)) {
            return true;
        }

        int handled = handleKeplrPopups(false, 1, quickWait,
                Integer.parseInt(
                        com.verana.utils.DriverManager.getConfig().getProperty("keplr.optional.popup.wait.seconds", "1")));
        return handled > 0;
    }

    /**
     * Handles required Keplr popup after submitting DID creation transaction.
     */
    public void approveTransactionRequest() {
        int waitSeconds = Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("keplr.transaction.popup.wait.seconds", "20"));

        // Main path: click approve from any Selenium-visible context first.
        if (tryApproveInAnySeleniumContext(waitSeconds)) {
            return;
        }

        // Main path for your flow: approve in the same Keplr-style popup window.
        if (attemptNativeApproveFallback(waitSeconds)) {
            return;
        }

        // Fallback if Selenium can directly access the approval context.
        handleKeplrPopups(true, 2, waitSeconds,
                Integer.parseInt(
                        com.verana.utils.DriverManager.getConfig().getProperty("keplr.optional.popup.wait.seconds", "1")));
    }

    private int handleKeplrPopups(boolean popupRequired, int maxPopups, int firstPopupWaitSeconds, int subsequentWaitSeconds) {
        String mainWindowHandle = driver.getWindowHandle();
        int handledPopups = 0;

        while (handledPopups < maxPopups) {
            int timeoutSeconds = (popupRequired && handledPopups == 0)
                    ? firstPopupWaitSeconds
                    : subsequentWaitSeconds;

            String approvalHandle = waitForApprovalContext(mainWindowHandle, timeoutSeconds);
            if (approvalHandle == null) {
                if (popupRequired && handledPopups == 0) {
                    if (attemptNativeApproveFallback(firstPopupWaitSeconds)) {
                        handledPopups++;
                        popupRequired = false;
                        continue;
                    }
                    throw new RuntimeException("Keplr approval did not appear for required step.");
                }
                break;
            }

            driver.switchTo().window(approvalHandle);
            boolean isMainContext = approvalHandle.equals(mainWindowHandle);
            System.out.println("[WalletModalPage] Keplr approval context found: " + approvalHandle +
                    (isMainContext ? " (main window)" : ""));

            boolean clickedAction = approveCurrentPopup(approvalHandle, !isMainContext);
            if (!clickedAction) {
                throw new RuntimeException(
                        "Keplr popup opened but no actionable button was found (Approve/Connect/Sign/Confirm).");
            }

            if (!isMainContext) {
                wait.waitUntilWindowClosed(approvalHandle, 3);
                if (driver.getWindowHandles().contains(mainWindowHandle)) {
                    driver.switchTo().window(mainWindowHandle);
                } else {
                    Set<String> remaining = driver.getWindowHandles();
                    if (!remaining.isEmpty()) {
                        mainWindowHandle = remaining.iterator().next();
                        driver.switchTo().window(mainWindowHandle);
                    }
                }
            }

            handledPopups++;
            popupRequired = false;
            WaitUtils.sleep(70);
        }

        if (driver.getWindowHandles().contains(mainWindowHandle)) {
            driver.switchTo().window(mainWindowHandle);
        }
        if (handledPopups == 0) {
            System.out.println("[WalletModalPage] No Keplr popup appeared for this step.");
        } else {
            System.out.println("[WalletModalPage] Completed Keplr popup approvals. Total popups handled: " + handledPopups);
        }
        return handledPopups;
    }


    private String waitForApprovalContext(String mainWindowHandle, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        String originalHandle = driver.getWindowHandle();

        while (System.currentTimeMillis() < deadline) {
            String context = findApprovalContext(mainWindowHandle);
            if (context != null) {
                if (driver.getWindowHandles().contains(originalHandle)) {
                    driver.switchTo().window(originalHandle);
                }
                return context;
            }
            WaitUtils.sleep(80);
        }

        if (driver.getWindowHandles().contains(originalHandle)) {
            driver.switchTo().window(originalHandle);
        }
        return null;
    }

    private String findApprovalContext(String mainWindowHandle) {
        Set<String> handles = driver.getWindowHandles();
        if (handles.isEmpty()) {
            return null;
        }

        List<String> orderedHandles = new ArrayList<>();
        for (String handle : handles) {
            if (!handle.equals(mainWindowHandle)) {
                orderedHandles.add(handle);
            }
        }
        if (handles.contains(mainWindowHandle)) {
            orderedHandles.add(mainWindowHandle);
        }

        for (String handle : orderedHandles) {
            try {
                driver.switchTo().window(handle);
                if (isWalletLocked() || isLikelyKeplrContext() || hasApproveIndicatorsInCurrentContext()
                        || findClickableActionButton() != null) {
                    return handle;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean tryApproveInAnySeleniumContext(int waitSeconds) {
        long deadline = System.currentTimeMillis() + (Math.max(1, waitSeconds) * 1000L);
        String originalHandle = driver.getWindowHandle();

        while (System.currentTimeMillis() < deadline) {
            Set<String> handles = driver.getWindowHandles();
            for (String handle : handles) {
                try {
                    driver.switchTo().window(handle);
                    if (tryApproveInCurrentContext()) {
                        if (driver.getWindowHandles().contains(originalHandle)) {
                            driver.switchTo().window(originalHandle);
                        }
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            WaitUtils.sleep(120);
        }

        if (driver.getWindowHandles().contains(originalHandle)) {
            driver.switchTo().window(originalHandle);
        }
        return false;
    }

    private boolean tryApproveInCurrentContext() {
        if (isWalletLocked()) {
            return false;
        }

        if (clickApproveFromConfirmTransactionLayout()) {
            System.out.println("[WalletModalPage] Clicked Keplr action from Confirm Transaction layout.");
            return true;
        }

        if (clickApproveFromProvidedSelector()) {
            System.out.println("[WalletModalPage] Clicked Keplr action from provided selector: div.sc-gUAEMC.jxcvNv");
            return true;
        }

        String shadowLabel = clickShadowDomActionButton();
        if (shadowLabel != null) {
            System.out.println("[WalletModalPage] Clicked Keplr action (shadow DOM): " + shadowLabel);
            return true;
        }

        WebElement actionButton = findClickableActionButton();
        if (actionButton != null) {
            String label = actionButton.getText().trim();
            click(actionButton);
            System.out.println("[WalletModalPage] Clicked Keplr action: " + (label.isEmpty() ? "<no-label>" : label));
            return true;
        }
        return false;
    }

    private boolean hasApproveIndicatorsInCurrentContext() {
        try {
            List<WebElement> contextNodes = driver.findElements(keplrConfirmTxContext);
            if (!contextNodes.isEmpty()) {
                return true;
            }
        } catch (Exception ignored) {
        }

        try {
            List<WebElement> containers = driver.findElements(keplrApproveContainerByClass);
            for (WebElement container : containers) {
                if (container.isDisplayed()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Object hasShadowApprove = ((JavascriptExecutor) driver).executeScript(
                    "const roots=[document];" +
                            "const seen=new Set();" +
                            "while(roots.length){" +
                            "  const root=roots.shift();" +
                            "  if(!root || seen.has(root)) continue;" +
                            "  seen.add(root);" +
                            "  if(root.querySelector('div.sc-gUAEMC.jxcvNv')) return true;" +
                            "  const btns=root.querySelectorAll('button,[role=\"button\"],div');" +
                            "  for(const b of btns){" +
                            "    const t=(b.innerText||b.textContent||'').toLowerCase();" +
                            "    if(t.trim()==='approve') return true;" +
                            "  }" +
                            "  const all=root.querySelectorAll('*');" +
                            "  for(const el of all){ if(el.shadowRoot) roots.push(el.shadowRoot); }" +
                            "}" +
                            "return false;");
            return Boolean.TRUE.equals(hasShadowApprove);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean approveCurrentPopup(String contextHandle, boolean expectWindowClose) {
        boolean clickedAny = false;
        int idleRoundsAfterClick = 0;
        int unlockWaitSeconds = Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("keplr.unlock.wait.seconds", "120"));
        long unlockDeadline = System.currentTimeMillis() + (unlockWaitSeconds * 1000L);

        // Single popup can have multiple steps (Next -> Approve/Sign).
        for (int i = 0; i < 10; i++) {
            if (expectWindowClose && isWindowClosed(contextHandle)) {
                break;
            }

            if (isWalletLocked()) {
                System.out.println("[WalletModalPage] Keplr is locked. Enter password in popup and continue.");
                waitForUnlockScreenToClear(unlockDeadline);
            }

            if (clickApproveFromConfirmTransactionLayout()) {
                idleRoundsAfterClick = 0;
                clickedAny = true;
                System.out.println("[WalletModalPage] Clicked Keplr action from Confirm Transaction layout.");
                WaitUtils.sleep(90);
                continue;
            }

            if (clickApproveFromProvidedSelector()) {
                idleRoundsAfterClick = 0;
                clickedAny = true;
                System.out.println("[WalletModalPage] Clicked Keplr action from provided selector: div.sc-gUAEMC.jxcvNv");
                WaitUtils.sleep(90);
                continue;
            }

            WebElement actionButton = findClickableActionButton();
            if (actionButton == null) {
                String shadowLabel = clickShadowDomActionButton();
                if (shadowLabel != null) {
                    idleRoundsAfterClick = 0;
                    clickedAny = true;
                    System.out.println("[WalletModalPage] Clicked Keplr action (shadow DOM): " + shadowLabel);
                    WaitUtils.sleep(90);
                    continue;
                }

                if (clickedAny) {
                    idleRoundsAfterClick++;
                    if (idleRoundsAfterClick >= 3) {
                        break;
                    }
                }
                WaitUtils.sleep(80);
                continue;
            }

            idleRoundsAfterClick = 0;
            String label = actionButton.getText().trim();
            click(actionButton);
            clickedAny = true;
            System.out.println("[WalletModalPage] Clicked Keplr action: " + (label.isEmpty() ? "<no-label>" : label));

            WaitUtils.sleep(90);
            if (expectWindowClose && isWindowClosed(contextHandle)) {
                break;
            }
        }

        return clickedAny;
    }

    private boolean isWalletLocked() {
        try {
            List<WebElement> passwordInputs = driver.findElements(keplrPasswordInput);
            for (WebElement input : passwordInputs) {
                if (input.isDisplayed()) {
                    return true;
                }
            }
            return hasPasswordFieldInShadowDom();
        } catch (NoSuchWindowException e) {
            return false;
        }
    }

    private void waitForUnlockScreenToClear(long deadlineMillis) {
        while (System.currentTimeMillis() < deadlineMillis) {
            if (isCurrentWindowClosed()) {
                return;
            }
            if (!isWalletLocked()) {
                return;
            }
            WaitUtils.sleep(250);
        }
        throw new RuntimeException("Keplr unlock screen did not clear in time. Enter password and click Unlock in popup.");
    }

    private WebElement findClickableActionButton() {
        List<WebElement> buttons = driver.findElements(keplrActionButton);
        for (WebElement button : buttons) {
            if (button.isDisplayed() && button.isEnabled()) {
                return button;
            }
        }
        return null;
    }

    private boolean clickApproveFromProvidedSelector() {
        try {
            List<WebElement> containers = driver.findElements(keplrApproveContainerByClass);
            for (WebElement container : containers) {
                if (!container.isDisplayed()) {
                    continue;
                }

                // Prefer explicit "Approve" child element inside container.
                List<WebElement> approveNodes = container.findElements(keplrApproveButtonInContainer);
                for (WebElement approveNode : approveNodes) {
                    if (approveNode.isDisplayed() && approveNode.isEnabled()) {
                        click(approveNode);
                        return true;
                    }
                }

                // Fallback: click the container itself if child is not discoverable.
                click(container);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean clickApproveFromConfirmTransactionLayout() {
        try {
            List<WebElement> contexts = driver.findElements(keplrConfirmTxContext);
            if (contexts.isEmpty()) {
                // Even if context text isn't resolved due dynamic nodes, still try explicit approve button.
                List<WebElement> approveButtons = driver.findElements(keplrApproveTextButton);
                for (WebElement button : approveButtons) {
                    if (button.isDisplayed() && button.isEnabled()) {
                        click(button);
                        return true;
                    }
                }
                return false;
            }

            for (WebElement context : contexts) {
                if (!context.isDisplayed()) {
                    continue;
                }

                // Prefer approve nodes inside the same visual context.
                List<WebElement> approveInside = context.findElements(By.xpath(
                        ".//*[self::button or @role='button' or self::div][not(@disabled) and " +
                                "(normalize-space()='Approve' or .//*[normalize-space()='Approve'] or " +
                                "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), ' approve'))]"));
                for (WebElement approve : approveInside) {
                    if (approve.isDisplayed() && approve.isEnabled()) {
                        click(approve);
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean isLikelyKeplrContext() {
        try {
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl != null && currentUrl.startsWith("chrome-extension://")) {
                return true;
            }
        } catch (Exception ignored) {
        }

        try {
            String title = driver.getTitle();
            return title != null && title.toLowerCase().contains("keplr");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasPasswordFieldInShadowDom() {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "const roots=[document];" +
                            "const seen=new Set();" +
                            "while(roots.length){" +
                            "  const root=roots.shift();" +
                            "  if(!root || seen.has(root)) continue;" +
                            "  seen.add(root);" +
                            "  const input=root.querySelector('input[type=\"password\"]');" +
                            "  if(input) return true;" +
                            "  const all=root.querySelectorAll('*');" +
                            "  for(const el of all){ if(el.shadowRoot) roots.push(el.shadowRoot); }" +
                            "}" +
                            "return false;");
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private String clickShadowDomActionButton() {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "const include=['unlock','next','approve','connect','confirm','sign','allow','grant'];" +
                    "const exclude=['reject','cancel','deny','close'];" +
                    "const roots=[document];" +
                    "const seen=new Set();" +
                    "const isVisible=(el)=>{" +
                    "  if(!el) return false;" +
                    "  const s=getComputedStyle(el);" +
                    "  if(s.visibility==='hidden' || s.display==='none') return false;" +
                    "  const r=el.getBoundingClientRect();" +
                    "  return r.width>0 && r.height>0;" +
                    "};" +
                    "while(roots.length){" +
                    "  const root=roots.shift();" +
                    "  if(!root || seen.has(root)) continue;" +
                    "  seen.add(root);" +
                    "  const txCtx = root.querySelector('*');" +
                    "  if(txCtx){" +
                    "    const allNodes = root.querySelectorAll('*');" +
                    "    let hasConfirm=false; let hasMsg=false;" +
                    "    for(const n of allNodes){" +
                    "      const t=(n.innerText||n.textContent||'').toLowerCase();" +
                    "      if(!hasConfirm && t.includes('confirm transaction')) hasConfirm=true;" +
                    "      if(!hasMsg && t.includes('msgadddid')) hasMsg=true;" +
                    "      if(hasConfirm && hasMsg) break;" +
                    "    }" +
                    "    if(hasConfirm && hasMsg){" +
                    "      for(const n of allNodes){" +
                    "        const raw=(n.innerText||n.textContent||'').trim();" +
                    "        const t=raw.toLowerCase();" +
                    "        if((n.tagName==='BUTTON' || n.getAttribute('role')==='button' || n.tagName==='DIV') && t==='approve' && isVisible(n)){" +
                    "          n.click(); return raw || 'approve';" +
                    "        }" +
                    "      }" +
                    "    }" +
                    "  }" +
                    "  const approveContainer=root.querySelector('div.sc-gUAEMC.jxcvNv');" +
                    "  if(approveContainer && isVisible(approveContainer)){" +
                    "    const btn=approveContainer.querySelector('button,[role=\"button\"],div');" +
                    "    if(btn && isVisible(btn)){ btn.click(); return (btn.innerText||btn.textContent||'approve').trim(); }" +
                    "    approveContainer.click(); return 'approve-container';" +
                    "  }" +
                    "  const candidates=root.querySelectorAll('button,[role=\"button\"],input[type=\"submit\"],input[type=\"button\"]');" +
                    "  for(const el of candidates){" +
                    "    const raw=(el.innerText||el.textContent||el.value||el.getAttribute('aria-label')||'').trim();" +
                    "    const txt=raw.toLowerCase();" +
                    "    if(!txt) continue;" +
                            "    if(exclude.some(w=>txt.includes(w))) continue;" +
                            "    if(!include.some(w=>txt.includes(w))) continue;" +
                            "    if(el.disabled || el.getAttribute('disabled')!==null) continue;" +
                            "    if(!isVisible(el)) continue;" +
                            "    el.click();" +
                            "    return raw;" +
                            "  }" +
                            "  const all=root.querySelectorAll('*');" +
                            "  for(const el of all){ if(el.shadowRoot) roots.push(el.shadowRoot); }" +
                            "}" +
                            "return null;");
            return result == null ? null : result.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean attemptNativeApproveFallback(int waitSeconds) {
        boolean enableNativeFallback = Boolean.parseBoolean(
                com.verana.utils.DriverManager.getConfig().getProperty("keplr.native.approve.fallback", "true"));
        if (!enableNativeFallback) {
            return false;
        }

        long deadline = System.currentTimeMillis() + (Math.max(1, waitSeconds) * 1000L);
        while (System.currentTimeMillis() < deadline) {
            if (clickApproveInKeplrWindow()) {
                return true;
            }
            WaitUtils.sleep(250);
        }
        return false;
    }

    private boolean clickApproveInKeplrWindow() {
        try {
            // Find Chrome window containing "Keplr" (or smallest popup-like window) and click near
            // bottom-right where Approve is shown.
            String script =
                    "set didClick to false\n" +
                            "set foundProcess to false\n" +
                            "tell application \"System Events\"\n" +
                            "  set candidateProcesses to {\"Google Chrome\", \"Chrome\"}\n" +
                            "  repeat with pname in candidateProcesses\n" +
                            "    if exists process (contents of pname) then\n" +
                            "      set foundProcess to true\n" +
                            "      tell process (contents of pname)\n" +
                            "        set frontmost to true\n" +
                            "        set targetWindow to missing value\n" +
                            "        repeat with w in windows\n" +
                            "          set wname to \"\"\n" +
                            "          set wSize to 0\n" +
                            "          set hSize to 0\n" +
                            "          try\n" +
                            "            set wname to name of w\n" +
                            "          end try\n" +
                            "          try\n" +
                            "            set {wSize, hSize} to size of w\n" +
                            "          end try\n" +
                            "          if wname contains \"Keplr\" then\n" +
                            "            set targetWindow to w\n" +
                            "            exit repeat\n" +
                            "          end if\n" +
                            "          if targetWindow is missing value and wSize > 250 and wSize < 700 and hSize > 350 and hSize < 900 then\n" +
                            "            set targetWindow to w\n" +
                            "          end if\n" +
                            "        end repeat\n" +
                            "        if targetWindow is not missing value then\n" +
                            "          set {xPos, yPos} to position of targetWindow\n" +
                            "          set {wSize, hSize} to size of targetWindow\n" +
                            "          set clickX to (xPos + (wSize * 3 / 4))\n" +
                            "          set clickY to (yPos + hSize - 36)\n" +
                            "          click at {clickX, clickY}\n" +
                            "          delay 0.08\n" +
                            "          key code 36\n" +
                            "          set didClick to true\n" +
                            "        end if\n" +
                            "      end tell\n" +
                            "      if didClick then exit repeat\n" +
                            "    end if\n" +
                            "  end repeat\n" +
                            "end tell\n" +
                            "if didClick then\n" +
                            "  return \"clicked\"\n" +
                            "else if foundProcess then\n" +
                            "  return \"no_window\"\n" +
                            "else\n" +
                            "  return \"no_process\"\n" +
                            "end if";

            Process clickProcess = new ProcessBuilder("osascript", "-e", script).start();
            clickProcess.waitFor();

            String output = new String(clickProcess.getInputStream().readAllBytes()).trim().toLowerCase();
            boolean clicked = "clicked".equals(output);
            if (clicked) {
                System.out.println(
                        "[WalletModalPage] Native fallback attempted: clicked Keplr window Approve area and pressed Enter.");
                WaitUtils.sleep(300);
            }
            if ("no_process".equals(output)) {
                System.out.println("[WalletModalPage] Native fallback: Chrome process not found via AppleScript.");
            } else if ("no_window".equals(output)) {
                System.out.println("[WalletModalPage] Native fallback: Chrome found but Keplr-like window not found yet.");
            }
            return clicked;
        } catch (Exception e) {
            System.out.println("[WalletModalPage] Native approve fallback unavailable: " + e.getMessage());
            return false;
        }
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
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            }
        }
    }

    private boolean isPopupClosed() {
        try {
            return driver.getWindowHandles().size() == 1;
        } catch (NoSuchWindowException e) {
            return true;
        }
    }

    private boolean isWindowClosed(String handle) {
        try {
            return !driver.getWindowHandles().contains(handle);
        } catch (NoSuchWindowException e) {
            return true;
        }
    }

    private boolean isCurrentWindowClosed() {
        try {
            driver.getTitle();
            return false;
        } catch (NoSuchWindowException e) {
            return true;
        }
    }
}
