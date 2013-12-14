package com.felipe.airlinemanager;

import java.util.concurrent.TimeUnit;

import org.junit.*;

import static org.junit.Assert.*;

import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class StartFlight{

private WebDriver driver;
  private String baseUrl;  
  
  @Before
  public void setUp() throws Exception {
    driver = new FirefoxDriver();
    this.baseUrl = "https://apps.facebook.com/airline_manager/route.php?token=flight";
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
  }

  @Test
  public void testAirlineManager() throws Exception {
    driver.get(this.baseUrl);
    
    //Login To FB
    WebElement emailInput = driver.findElement(By.id("email"));    
    emailInput.sendKeys("felipao_8_7@hotmail.com");    
    WebElement pswInput = driver.findElement(By.id("pass"));
    pswInput.sendKeys("681231531fb");    
    WebElement sendInput = driver.findElement(By.id("u_0_1"));
    sendInput.click();
    
    manageAC();
    
  }
  
  
  private void manageAC() throws Exception {
	
	  	moveToFlightTab();
	    
		WebElement container = driver.findElement(By.xpath("//*[@id='flight']/table[2]/tbody/tr[2]/td[5]"));
		
		if (container.findElements(By.xpath(".//a")).size() != 0){
			container.findElement(By.xpath(".//a")).click();
			
			Thread.sleep(10000);
			
			manageAC();
		}else{
		
		    WebElement countDown = driver.findElement(By.id("c0"));
		    String countDownFull = countDown.getText();
		    
		    Long countDownSeconds = Long.valueOf(countDownFull.split(":")[2]);
		    Long countDownMinutes = Long.valueOf(countDownFull.split(":")[1]);
		    Long countDownHours = Long.valueOf(countDownFull.split(":")[0]);
		    
		    countDownMinutes = countDownMinutes + (countDownHours * 60);
		    countDownSeconds = countDownSeconds + (countDownMinutes * 60);
		    
		    Thread.sleep(countDownSeconds * 1000);
		    
		    manageAC();
		}
  }
  
  private void moveToFlightTab(){
	//Go to flights tab
    driver.get(baseUrl);    
    driver.switchTo().frame("iframe_canvas_fb_https");	    
  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
  }

}
