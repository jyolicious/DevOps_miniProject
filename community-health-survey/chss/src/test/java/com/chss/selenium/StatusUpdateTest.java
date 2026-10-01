package com.chss.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusUpdateTest extends BaseSeleniumTest {
    @Test
    void administratorPerformsForwardStatusWorkflow() {
        loginAs("admin1");
        String name = uniqueName();
        createSurvey(name);
        driver.findElement(By.xpath("//tr[td[normalize-space()=" + xpathLiteral(name) + "]]//a[normalize-space()='View']")).click();
        wait.until(ExpectedConditions.urlContains("/surveys/"));
        By status = By.xpath("//tr[th[normalize-space()='Status']]/td");
        for (String next : new String[]{"SUBMITTED", "VERIFIED", "CLOSED"}) {
            new Select(driver.findElement(By.name("target"))).selectByVisibleText(next);
            driver.findElement(By.xpath("//form[.//select[@name='target']]//button[@type='submit']")).click();
            wait.until(ExpectedConditions.textToBePresentInElementLocated(status, next));
        }
        assertTrue(driver.findElement(status).getText().equals("CLOSED"));
    }
}
