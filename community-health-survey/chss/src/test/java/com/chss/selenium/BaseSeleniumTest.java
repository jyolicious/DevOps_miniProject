package com.chss.selenium;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.TimeoutException;
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
        wait.until(d -> "complete".equals(((JavascriptExecutor) d)
                .executeScript("return document.readyState")));
        setSurveyFieldValue(By.name("name"), name);
        setSurveyFieldValue(By.name("age"), "34");
        setSurveyFieldValue(By.name("gender"), "Synthetic");
        setSurveyFieldValue(By.name("location"), "TestLocation_01");
        setSurveyFieldValue(By.name("healthCondition"), "SyntheticCondition");
        String expectedDate = LocalDate.now().toString();
        setSurveyFieldValue(By.name("surveyDate"), expectedDate);
        try {
            wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return arguments[0].checkValidity()", d.findElement(By.cssSelector("form")))));
        } catch (TimeoutException invalidForm) {
            assertTrue(false, "Survey form is invalid before submission. Invalid controls: " +
                    ((JavascriptExecutor) driver).executeScript(
                            "return Array.from(arguments[0].elements)" +
                                    ".filter(element => !element.checkValidity())" +
                                    ".map(element => ({name: element.name, type: element.type, value: element.value, " +
                                    "message: element.validationMessage}))",
                            driver.findElement(By.cssSelector("form"))));
        }
        assertTrue((Boolean) ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].checkValidity()", driver.findElement(By.cssSelector("form"))),
                "Survey form must satisfy browser validation before submission");
        WebElement submitButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']")));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].requestSubmit(arguments[1])",
                driver.findElement(By.cssSelector("form")), submitButton);
        wait.until(ExpectedConditions.urlToBe(baseUrl + "/surveys"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main h1"), "Survey Records"));
        wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//td[normalize-space()=" + xpathLiteral(name) + "]"))));
    }

    private void setSurveyFieldValue(By locator, String value) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles:true})); " +
                        "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                field, value);
        wait.until(d -> value.equals(d.findElement(locator).getDomProperty("value")));
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
