package com.chss.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateSurveyTest extends BaseSeleniumTest {
    @Test
    void surveyorCreatesNewSurveyRecord() {
        loginAs("surveyor1");
        driver.get(baseUrl + "/surveys");
        driver.findElement(By.linkText("+ New Record")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("name")));
        String name = uniqueName();
        createSurvey(name);
        assertTrue(driver.findElement(By.xpath("//td[normalize-space()=" + xpathLiteral(name) + "]")).isDisplayed());
        assertTrue(driver.findElement(By.xpath("//tr[td[normalize-space()=" + xpathLiteral(name) + "]]"))
                .getText().contains("DRAFT"));
    }
}
