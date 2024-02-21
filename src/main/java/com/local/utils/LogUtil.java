/**
 * 
 */
package com.local.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @desc  : TODO
 * @author: Zhu
 * @date  : 2017年9月15日
 */
public class LogUtil {
	
	public static Log getTaskLog(){
		
		return LogFactory.getLog("commonLog");
		
	}
}
