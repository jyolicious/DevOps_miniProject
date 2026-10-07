package com.chss.selenium;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Requires the application to be running locally at port 8081 before mvn test. */
public abstract class BaseSeleniumTest {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected final String baseUrl = System.getProperty(
            "selenium.base.url",
            System.getenv().getOrDefault("SELENIUM_BASE_URL", "http://localhost:8081"));

    @RegisterExtension
    final TestWatcher screenshots = new TestWatcher() {
        @Override public void testFailed(ExtensionContext context, Throwable cause) {
            if (driver != null) {
                try {
                    Path directory = Path.of("target", "selenium-screenshots");
                    Files.createDirectories(directory);
                    String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
                    String filename = context.getRequiredTestClass().getSimpleName() + "_" +
                            context.getRequiredTestMethod().getName() + "_" + stamp + ".png";
                    Files.write(directory.resolve(filename), ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
                } catch (IOException | RuntimeException screenshotError) {
                    System.err.println("Could not capture Selenium failure screenshot: " + screenshotError.getMessage());
                }
            }
            quitDriver();
        }
        @Override public void testSuccessful(ExtensionContext context) { quitDriver(); }
        @Override public void testAborted(ExtensionContext context, Throwable cause) { quitDriver(); }
    };

    @BeforeEach
    void startChrome() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        driver = new ChromeDriver(options); // Selenium Manager resolves the matching driver.
        try { driver.manage().window().maximize(); } catch (RuntimeException ignored) { /* headless/limited desktop */ }
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(baseUrl);
    }

    protected void loginAs(String username) {
        driver.get(baseUrl + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }

    protected String uniqueName() {
        return "SeleniumUser_" + UUID.randomUUID().toString().replace("-", "");
    }

    protected void createSurvey(String name) {
        driver.get(baseUrl + "/surveys/new");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name"))).sendKeys(name);
        driver.findElement(By.name("age")).sendKeys("34");
        driver.findElement(By.name("gender")).sendKeys("Synthetic");
        driver.findElement(By.name("location")).sendKeys("TestLocation_01");
        driver.findElement(By.name("healthCondition")).sendKeys("SyntheticCondition");
        var surveyDate = driver.findElement(By.name("surveyDate"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles:true})); " +
                        "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                surveyDate, LocalDate.now().toString());
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.urlToBe(baseUrl + "/surveys"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main h1"), "Survey Records"));
        wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//td[normalize-space()=" + xpathLiteral(name) + "]"))));
    }

    protected void assertVisibleText(String text) {
        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[normalize-space()=" + xpathLiteral(text) + "]"))).isDisplayed(), text + " should be visible");
    }

    protected static String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }

    private void quitDriver() {
        if (driver != null) {
            try { driver.quit(); } catch (RuntimeException ignored) { }
            driver = null;
        }
    }
}
