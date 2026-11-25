package com.example.student.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.example.student.repository.StudentRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("selenium")
@EnabledIfSystemProperty(named = "runSelenium", matches = "true")
class StudentRegistrationSeleniumTest {

    @LocalServerPort
    private int port;

    @Autowired
    private StudentRepository studentRepository;

    private WebDriver driver;

    @BeforeEach
    void openRegistrationPage() {
        studentRepository.deleteAll();
        String chromeDriverPath = System.getenv("CHROMEDRIVER_PATH");
        if (chromeDriverPath != null && !chromeDriverPath.isBlank()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        driver.get("http://localhost:" + port);
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void registersAndDeletesStudentFromThePage() {
        driver.findElement(By.id("name")).sendKeys("Selenium Student");
        driver.findElement(By.id("email")).sendKeys("selenium.student@example.com");
        new Select(driver.findElement(By.id("course"))).selectByVisibleText("Java");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals("Selenium Student", driver.findElement(By.cssSelector("tbody tr td:nth-child(2)")).getText());

        driver.findElement(By.linkText("Delete")).click();
        Alert confirmation = driver.switchTo().alert();
        confirmation.accept();

        assertEquals("No student records found",
                driver.findElement(By.cssSelector("tbody tr td")).getText());
    }

    @Test
    void preventsSubmittingNamesWithDigitsInTheBrowser() {
        driver.findElement(By.id("name")).sendKeys("Student2");
        driver.findElement(By.id("email")).sendKeys("selenium.student@example.com");
        new Select(driver.findElement(By.id("course"))).selectByVisibleText("Java");

        Boolean valid = (Boolean) ((JavascriptExecutor) driver)
                .executeScript("return document.getElementById('name').checkValidity();");

        assertFalse(valid);
        assertEquals(0, studentRepository.count());
    }
}
