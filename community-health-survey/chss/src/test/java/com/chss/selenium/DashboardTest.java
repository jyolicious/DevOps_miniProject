package com.chss.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardTest extends BaseSeleniumTest {
    @Test
    void dashboardDisplaysNumericSurveySummary() {
        loginAs("surveyor1");
        driver.get(baseUrl + "/dashboard");
        assertVisibleText("Survey Summary");
        for (String label : List.of("Total", "Draft", "Submitted", "Verified", "Closed")) {
            WebElement card = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class,'card')][.//div[normalize-space()=" + xpathLiteral(label) + "]]")));
            assertTrue(card.isDisplayed());
            String count = card.findElement(By.cssSelector(".count")).getText();
            assertTrue(count.matches("\\d+"), label + " count should be numeric but was: " + count);
        }
    }
}
