/**
 * 
 */
package com.local.WebSpider;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.local.WebSpider.cfda.CfdaProcessor;
import com.local.WebSpider.cfda.CfdaSeleniumDownloader;
import com.local.utils.Constants;


/**
 * @desc  : TODO
 * @author: Zhu
 * @date  : 2017年9月15日
 */
public class StartSpider {
	
	@SuppressWarnings({ "unused", "resource" })
	public static void main(String args[]) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath*:applicationContext.xml");
		/*MySpider.create(new ZhiHuProcessor())
		.addUrl(Constants.STARURL)
		.addPipeline(new MyFilePipeline(Constants.SAVEPATH))
		.setDownloader(new ZhiHuSeleniumDownloader()
							.setPageSize(Integer.valueOf(Constants.DEEPPAGESIZE))
							.setSleepTime(Integer.valueOf(Constants.SLEEPTIME)))
		.thread(Integer.valueOf(Constants.THREADNUM))
		.run();*/
		
		MySpider.create(new CfdaProcessor())
		.addUrl(Constants.STARURL)
		.addPipeline(new MyFilePipeline(Constants.SAVEPATH))
		.setDownloader(new CfdaSeleniumDownloader()
							.setSleepTime(Integer.valueOf(Constants.SLEEPTIME)))
		.thread(Integer.valueOf(Constants.THREADNUM))
		.run();
	}
}
