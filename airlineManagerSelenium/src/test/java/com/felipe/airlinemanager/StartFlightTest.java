package com.felipe.airlinemanager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.ErrorHandler.UnknownServerException;
import org.openqa.selenium.remote.RemoteWebDriver;

public class StartFlightTest{

	private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		    
	
  private WebDriver driver;
  
  private static final String baseUrl = "https://apps.facebook.com/airline_manager/";
  
  private static final String flightsUrl = "https://apps.facebook.com/airline_manager/route.php?token=flight";
  
  private static final String fuelPurchaseUrl = "https://airlinemanager.activewebs.dk/am/fuel.php?amount=";
  private static final int MAX_FUEL_VALUE = 200;
  private static final int MIN_FUEL_VALUE = 50;
  private static final int TANK_CAPACITY = 1000000000;
  
  private String dbFilePath = this.getClass().getResource("/db/flightsLogs.sqlite").getPath().replace("target/test-classes","src/test/resources");
  
  private String chromeDriverPath = this.getClass().getResource("/chromedriver.exe").getPath();
  
  private Connection dbConnection = null;
  private Statement stmt = null;
  
  private static ChromeDriverService chromeService;
  
  private List<String> flightRegsToSkipStart = new ArrayList<String>();
  
  @Before
  public void setUp() throws Exception {
	  
	  FileHandler logFile = new FileHandler(StartFlightTest.class.getCanonicalName()+".log");
	  logFile.setFormatter(new SimpleFormatter());
	  logger.addHandler(logFile);
	  
	  logger.setLevel(Level.FINEST);
	  
	  chromeService = new ChromeDriverService.Builder()
	  						.usingDriverExecutable(new File(chromeDriverPath))
	  						.usingAnyFreePort()
	  						.build();
	  
	 chromeService.start();	  
	  
    driver = new RemoteWebDriver(chromeService.getUrl(),DesiredCapabilities.chrome());
	  
	  //driver = new FirefoxDriver();
    
    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    
  }

  @Test
  public void testAirlineManager() throws Exception {
    driver.get("http://apps.facebook.com/airline_manager/");
    
    //Login To FB

    //Wait 20 seconds until user enters credentials
    Thread.sleep(20000);    

    WebElement sendInput = driver.findElement(By.id("u_0_0"));
    sendInput.click();
    
    Thread.sleep(5000);
    
    logger.info("Start..");
    
    purchaseFuel();
    
    setupFlights();
    
    try {
    	manageAC(0);
    } catch(UnknownServerException e){
    	logger.info("Error in manageAC: "+e);
    	driver.close();
    	testAirlineManager();
    }
    
  }
  
  private void purchaseFuel() throws InterruptedException {
	  moveToOverviewTab();
	  
	  WebElement fuelCostElement = driver.findElement(By.xpath("//*[@id='indexContent']/table/tbody/tr/td/table[1]/tbody/tr/td[1]/table/tbody/tr[4]/td/table/tbody/tr[5]/td[2]/font"));
	  
	  int fuelCost = Integer.valueOf(normalizeString(fuelCostElement.getText()));
	  
	  logger.info("Fuel Costing: "+fuelCost);
	  
	  if(fuelCost <= MAX_FUEL_VALUE) {
		  WebElement currentFuel = driver.findElement(By.xpath("//*[@id='fuelheader']"));
		  int tankCapacityLeft = TANK_CAPACITY - Integer.valueOf(normalizeString(currentFuel.getText()));
		  
		  logger.info("Amount to purchase: "+ tankCapacityLeft);
		  
		  int acountBalance = getAccountBalance();
		  
		  if ((tankCapacityLeft/1000*fuelCost) <= acountBalance) {
			  logger.info("Account Balance is enogh to purchase maximum fuel");
			  driver.get(fuelPurchaseUrl+tankCapacityLeft);
		  }else if (fuelCost <= MIN_FUEL_VALUE){
			  logger.info("Account Balance is not enogh, and is less than the minimun, purchase maximun posible");
			  int maxAmountLbsCanPurchase = acountBalance/fuelCost*1000;
			  driver.get(fuelPurchaseUrl+maxAmountLbsCanPurchase);
		  }else {
			  logger.info("Not going to purchase fuel.");
		  }

	  }
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
  
  private void manageAC(int totalSecondsWaited) throws Exception {
	  
	  	logger.info("total waited seconds: "+totalSecondsWaited);
	  
	  	if(totalSecondsWaited > 3600) {
	  		//total waiting more than one hour
	  		totalSecondsWaited = 0;
	  		purchaseFuel();
	  	}
	  
	  	moveToFlightTab();
	    
		WebElement container = driver.findElement(By.xpath("//*[@id='routeStarter']/table/tbody/tr[2]/td[6]"));

		logger.info("checking flight to start....");
		
		String flightReg = driver.findElement(By.xpath("//*[@id='routeStarter']/table/tbody/tr[2]/td[5]")).getText().trim();
		
		if (container.findElements(By.xpath(".//a")).size() != 0 && !flightRegsToSkipStart.contains(flightReg)){
			
			logger.info("starting flight....");
			
			container.findElement(By.xpath(".//a")).click();
			Thread.sleep(10000);
			
			//Collect data
			WebElement flewResult = driver.findElement(By.id("singleStart"));
			
			String standardPax = normalizeString(flewResult.findElement(By.xpath(".//table[1]/tbody/tr[2]/td[2]")).getText());
			String businessPax = normalizeString(flewResult.findElement(By.xpath(".//table[1]/tbody/tr[2]/td[3]")).getText());
			String expenses = normalizeString(flewResult.findElement(By.xpath(".//table[1]/tbody/tr[2]/td[5]/font")).getText());
			
			String fuelUsed = normalizeString(flewResult.findElement(By.xpath(".//table[2]/tbody/tr[1]/td[2]/b")).getText());
			String damages = normalizeString(flewResult.findElement(By.xpath(".//table[2]/tbody/tr[2]/td[2]/b")).getText());
			
			String income;
			String mealsSold = "0";
			String cateringIncome = "0";
			
			WebElement thirdCol = flewResult.findElement(By.xpath(".//table[2]/tbody/tr[3]"));
			
			if(thirdCol.findElement(By.xpath(".//td[1]")).getText().contains("Total expenses:")){
				//is without catering
				income = normalizeString(flewResult.findElement(By.xpath(".//table[2]/tbody/tr[4]/td[2]/b/font")).getText());
			}else{
				//is with catering
				mealsSold = normalizeString(thirdCol.findElement(By.xpath(".//td[2]/b")).getText());
				cateringIncome = normalizeString(flewResult.findElement(By.xpath(".//table[2]/tbody/tr[4]/td[2]/b/font")).getText());
				
				income = normalizeString(flewResult.findElement(By.xpath(".//table[2]/tbody/tr[6]/td[2]/b/font")).getText());
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
			
			manageAC(totalSecondsWaited);
		}else{
			
		    WebElement countDown = driver.findElement(By.id("c0"));
		    String countDownFull = countDown.getText();
		    
		    if(countDownFull.trim().equalsIgnoreCase("ready")){
		    	logger.info("wait out while getting element value is: "+countDownFull+" ...");
		    	manageAC(totalSecondsWaited);
		    	return;
		    }
		    
		    logger.info("wait for: "+countDownFull+" ...");
		    
		    Long countDownSeconds = Long.valueOf(countDownFull.split(":")[2]);
		    Long countDownMinutes = Long.valueOf(countDownFull.split(":")[1]);
		    Long countDownHours = Long.valueOf(countDownFull.split(":")[0]);
		    
		    countDownMinutes = countDownMinutes + (countDownHours * 60);
		    countDownSeconds = countDownSeconds + (countDownMinutes * 60);
		    
		    Thread.sleep(countDownSeconds * 1000);
		    
		    logger.info("wait finished...");
		    
		    totalSecondsWaited = totalSecondsWaited + countDownSeconds.intValue();
		    
		    manageAC(totalSecondsWaited);
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
	driver.navigate().to(flightsUrl);
    driver.switchTo().frame("iframe_canvas_fb_https");	    
  }
  
  private void moveToOverviewTab() {
	  driver.navigate().to(baseUrl);
	  driver.switchTo().frame("iframe_canvas_fb_https");
  }
  
  private int getAccountBalance() {
	  WebElement accountElement = driver.findElement(By.xpath("//*[@id='accountheader']"));
	  
	  return Integer.valueOf(normalizeString(accountElement.getText()));
  }
  
  private String normalizeString(String inputString) {
	  return inputString.replace("$", "").replace(",", "").replace("Pax", "").replace("Lbs", "").replace(",", "").trim();
  }

  @After
  public void tearDown() throws Exception {
    driver.quit();
  }

}
