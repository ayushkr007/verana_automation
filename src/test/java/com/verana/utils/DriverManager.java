package com.verana.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

/**
 * DriverManager
 *
 * Creates Chrome with a dedicated persistent profile so Keplr remains installed
 * and signed in across runs.
 */
public class DriverManager {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final Properties config = loadConfig();

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties: " + e.getMessage(), e);
        }
        return props;
    }

    public static WebDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            driverThreadLocal.set(createDriver());
        }
        return driverThreadLocal.get();
    }

    private static WebDriver createDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        String userDataDir = config.getProperty("chrome.user.data.dir", "").trim();
        if (userDataDir.isEmpty()) {
            throw new IllegalStateException("Missing 'chrome.user.data.dir' in config.properties");
        }
        Path userDataPath = Paths.get(userDataDir).toAbsolutePath();
        ensureDirectoryExists(userDataPath);

        String profileDirectory = config.getProperty("chrome.profile.directory", "Default").trim();
        String chromeBinaryPath = config.getProperty("chrome.binary.path", "").trim();
        boolean headless = Boolean.parseBoolean(config.getProperty("browser.headless", "false"));

        options.addArguments("--user-data-dir=" + userDataPath);
        options.addArguments("--profile-directory=" + profileDirectory);
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        } else {
            options.addArguments("--start-maximized");
        }

        if (!chromeBinaryPath.isEmpty()) {
            options.setBinary(chromeBinaryPath);
        }

        System.out.println("[DriverManager] Starting Chrome with persistent profile:");
        System.out.println("               user-data-dir = " + userDataPath);
        System.out.println("               profile        = " + profileDirectory);
        System.out.println("               headless       = " + headless);

        int implicitWait = Integer.parseInt(config.getProperty("implicit.wait.seconds", "10"));
        int pageLoadTimeout = Integer.parseInt(config.getProperty("page.load.timeout.seconds", "60"));

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        if (!headless) {
            driver.manage().window().maximize();
        }

        System.out.println("[DriverManager] Chrome session started. Current URL: " + driver.getCurrentUrl());
        return driver;
    }

    private static void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create Chrome user-data-dir: " + dir, e);
        }
    }

    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
            driverThreadLocal.remove();
        }
    }

    public static Properties getConfig() {
        return config;
    }
}
