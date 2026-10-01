package com.chss.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginTest extends BaseSeleniumTest {
    @Test
    void testValidSurveyorLogin() {
        driver.get(baseUrl + "/login");
        driver.findElement(By.id("username")).sendKeys("surveyor1");
        driver.findElement(By.id("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        assertVisibleText("Survey Summary");
        assertTrue(driver.findElement(By.tagName("body")).getText().contains("surveyor1 / SURVEYOR"));
    }

    @Test
    void invalidCredentialsRemainOnLoginWithError() {
        driver.get(baseUrl + "/login");
        driver.findElement(By.id("username")).sendKeys("invalid_selenium_user");
        driver.findElement(By.id("password")).sendKeys("invalid_password");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        assertTrue(wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".error"))).isDisplayed());
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }
}
