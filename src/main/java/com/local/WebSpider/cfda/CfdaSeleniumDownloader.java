/**   
* @Title: CfdaSeleniumDownloader.java 
* @Package com.local.WebSpider.cfda 
* @Description: TODO(用一句话描述该文件做什么) 
* @author zhuyj   
* @date 2019-08-16 
*/
package com.local.WebSpider.cfda;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.local.utils.Constants;
import com.local.utils.LogUtil;
import com.local.utils.WebDriverPool;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.PlainText;

/** 
* @ClassName: CfdaSeleniumDownloader 
* @Description: TODO(这里用一句话描述这个类的作用) 
* @author: zhuyj
* @date: 2019-08-16 
*/
public class CfdaSeleniumDownloader implements Downloader, Closeable{
	
	private volatile WebDriverPool webDriverPool;

	private static Log logger = LogUtil.getTaskLog();

	private int sleepTime = 0;

	private int poolSize = 1;
	
	
	public CfdaSeleniumDownloader setSleepTime(int sleepTime) {
		this.sleepTime = sleepTime;
		return this;
	}

	@Override
	public Page download(Request request, Task task) {
		// TODO Auto-generated method stub
		checkInit();
		WebDriver webDriver;
		try {
			webDriver = webDriverPool.get();
		} catch (InterruptedException e) {
			logger.warn("interrupted", e);
			return null;
		}
		
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info("----------------------------------------------------------------");
		logger.info("start processing page:" + request.getUrl());
		
		request.putExtra("UUID", task.getUUID());
		String content = "";
		Page page = new Page();
		page.setUrl(new PlainText(request.getUrl()));
		page.setRequest(request);

		webDriver.get(request.getUrl());
		WebDriver.Options manage = webDriver.manage();
		Site site = task.getSite();
		if (site.getCookies() != null) {
			for (Map.Entry<String, String> cookieEntry : site.getCookies()
					.entrySet()) {
				Cookie cookie = new Cookie(cookieEntry.getKey(),
						cookieEntry.getValue());
				manage.addCookie(cookie);
			}
		}
		
		try{
			String url = request.getUrl();
			if(!url.matches(".*?/cfda$")){
				List<WebElement> trElements = webDriver.findElements(By.xpath("//div[@class='detail_list']//table/tbody/tr"));
				Map<String, String> map = new HashMap<String, String>();
				for(WebElement trElement : trElements) {
					if(trElement.findElements(By.xpath("td")).size() == 2) {
						WebElement fristTd = trElement.findElement(By.xpath("td[1]"));
						
						String value = trElement.findElement(By.xpath("td[2]")).getText();
						if(fristTd.getText().equals("批准日期")) {
							map.put("APPROVAL_DATE", value);
						}
						if(fristTd.getText().equals("有效期") || fristTd.getText().equals("有效期至") || fristTd.getText().equals("有效期截止日")) {
							map.put("EXPIRED_DATE", value);
						}
						if(fristTd.getText().equals("地址") || fristTd.getText().equals("注册人住所") || fristTd.getText().equals("生产厂地址（中文）")) {
							map.put("ENTERPRISE_ADDRESS", value);
						}
						if(fristTd.getText().equals("生产单位") || fristTd.getText().equals("注册人名称") || fristTd.getText().equals("生产厂商名称（中文）")) {
							map.put("ENTERPRISE", value);
						}
						if(fristTd.getText().equals("注册号") || fristTd.getText().equals("注册证编号")) {
							map.put("APPROVAL_NUM", value);
						}
						if(fristTd.getText().equals("产品名称") || fristTd.getText().equals("产品名称（中文）")) {
							map.put("PRODUCT_NAME", value);
						}
						String[] splitUrl = url.split("/");
						map.put("ES_ID", splitUrl[splitUrl.length - 1]);
					}
				}
				logger.info(map);
			}else {
	        	int menuSize = webDriver.findElements(By.xpath("//ul[@class='show_lits ylqx']//li")).size();
				for(int i = 1; i <= menuSize; i++) {
					WebElement menuElement = webDriver.findElement(By.xpath("//ul[@class='show_lits ylqx']//li[" + i + "]"));
					menuElement.click();
					
					WebDriverWait wait = new WebDriverWait(webDriver, 20);
					waitDone(wait);
					
					getInfo(webDriver, page);
					
					/*
					int totalNum = Integer.valueOf(webDriver.findElement(By.xpath("//b[@class='totalPage']")).getText()) ;
					for(int j = 2; j <= totalNum; j++) {
						WebElement nextPage = webDriver.findElement(By.xpath("//a[@class='laypage_next']"));
						nextPage.click();
						waitDone(wait);
						getInfo(webDriver, page);
					}
					*/
					
				}
	        }
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
		page.setRawText(content);
		webDriverPool.returnToPool(webDriver);
		return page;
	}
	
	private void waitDone(WebDriverWait wait) {
		wait.until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver driver) {
				// TODO Auto-generated method stub
				JavascriptExecutor driver_js= ((JavascriptExecutor) driver);
				return (boolean) driver_js.executeScript("jQuery.ajaxPrefilter(function( options ) { " +
						"	options.global = true;" + 
						"}); return jQuery.active == 0;");
			}					
		});
	}
	
	private void getInfo(WebDriver webDriver, Page page) {
		int liSize = webDriver.findElements(By.xpath("//ul[@id='data_content']//li/a")).size();
		for(int j = 1; j <= liSize; j++) {
			WebElement liElement = webDriver.findElement(By.xpath("//ul[@id='data_content']//li[" + j + "]/a"));
			
			String val = liElement.getAttribute("onclick");
			
			Pattern pattern_page = Pattern.compile(".*?\\((.*?)\\);$",Pattern.CASE_INSENSITIVE);
	        Matcher matcher_page = pattern_page.matcher(val);
	        if(matcher_page.find()) {
	        	page.addTargetRequest(Constants.DETAILPATH + matcher_page.group(1).replaceAll("'", "").replaceAll(",", "/"));
	        	logger.info(matcher_page.group(1).replaceAll("'", "").replaceAll(",", "/"));
	        }
		}
	}


	private void checkInit() {
		if (webDriverPool == null) {
			synchronized (this) {
				webDriverPool = new WebDriverPool(poolSize);
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		webDriverPool.closeAll();
	}

	@Override
	public void setThread(int threadNum) {
		// TODO Auto-generated method stub
		this.poolSize = threadNum;
	}

}
