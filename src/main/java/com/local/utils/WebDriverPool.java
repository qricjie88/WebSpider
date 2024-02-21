/**
 * 
 */
package com.local.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.openqa.selenium.Proxy.ProxyType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.phantomjs.PhantomJSDriverService.Builder;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * @desc  : 重写webmagic的WebDriverPool，使用phantomjs和selenium组合
 * @author: Zhu
 * @date  : 2017年9月26日
 */
public class WebDriverPool {
	private static Log logger = LogUtil.getTaskLog();
	
	private final static int DEFAULT_CAPACITY = 5;

	private final int capacity;

	private final static int STAT_RUNNING = 1;

	private final static int STAT_CLODED = 2;

	private AtomicInteger stat = new AtomicInteger(STAT_RUNNING);

	private WebDriver mDriver = null;

	@SuppressWarnings("unused")
	private boolean mAutoQuitDriver = true;

	private static final String DRIVER_PHANTOMJS = "phantomjs";

	protected static DesiredCapabilities sCaps;
	
	public void configure() throws IOException{
		String driver = Constants.DRIVER_NAME;
		String isUseProxy = Constants.PROXY_TYPE;
		
		PhantomJSDriverService pjsds = null;
		
		if (driver.equals(DRIVER_PHANTOMJS)) {
			sCaps = new DesiredCapabilities();
			sCaps.setJavascriptEnabled(true);
			sCaps.setCapability("takesScreenshot", false);
			
			String[] phantomArgs = new String[]{"--webdriver-loglevel=" + Constants.DRIVER_LOGLEVEL
												,"--web-security=false"
												,"--ssl-protocol=any"
												,"--ignore-ssl-errors=true"};
			
			File logfile = new File(Constants.DRIVER_LOGFILE);
			
			Builder builder = new PhantomJSDriverService.Builder();
			
			builder.usingCommandLineArguments(phantomArgs)
					.withLogFile(logfile);
			
			// "phantomjs_exec_path"
			if (Constants.DRIVER_PATH != null) {
				String driverPath = WebDriverPool.class.getClassLoader().getResource(Constants.DRIVER_PATH).getPath();
				builder.usingPhantomJSExecutable(new File(driverPath));
			} else {
				throw new IOException(
						String.format(
								"Property '%s' not set!",
								PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY));
			}
			
			sCaps.setCapability("acceptSslCerts",true);
			
			if(isUseProxy.equals("on")){
				String proxyStr = Constants.PROXY_HOST + ":" + Constants.PROXY_PORT;
				org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
				proxy.setSslProxy(proxyStr)
					.setHttpProxy(proxyStr);
				proxy.setProxyType(ProxyType.MANUAL);
				builder.withProxy(proxy);
			}
			pjsds = builder.build();
		}
		
		if (isUrl(driver)) {
			sCaps.setBrowserName("phantomjs");
			mDriver = new RemoteWebDriver(new URL(driver), sCaps);
		} else if (driver.equals(DRIVER_PHANTOMJS)) {
			mDriver = new PhantomJSDriver(pjsds, sCaps);
		}
		
	}
	
	private boolean isUrl(String urlString) {
		try {
			new URL(urlString);
			return true;
		} catch (MalformedURLException mue) {
			return false;
		}
	}
	
	/**
	 * store webDrivers created
	 */
	private List<WebDriver> webDriverList = Collections
			.synchronizedList(new ArrayList<WebDriver>());

	/**
	 * store webDrivers available
	 */
	private BlockingDeque<WebDriver> innerQueue = new LinkedBlockingDeque<WebDriver>();

	public WebDriverPool(int capacity) {
		this.capacity = capacity;
	}

	public WebDriverPool() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public WebDriver get() throws InterruptedException {
		checkRunning();
		WebDriver poll = innerQueue.poll();
		if (poll != null) {
			return poll;
		}
		if (webDriverList.size() < capacity) {
			synchronized (webDriverList) {
				if (webDriverList.size() < capacity) {

					// add new WebDriver instance into pool
					try {
						configure();
						innerQueue.add(mDriver);
						webDriverList.add(mDriver);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
		return innerQueue.take();
	}
	
	public void returnToPool(WebDriver webDriver) {
		checkRunning();
		innerQueue.add(webDriver);
	}

	protected void checkRunning() {
		if (!stat.compareAndSet(STAT_RUNNING, STAT_RUNNING)) {
			throw new IllegalStateException("Already closed!");
		}
	}

	public void closeAll() {
		boolean b = stat.compareAndSet(STAT_RUNNING, STAT_CLODED);
		if (!b) {
			throw new IllegalStateException("Already closed!");
		}
		for (WebDriver webDriver : webDriverList) {
			logger.info("Quit webDriver" + webDriver);
			webDriver.quit();
			webDriver = null;
		}
	}
}
