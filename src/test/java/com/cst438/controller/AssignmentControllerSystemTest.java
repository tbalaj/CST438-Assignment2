package com.cst438.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class AssignmentControllerSystemTest {

    // TODO edit the following to give the location and file name
    // of the Chrome driver.
    // for WinOS the file name will be chromedriver.exe
    // for MacOS the file name will be chromedriver
    public static final String CHROME_DRIVER_FILE_LOCATION = "C:/chromedriver_win32/chromedriver.exe";

    // public static final String CHROME_DRIVER_FILE_LOCATION =
    // "~/chromedriver_macOS/chromedriver";
    public static final String URL = "http://localhost:3000";

    public static final int SLEEP_DURATION = 1000; // 1 second.

    // add selenium dependency to pom.xml

    // these tests assumes that test data does NOT contain any
    // sections for course cst499 in 2024 Spring term.

    WebDriver driver;

    @BeforeEach
    public void setUpDriver() throws Exception {

        // set properties required by Chrome Driver
        System.setProperty(
                "webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
        ChromeOptions ops = new ChromeOptions();
        ops.addArguments("--remote-allow-origins=*");

        // start the driver
        driver = new ChromeDriver(ops);

        driver.get(URL);
        // must have a short wait to allow time for the page to download
        Thread.sleep(SLEEP_DURATION);

    }

    @AfterEach
    public void terminateDriver() {
        if (driver != null) {
            // quit driver
            driver.close();
            driver.quit();
            driver = null;
        }
    }

    @Test
    public void systemAddAssignment() throws Exception {
        String assignmentTitle = "End To End Test Assignment";

        // Find Section
        driver.findElement(By.id("year")).sendKeys("2024");
        driver.findElement(By.id("semester")).sendKeys("Spring");
        Thread.sleep(SLEEP_DURATION);

        driver.findElement(By.id("sectionLink")).click();

        Thread.sleep(SLEEP_DURATION);
        // Click on first entry
        driver.findElements(By.xpath("//tr/td/a[contains(@href, '/assignments')]")).get(0).click();
        // Click on add assignment buttoin
        driver.findElement(By.id("addAssignment")).click();
        // Populate dialog Popup
        driver.findElement(By.id("assignTitle")).sendKeys(assignmentTitle);
        driver.findElement(By.id("assignDueDate")).sendKeys("2024-02-01");
        Thread.sleep(SLEEP_DURATION);
        // Save
        driver.findElement(By.id("save")).click();

        // Delete newly added assignment from end-to-end test.
        Thread.sleep(SLEEP_DURATION);
        String xpathForDeleteButton = String.format("//tr[td[2][text()='%s']]//button[text()='Delete']",
                assignmentTitle);
        driver.findElement(By.xpath(xpathForDeleteButton)).click();
        Thread.sleep(SLEEP_DURATION);

        driver.findElement(By.className("react-confirm-alert-button-group"))
                .findElements(By.tagName("button")).get(0).click();
        Thread.sleep(SLEEP_DURATION);

    }

    @Test
    public void systemGradeAssignments() throws Exception {
        Random random = new Random();
        int min = 75;
        int max = 100;

        // Generate random number between 75 (inclusive) and 100 (inclusive)
        // Find Section
        driver.findElement(By.id("year")).sendKeys("2024");
        driver.findElement(By.id("semester")).sendKeys("Spring");
        Thread.sleep(SLEEP_DURATION);

        driver.findElement(By.id("sectionLink")).click();

        Thread.sleep(SLEEP_DURATION);
        // Click on first entry
        driver.findElements(By.xpath("//tr/td/a[contains(@href, '/assignments')]")).get(0).click();
        // Click on Grade the first assignment
        driver.findElements(By.xpath("//tbody//button[text()='Grade']")).get(0).click();
        // Populate dialog Popup
        driver.findElements(By.xpath("//tbody//input")).stream().forEach(element -> {
            element.sendKeys(Keys.chord(Keys.CONTROL, "a", Keys.DELETE));
            int randomNumber = random.nextInt((max - min) + 1) + min;
            element.sendKeys(String.valueOf(randomNumber));
        });
        Thread.sleep(SLEEP_DURATION);
        // Save
        driver.findElement(By.xpath("//button[text()='Save']")).click();
        Thread.sleep(SLEEP_DURATION);

        // Check for success message.
        String message = driver.findElement(By.xpath("//div[contains(@class, 'MuiDialogContent-root')]//h4")).getText();
        assertTrue(message.contains("Grades saved"));
        Thread.sleep(SLEEP_DURATION);
    }
}
