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
        By viewLink = By.xpath("//tr[td[normalize-space()=" + xpathLiteral(name) + "]]//a[normalize-space()='View']");
        var link = wait.until(ExpectedConditions.elementToBeClickable(viewLink));
        String detailUrl = link.getDomProperty("href");
        driver.navigate().to(detailUrl);
        wait.until(ExpectedConditions.urlToBe(detailUrl));
        By status = By.xpath("//tr[th[normalize-space()='Status']]/td");
        wait.until(ExpectedConditions.visibilityOfElementLocated(status));
        for (String next : new String[]{"SUBMITTED", "VERIFIED", "CLOSED"}) {
            new Select(driver.findElement(By.name("target"))).selectByVisibleText(next);
            driver.findElement(By.xpath("//form[.//select[@name='target']]//button[@type='submit']")).click();
            wait.until(ExpectedConditions.textToBePresentInElementLocated(status, next));
        }
        assertTrue(driver.findElement(status).getText().equals("CLOSED"));
    }
}
