package com.cst438.controller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import javax.swing.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StudentControllerSystemTest {

    public static final int SLEEP_DURATION = 1000; // 1 second.

    private WebDriver driver;


    @BeforeEach
    public void setUp() {
        System.setProperty("webdriver.chrome.driver", "/Users/harutlementsyan/Downloads/chromedriver-mac-arm64/chromedriver");
        driver = new ChromeDriver();

        driver.get("http://localhost:3000");
    }

    @AfterEach
    public void tearDown() {
        // Close the browser after each test
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testStudentEnrollsIntoSection() throws InterruptedException {

        // Find and click the "schedule" element
        WebElement assignmentsLink = driver.findElement(By.id("schedule"));
        assignmentsLink.click();

        // Find the "year" input field and type "2024"
        WebElement yearField = driver.findElement(By.id("year"));
        yearField.sendKeys("2024");

        // Find the "semester" input field and type "Spring"
        WebElement semesterField = driver.findElement(By.id("semester"));
        semesterField.sendKeys("Spring");

        // Find and click the "get schedule" button
        WebElement schedule = driver.findElement(By.tagName("button"));
        schedule.click();
        Thread.sleep(SLEEP_DURATION);

        // Wait for the schedule to load and find the first "Drop" button
        WebElement dropButton = driver.findElement(By.xpath("(//button[contains(@class, 'MuiButtonBase-root') and text()='Drop'])[1]"));
        dropButton.click();
        Thread.sleep(SLEEP_DURATION);

        // Confirm the drop by clicking "yes" on the confirmation dialog
        WebElement confirmButton = driver.findElement(By.xpath("//div[@class='react-confirm-alert-button-group']/button[1]"));
        confirmButton.click();
        Thread.sleep(SLEEP_DURATION);


    }

}
