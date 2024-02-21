/**   
* @Title: CfdaProcessor.java 
* @Package com.local.WebSpider.cfda 
* @Description: TODO(用一句话描述该文件做什么) 
* @author zhuyj   
* @date 2019-08-16 
*/
package com.local.WebSpider.cfda;

import com.local.utils.Constants;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * @ClassName: CfdaProcessor
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author: zhuyj
 * @date: 2019-08-16
 */
public class CfdaProcessor implements PageProcessor {
	private Site site = Site.me();

	/**
	 * @Title: process
	 * @Description: TODO(这里用一句话描述这个方法的作用)
	 * @param page
	 * @see us.codecraft.webmagic.processor.PageProcessor#process(us.codecraft.webmagic.Page)
	 */
	@Override
	public void process(Page page) {
		// TODO Auto-generated method stub
		System.out.println("test");
	}

	/**
	 * @Title: getSite
	 * @Description: TODO(这里用一句话描述这个方法的作用)
	 * @return
	 * @see us.codecraft.webmagic.processor.PageProcessor#getSite()
	 */
	@Override
	public Site getSite() {
		site.setRetryTimes(Constants.RETRYTIMES).setSleepTime(Constants.SLEEPTIME);

		return site;
	}
}
