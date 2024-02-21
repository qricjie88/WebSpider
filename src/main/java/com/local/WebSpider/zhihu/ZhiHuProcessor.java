/**
 * 
 */
package com.local.WebSpider.zhihu;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;

import com.local.utils.Constants;
import com.local.utils.MongoManager;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;


/**
 * @desc  : TODO
 * @author: Zhu
 * @date  : 2017年9月26日
 */
public class ZhiHuProcessor implements PageProcessor{

	private Site site = Site.me();
	
	@Override
	public void process(Page page) {
		// TODO Auto-generated method stub
		try {
    		String authorUrl = java.net.URLDecoder.decode(page.getUrl().toString(),"UTF-8");
    		String PATH_SEPERATOR = "/";
    		String UUID = String.valueOf(page.getRequest().getExtra("UUID"));
			
			//图片处理--直接下载
			Pattern pattern_attach = Pattern.compile(".*?\\/([^\\/]*?)\\.(jpg|png|jpeg|gif)$",Pattern.CASE_INSENSITIVE);
            Matcher matcher_attach = pattern_attach.matcher(authorUrl);
            if(matcher_attach.find()){
            	String location = PATH_SEPERATOR + UUID + PATH_SEPERATOR + authorUrl.substring(authorUrl.indexOf(UUID) + UUID.length(), authorUrl.lastIndexOf("/")) + PATH_SEPERATOR;
            	page.putField("fileUrl", location);
            	page.putField("fileName", matcher_attach.group(1));
            	page.putField("fileType", matcher_attach.group(2));
            	return;
            }
            
            MongoDatabase mongo = MongoManager.getDB();
    		MongoCollection<Document> collection = mongo.getCollection("spider");
    		//查询已存在且已下载列表页数据
            Document boL = new Document();
    		boL.put("columnUrl", authorUrl);
    		boL.put("isScan", 1);
    		FindIterable<Document> findIterableL = collection.find(boL);
    		MongoCursor<Document> mongoCursorL = findIterableL.iterator();
    		
    		List<String> results = new ArrayList<String>();
    		while(mongoCursorL.hasNext()){
    			results.add(mongoCursorL.next().getString("fileUrl"));
    		}
    		
    		//查询已存在且未下载列表页数据
    		Document boC = new Document();
    		boC.put("columnUrl", authorUrl);
    		boC.put("isScan", 0);
    		FindIterable<Document> findIterableC = collection.find(boC);
    		MongoCursor<Document> mongoCursorC = findIterableC.iterator();
    		
    		List<String> resultsC = new ArrayList<String>();
    		while(mongoCursorC.hasNext()){
    			resultsC.add(mongoCursorC.next().getString("fileUrl"));
    		}
    		List<Document> contents = new ArrayList<Document>();
            
            //内容页处理--连接查找和入库
            page.putField("content", page.getRawText());
            Pattern pattern_page = Pattern.compile(".*?\\/([^\\/]*?)(\\/collection\\/\\d+.*?)$",Pattern.CASE_INSENSITIVE);
            Matcher matcher_page = pattern_page.matcher(authorUrl);
            if(matcher_page.find()){
            	//2级连接不再处理
            	if(page.getRequest().getExtra("level") != null && 
            			"2".equals(String.valueOf(page.getRequest().getExtra("level")))){
            		return;
            	}
            	
            	//并将下一页添加到待处理流程中
            	if(page.getRequest().getExtra("NextPage") != null){
            		if(!authorUrl.matches("(?i).*?page\\=(\\d+)$")){
            			authorUrl += "?page=1";
            		}
            		String urlNext = authorUrl.replaceAll("\\?page\\=(\\d+)", String.valueOf(page.getRequest().getExtra("NextPage")));
            		page.addTargetRequest(new Request(urlNext));
            	}
            	
            	//二级链接处理
            	List<String> getUrls = page.getHtml().xpath("//img/@src").all();
        		for(String str : getUrls){
        			str = java.net.URLDecoder.decode(str,"UTF-8");
        			if(results.contains(str) || !str.matches(".*?\\/([^\\/]*?)\\.(jpg|png|jpeg|gif)$")) continue;
        			
        			Request request = new Request(str);
        			request.putExtra("level", "2");
        			page.addTargetRequest(request);
        			
        			//未下载的不需要再次插入数据库
        			if(resultsC.contains(str)) continue;
        			
                	Document doc = new Document();
                	doc.append("columnUrl",authorUrl);
                	String picUrl = PATH_SEPERATOR + UUID + PATH_SEPERATOR + str.substring(str.indexOf(UUID) + UUID.length());
                    doc.append("fileUrl", picUrl);
                    doc.append("fileName", str.substring(str.lastIndexOf("/") + 1));
                	SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                	doc.append("saveTime",sdf.format(new Date()));
                	doc.append("isScan",0);
                	doc.append("isNew",1);
                	doc.append("isChild", 1);
                	doc.append("isValid", 1);
                	contents.add(doc);
        		}
        		if(!contents.isEmpty()){
                	collection.insertMany(contents);
                }
            }
            
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Site getSite() {
		// TODO Auto-generated method stub
		site.setRetryTimes(Constants.RETRYTIMES)
			.setSleepTime(Constants.SLEEPTIME);
		
		return site;
	}

}
