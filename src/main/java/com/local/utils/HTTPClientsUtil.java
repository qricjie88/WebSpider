package com.local.utils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
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

public class HTTPClientsUtil {

	
	public static void doGet(String url ,String type, List<NameValuePair> paras) {
		CloseableHttpClient httpclient = null;
		CloseableHttpResponse httpResponse = null;
		int statusCode = 0;
		try {
			SSLContext sslcontext = createIgnoreVerifySSL();
			RequestBuilder requestBuilder = null;
			if("get".equalsIgnoreCase(type)) {
				requestBuilder = RequestBuilder.get().setUri(java.net.URLDecoder.decode(url, "UTF-8"));
			}else {
				requestBuilder = RequestBuilder.post().setUri(java.net.URLDecoder.decode(url, "UTF-8"));
				NameValuePair[] NParas = new NameValuePair[paras.size()];
				requestBuilder.addParameters(paras.toArray(NParas));
			}
			
			RequestConfig defaultRequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT)
					.setExpectContinueEnabled(true)
					.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
					.setConnectionRequestTimeout(3000)
					.setSocketTimeout(3000)
					.setConnectTimeout(3000)
					.build();
			RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig).build();
			requestBuilder.setConfig(requestConfig);
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
					.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", new SSLConnectionSocketFactory(sslcontext)).build();
			PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
					socketFactoryRegistry);
			HttpClients.custom().setConnectionManager(connManager);
			httpclient = HttpClients.custom().setConnectionManager(connManager).build();

			HttpUriRequest httpUriRequest = requestBuilder.build();
			httpResponse = httpclient.execute(httpUriRequest);
			statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_OK) {
				HttpEntity entity = httpResponse.getEntity();
				System.out.println(EntityUtils.toString(entity));
			}
		} catch (Exception e) {
			// TODO: handle exception
		}finally {
			
		}
		
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
	
	public static void main(String[] args) {
		//HTTPClientsUtil.doGet("https://edu.sse.com.cn/edumanage/bespeak/queryBespeakTime.do?st=68003");
	}

}
