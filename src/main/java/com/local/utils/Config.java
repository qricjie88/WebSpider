/**
 * 
 */
package com.local.utils;

import java.util.Properties;

import org.apache.commons.logging.Log;

/**
 * @desc  : TODO
 * @author: Zhu
 * @date  : 2017年9月15日
 */
public class Config {
	private static Log logger = LogUtil.getTaskLog();
	
	private String proxyFile = "proxy.properties";
	private String dbFile = "db.properties";
	private String driverFile = "phantomjs/phantomjs.properties";
	private String configFile = "config.properties";
	
	public void init() {
		loadConfig();
	}
	
	public void loadConfig() {
		logger.info("开始加载配置文件");
		
		Properties proxyConfig = new Properties();
		Properties dbConfig = new Properties();
		Properties driverConfig = new Properties();
		Properties configConfig = new Properties();
		
		try {
			proxyConfig.load(Config.class.getClassLoader().getResourceAsStream(proxyFile));
			Constants.PROXY_HOST = proxyConfig.getProperty("proxy_host");
			Constants.PROXY_PORT = Integer.valueOf(proxyConfig.getProperty("proxy_port"));
			Constants.PROXY_TYPE = proxyConfig.getProperty("proxy_type");
			logger.info(proxyFile + "加载完成");
		} catch (Exception e) {
			logger.info(proxyFile + "加载失败");
		}
		
		try {
			dbConfig.load(Config.class.getClassLoader().getResourceAsStream(dbFile));
			Constants.MONGO_DBNAME = dbConfig.getProperty("dbName");
			Constants.MONGO_HOST = dbConfig.getProperty("host");
			Constants.MONGO_PORT = Integer.valueOf(dbConfig.getProperty("port"));
			Constants.MONGO_PWD = dbConfig.getProperty("pwd");
			Constants.MONGO_USR = dbConfig.getProperty("user");
			Constants.MONGO_TIMEOUT = Integer.valueOf(dbConfig.getProperty("timeOut"));
			logger.info(dbFile + "加载完成");
		} catch (Exception e) {
			logger.info(dbFile + "加载失败");
		}

		try {
			driverConfig.load(Config.class.getClassLoader().getResourceAsStream(driverFile));
			Constants.DRIVER_PATH = driverConfig.getProperty("exec_path");
			Constants.DRIVER_LOGFILE = driverConfig.getProperty("driver_logFile");
			Constants.DRIVER_LOGLEVEL = driverConfig.getProperty("driver_loglevel");
			Constants.DRIVER_NAME = driverConfig.getProperty("driver");
			logger.info(driverFile + "加载完成");
		} catch (Exception e) {
			logger.info(driverFile + "加载失败");
		}
		
		try {
			configConfig.load(Config.class.getClassLoader().getResourceAsStream(configFile));
			Constants.DEEPPAGESIZE = Integer.valueOf(configConfig.getProperty("deepPageSize"));
			Constants.RETRYTIMES = Integer.valueOf(configConfig.getProperty("retryTimes"));
			Constants.SLEEPTIME = Integer.valueOf(configConfig.getProperty("sleepTime"));
			Constants.THREADNUM = Integer.valueOf(configConfig.getProperty("threadNum"));
			Constants.STARURL = configConfig.getProperty("starUrl");
			Constants.SAVEPATH = configConfig.getProperty("savePath");
			Constants.DETAILPATH = configConfig.getProperty("detailPath");
			logger.info(configFile + "加载完成");
		} catch (Exception e) {
			e.printStackTrace();
			logger.info(configFile + "加载失败");
		}
	}
}
