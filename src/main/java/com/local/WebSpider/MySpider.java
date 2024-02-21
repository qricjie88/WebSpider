/**
 * 
 */
package com.local.WebSpider;

import com.local.utils.MongoManager;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * @desc : TODO
 * @author: Zhu
 * @date : 2017年9月21日
 */
public class MySpider extends Spider {

	/**
	 * @param pageProcessor
	 */
	public MySpider(PageProcessor pageProcessor) {
		super(pageProcessor);
	}

	@Override
	public void run() {
		MongoManager.init();
		super.run();
	}
	
	@Override
	public void close() {
		super.close();
		MongoManager.closeDB();
	}
}
