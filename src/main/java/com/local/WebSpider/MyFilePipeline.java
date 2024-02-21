/**
 * 
 */
package com.local.WebSpider;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.bson.Document;

import com.google.common.collect.Sets;
import com.local.utils.Constants;
import com.local.utils.LogUtil;
import com.local.utils.MongoManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;
import us.codecraft.webmagic.utils.HttpConstant;

/**
 * @desc : TODO
 * @author: Zhu
 * @date : 2017年9月26日
 */
public class MyFilePipeline extends FilePersistentBase implements Pipeline {
	private static Log logger = LogUtil.getTaskLog();

	/**
	 * create a FilePipeline with default path"/data/webmagic/"
	 */
	public MyFilePipeline() {
		setPath("/data/webmagic/");
	}

	public MyFilePipeline(String path) {
		setPath(path);
	}

	@Override
	public void process(ResultItems resultItems, Task task) {
		// TODO Auto-generated method stub
		Request request = resultItems.getRequest();
		String urlStr = request.getUrl();
		String path = this.path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR
				+ urlStr.substring(urlStr.indexOf(task.getUUID()) + task.getUUID().length(), urlStr.lastIndexOf("/"))
				+ PATH_SEPERATOR;
		String fileName = resultItems.get("fileName");
		String fileType = resultItems.get("fileType");
		String fileUrl = resultItems.get("fileUrl");

		Site site = null;
		if (task != null) {
			site = task.getSite();
		}
		Set<Integer> acceptStatCode;
		Map<String, String> headers = null;
		if (site != null) {
			acceptStatCode = site.getAcceptStatCode();
			headers = site.getHeaders();
		} else {
			acceptStatCode = Sets.newHashSet(200);
		}

		CloseableHttpClient httpclient = null;
		CloseableHttpResponse httpResponse = null;

		InputStream in = null;
		int statusCode = 0;
		if (fileName != null) {
			try {
				// 附件直接下载、页面用之前下载的内容
				if (fileType.matches("(?i)jpg|png|jpeg|gif")) {
					SSLContext sslcontext = createIgnoreVerifySSL();
					Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
							.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE)
							.register("https", new SSLConnectionSocketFactory(sslcontext)).build();
					PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
							socketFactoryRegistry);
					HttpClients.custom().setConnectionManager(connManager);
					httpclient = HttpClients.custom().setConnectionManager(connManager).build();

					HttpUriRequest httpUriRequest = getHttpRequestBuilder(request, site, headers).build();
					httpResponse = httpclient.execute(httpUriRequest);
					statusCode = httpResponse.getStatusLine().getStatusCode();
					if (statusAccept(acceptStatCode, statusCode)) {
						HttpEntity entity = httpResponse.getEntity();
						in = entity.getContent();
					} else {
						logger.warn("code error " + statusCode + "\t" + request.getUrl());
					}
				} else {
					in = new ByteArrayInputStream(((String) resultItems.get("content")).getBytes());
				}

				FileOutputStream fout = new FileOutputStream(getFile(path + fileName + "." + fileType));
				int l = -1;
				byte[] tmp = new byte[1024];
				while ((l = in.read(tmp)) != -1) {
					fout.write(tmp, 0, l);
				}
				fout.flush();
				fout.close();

				// 下载完成后修改状态
				MongoDatabase mongo = MongoManager.getDB();
				MongoCollection<Document> collection = mongo.getCollection("spider");
				// 查询已存在且已下载列表页数据
				Document boL = new Document();
				boL.put("fileUrl", fileUrl + fileName + "." + fileType);
				boL.put("isScan", 0);

				Document bo = new Document();
				bo.put("isScan", 1);

				collection.updateOne(boL, new Document().append("$set", bo));

				logger.info("Processing complete");
			} catch (Exception e) {
				logger.warn("write file error", e);
			} finally {
				try {
					in.close();
					if (httpResponse != null) {
						// ensure the connection is released back to pool
						EntityUtils.consume(httpResponse.getEntity());
						httpclient.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				logger.info("----------------------------------------------------------------");
			}
		}
	}

	protected boolean statusAccept(Set<Integer> acceptStatCode, int statusCode) {
		return acceptStatCode.contains(statusCode);
	}

	protected RequestBuilder getHttpRequestBuilder(Request request, Site site, Map<String, String> headers)
			throws UnsupportedEncodingException {
		RequestBuilder requestBuilder = selectRequestMethod(request)
				.setUri(java.net.URLDecoder.decode(request.getUrl().toString(), "UTF-8"));
		if (headers != null) {
			for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
				requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
			}
		}
		RequestConfig defaultRequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT)
				.setExpectContinueEnabled(true)
				.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
				.setConnectionRequestTimeout(site.getTimeOut()).setSocketTimeout(site.getTimeOut())
				.setConnectTimeout(site.getTimeOut()).build();
		RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig).build();
		if ("on".equalsIgnoreCase(Constants.PROXY_TYPE)) {
			HttpHost host = new HttpHost(Constants.PROXY_HOST, Constants.PROXY_PORT);
			requestConfig = RequestConfig.copy(defaultRequestConfig).setProxy(host)
					.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).build();
		}
		requestBuilder.setConfig(requestConfig);
		return requestBuilder;
	}

	protected RequestBuilder selectRequestMethod(Request request) {
		String method = request.getMethod();
		if (method == null || method.equalsIgnoreCase(HttpConstant.Method.GET)) {
			// default get
			return RequestBuilder.get();
		} else if (method.equalsIgnoreCase(HttpConstant.Method.POST)) {
			RequestBuilder requestBuilder = RequestBuilder.post();
			NameValuePair[] nameValuePair = (NameValuePair[]) request.getExtra("nameValuePair");
			if (nameValuePair.length > 0) {
				requestBuilder.addParameters(nameValuePair);
			}
			return requestBuilder;
		} else if (method.equalsIgnoreCase(HttpConstant.Method.HEAD)) {
			return RequestBuilder.head();
		} else if (method.equalsIgnoreCase(HttpConstant.Method.PUT)) {
			return RequestBuilder.put();
		} else if (method.equalsIgnoreCase(HttpConstant.Method.DELETE)) {
			return RequestBuilder.delete();
		} else if (method.equalsIgnoreCase(HttpConstant.Method.TRACE)) {
			return RequestBuilder.trace();
		}
		throw new IllegalArgumentException("Illegal HTTP Method " + method);
	}

	public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sc = SSLContext.getInstance("SSLv3");

		// 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
		TrustManager trustManager = new X509TrustManager() {
			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		sc.init(null, new TrustManager[] { trustManager }, null);
		return sc;
	}

}
