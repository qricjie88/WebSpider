package com.local.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

public class MongoManager {
	private static Log logger = LogUtil.getTaskLog();
	
	private static MongoClient mongo = null;
	
	private MongoManager() {
		
	}
	
	public static MongoDatabase getDB(){
		if (mongo == null) {
			init();
		}
		return mongo.getDatabase(Constants.MONGO_DBNAME);
	}
	
	public static void closeDB(){
		if (mongo != null) {
			mongo.close();
		}
	}
	
	public static void init(){
		MongoClientOptions.Builder builder =  new MongoClientOptions.Builder();
		
		MongoClientOptions options = null;
		
		List<MongoCredential> credentialsList = new ArrayList<MongoCredential>();
		try {
			
			ServerAddress address = new ServerAddress(Constants.MONGO_HOST, Constants.MONGO_PORT);
	        MongoCredential credentials= MongoCredential.createCredential(Constants.MONGO_USR,Constants.MONGO_DBNAME,Constants.MONGO_PWD.toCharArray());
	        credentialsList.add(credentials);  
	        
	        //超时时间
			builder.connectTimeout(Constants.MONGO_TIMEOUT);
			
			options = builder.build();
			
	        mongo = new MongoClient(address,credentialsList,options);
	        logger.info("连接至：" + Constants.MONGO_HOST + ":" + Constants.MONGO_PORT + "@" + Constants.MONGO_DBNAME);
		} catch (Exception e) {
			logger.error("数据库连接错误" + e.getMessage());
		}
	}
	
}