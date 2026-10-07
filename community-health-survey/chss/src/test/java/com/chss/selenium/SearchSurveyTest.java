package com.chss.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchSurveyTest extends BaseSeleniumTest {
    @Test
    void surveyorSearchesForUniqueSurveyByNameCaseInsensitively() {
        loginAs("surveyor1");
        String name = uniqueName();
        createSurvey(name);
        driver.findElement(By.name("query")).clear();
        driver.findElement(By.name("query")).sendKeys(name.toLowerCase());
        driver.findElement(By.cssSelector("form button[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/surveys?query=" + name.toLowerCase()));
        By matchingSurvey = By.xpath("//td[normalize-space()=" + xpathLiteral(name) + "]");
        assertTrue(wait.until(ExpectedConditions.refreshed(
                ExpectedConditions.visibilityOfElementLocated(matchingSurvey))).isDisplayed());
        assertTrue(wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.tagName("body"), name)));
    }
}
