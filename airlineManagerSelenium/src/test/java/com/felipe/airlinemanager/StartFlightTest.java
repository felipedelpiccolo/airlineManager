package com.felipe.airlinemanager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class StartFlightTest{

  private WebDriver driver;
  private String baseUrl;
  
  private String dbFilePath = this.getClass().getResource("/db/flightsLogs.sqlite").getPath().replace("target/test-classes","src/test/resources");
  
  private String chromeDriverPath = this.getClass().getResource("/chromedriver.exe").getPath();
  
  private Connection dbConnection = null;
  private Statement stmt = null;
  
  private static ChromeDriverService chromeService;
  
  @Before
  public void setUp() throws Exception {
	
//	  chromeService = new ChromeDriverService.Builder()
//	  						.usingDriverExecutable(new File(chromeDriverPath))
//	  						.usingAnyFreePort()
//	  						.build();
//	  
//	 chromeService.start();	  
//	  
//    driver = new RemoteWebDriver(chromeService.getUrl(),DesiredCapabilities.chrome());
	  
	  driver = new FirefoxDriver();
    
    baseUrl = "https://apps.facebook.com/airline_manager/route.php?token=flight";
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    
  }

  @Test
  public void testAirlineManager() throws Exception {
    driver.get("http://apps.facebook.com/airline_manager/");
    
    //Login To FB
    WebElement emailInput = driver.findElement(By.id("email"));    
    emailInput.sendKeys("felipao_8_7@hotmail.com");    
    WebElement pswInput = driver.findElement(By.id("pass"));
    pswInput.sendKeys("681231531fb");    
    WebElement sendInput = driver.findElement(By.id("u_0_1"));
    sendInput.click();
    
    Thread.sleep(5000);
    
    System.out.println("start...");
    
    setupFlights();
    
    manageAC();
    
  }
  
  private void setupFlights() throws SQLException, ClassNotFoundException{
	moveToFlightTab();
	  	  
	WebElement flightsContainer = driver.findElement(By.xpath("//*[@id='routeStarter']/table/tbody"));
	
	List<WebElement> flights = flightsContainer.findElements(By.xpath(".//tr"));
	  
	Iterator<WebElement> flightsIterator = flights.iterator();
	
	flightsIterator.next();
	
	WebElement flight = null;
	
	getDBConnection(false);
    
	stmt = dbConnection.createStatement();
	ResultSet storedFlights = stmt.executeQuery("SELECT * FROM flight");
    
	Set<String> storedFlightsRegs = new HashSet<String>();
	
	while(storedFlights.next()){
		storedFlightsRegs.add(storedFlights.getString("reg"));
	}
	
	while (flightsIterator.hasNext()) {
		flight = flightsIterator.next();
		String flightReg = flight.findElement(By.xpath(".//td[5]")).getText();
		String aircraft = flight.findElement(By.xpath(".//td[4]")).getText();
		
		if(!storedFlightsRegs.contains(flightReg)){
			stmt = dbConnection.createStatement();
			
			String	insertFlightSql = "INSERT INTO flight (reg,aircraft) VALUES ('"+flightReg+"', '"+aircraft+"');";
			stmt.executeUpdate(insertFlightSql);			
		}
		
	}
	
	closeConnection();
  }
  
  private void manageAC() throws Exception {
	
	  	moveToFlightTab();
	    
		WebElement container = driver.findElement(By.xpath("//*[@id='routeStarter']/table/tbody/tr[2]/td[6]"));

		System.out.println("checking flight to start....");
		
		if (container.findElements(By.xpath(".//a")).size() != 0){
			
			System.out.println("starting flight....");
			
			String flightReg = driver.findElement(By.xpath("//*[@id='routeStarter']/table/tbody/tr[2]/td[5]")).getText().trim();
			
			container.findElement(By.xpath(".//a")).click();
			Thread.sleep(10000);
			
			//Collect data
			WebElement flewResult = driver.findElement(By.id("singleStart"));
			
			String standardPax = flewResult.findElement(By.xpath(".//table[1]/tbody/tr[2]/td[2]")).getText().replace("Pax", "").trim();
			String businessPax = flewResult.findElement(By.xpath(".//table[1]/tbody/tr[2]/td[3]")).getText().replace("Pax", "").trim();
			String expenses = flewResult.findElement(By.xpath(".//table[1]/tbody/tr[2]/td[5]/font")).getText().replace("$", "").replace(",", "").trim();
			
			String fuelUsed = flewResult.findElement(By.xpath(".//table[2]/tbody/tr[1]/td[2]/b")).getText().replace("Lbs", "").replace(",", "").trim();
			String damages = flewResult.findElement(By.xpath(".//table[2]/tbody/tr[2]/td[2]/b")).getText().trim();
			
			String income;
			String mealsSold = "0";
			String cateringIncome = "0";
			
			WebElement thirdCol = flewResult.findElement(By.xpath(".//table[2]/tbody/tr[3]"));
			
			if(thirdCol.findElement(By.xpath(".//td[1]")).getText().contains("Total expenses:")){
				//is without catering
				income = flewResult.findElement(By.xpath(".//table[2]/tbody/tr[4]/td[2]/b/font")).getText().replace("$", "").replace(",", "").trim();
			}else{
				//is with catering
				mealsSold = thirdCol.findElement(By.xpath(".//td[2]/b")).getText().trim();
				cateringIncome = flewResult.findElement(By.xpath(".//table[2]/tbody/tr[4]/td[2]/b/font")).getText().replace("$", "").replace(",", "").trim();
				
				income = flewResult.findElement(By.xpath(".//table[2]/tbody/tr[6]/td[2]/b/font")).getText().replace("$", "").replace(",", "").trim();
			}
			
			getDBConnection(false);		    
			stmt = dbConnection.createStatement();
			
			stmt.executeUpdate("INSERT INTO trip (flightReg, standardPax, businessPax, expenses, fuel, damages, income, mealsSold, cateringIncome) " +
					"VALUES('"+
							flightReg       +"'," +
							standardPax     +"," +
							businessPax     +"," +
							expenses        +"," +
							fuelUsed        +"," +
							damages	        +"," +
							income	        +"," +
							mealsSold       +"," +
							cateringIncome  +")");
			
			closeConnection();
			
			manageAC();
		}else{
			
		    WebElement countDown = driver.findElement(By.id("c0"));
		    String countDownFull = countDown.getText();
		    
		    if(countDownFull.trim().equalsIgnoreCase("ready")){
		    	System.out.println("wait out while getting element value is: "+countDownFull+" ...");
		    	manageAC();
		    	return;
		    }
		    
		    System.out.println("wait for: "+countDownFull+" ...");
		    
		    Long countDownSeconds = Long.valueOf(countDownFull.split(":")[2]);
		    Long countDownMinutes = Long.valueOf(countDownFull.split(":")[1]);
		    Long countDownHours = Long.valueOf(countDownFull.split(":")[0]);
		    
		    countDownMinutes = countDownMinutes + (countDownHours * 60);
		    countDownSeconds = countDownSeconds + (countDownMinutes * 60);
		    
		    Thread.sleep(countDownSeconds * 1000);
		    
		    System.out.println("wait finished...");
		    
		    manageAC();
		}
  }
  
  private void getDBConnection(boolean autoCommit) throws ClassNotFoundException, SQLException{	  
	  Class.forName("org.sqlite.JDBC");
	  dbConnection = DriverManager.getConnection("jdbc:sqlite:"+dbFilePath);
	  dbConnection.setAutoCommit(autoCommit);
  }
  
  private void closeConnection() throws SQLException{
	  stmt.close();
	  dbConnection.commit();
	  dbConnection.close();
  }
  
  private void moveToFlightTab(){
	//Go to flights tab
    //driver.get(baseUrl);
	driver.navigate().to(this.baseUrl);
    driver.switchTo().frame("iframe_canvas_fb_https");	    
  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
  }

}
