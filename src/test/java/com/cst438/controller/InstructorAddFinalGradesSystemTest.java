package com.cst438.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;

public class InstructorAddFinalGradesSystemTest {

    public static final String CHROME_DRIVER_FILE_LOCATION =
            "C:/chromedriver-win32/chromedriver.exe";
    //"C:/chromedriver_win32/chromedriver.exe"; //*Original

    public static final String URL = "http://localhost:3000";

    public static final int SLEEP_DURATION = 1000; // 1 second.

    private final String GRADE = "B";
    private final String YEAR = "2024";
    private final String SEMESTER = "Spring";

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

    // Helper method to check if an element is present
    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Test
    public void systemTestAddFinalGrades() throws Exception {
        driver.findElement(By.id("year")).sendKeys(YEAR);
        driver.findElement(By.id("semester")).sendKeys(SEMESTER);

        driver.findElement(By.linkText("Show Sections")).click();
        Thread.sleep(SLEEP_DURATION);

        ///////////////////////////////////////////////////////////////////////////////////////
        // enrollments
        
        List<WebElement> linkElements = driver.findElements(By.id("enroll"));
        for(int i = 0; i < linkElements.size(); i++){

            linkElements = driver.findElements(By.id("enroll"));
            WebElement link = linkElements.get(i);
            link.click();
            Thread.sleep(SLEEP_DURATION);

            // Grade update
            if (!driver.findElements(By.xpath("//input[@name='grade']")).isEmpty()) {

                String currentGrade = driver.findElement(By.xpath("//input[@name='grade']")).getAttribute("value");

                if(!currentGrade.equals("A") && !currentGrade.equals("B") && !currentGrade.equals("C")
                        && !currentGrade.equals("D") && !currentGrade.equals("F")){
                    driver.findElement(By.xpath("//input[@name='grade']")).clear();
                    driver.findElement(By.xpath("//input[@name='grade']")).sendKeys(GRADE);
                    driver.findElement(By.xpath("//button[contains(text(), 'Save Grades')]")).click();
                    Thread.sleep(SLEEP_DURATION);
                } else {
                    System.out.println("Grade exist!");
                }

                Thread.sleep(SLEEP_DURATION);
            } else {
                System.out.println("Grade input does not exist.");
            }
            //

            driver.navigate().back();
            Thread.sleep(SLEEP_DURATION);
        }
    }
}
