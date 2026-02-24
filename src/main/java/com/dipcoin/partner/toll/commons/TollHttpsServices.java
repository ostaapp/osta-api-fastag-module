package com.dipcoin.partner.toll.commons;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.api.utils.PartnerRequestContext;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.model.TollTransaction;

import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.dipcoin.commons.LogFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.RequestConfig;
import com.google.common.net.HttpHeaders;

@Component("tollHttpsServices")
public class TollHttpsServices {

	public static String bankIin = StringUtils.EMPTY;

	@Autowired
	TollProperties tollProperties;

	@Autowired
	private TollDBService tollDBService;

	private static final Logger LOG = LogManager.getLogger(TollHttpsServices.class);

	private String CEDGE_PROXY_HOST = "cedge.proxy.in";

	private int CEDGE_PROXY_PORT = 3128;

	public String npciHealthCheckApi(String traceId) throws IOException {

		String responseIp = null;
		String[] healthCheckIps = this.tollProperties.getNetcHealthCheckIps();
		int healthCheckPort = this.tollProperties.getNetcHealthCheckPort();
		String healthCheckEndPoint = this.tollProperties.getNetcHealthCheckEndPoint();

		PartnerRequestContext partnerReqContext = PartnerRequestContext.instance();
		partnerReqContext.setTraceId(traceId);

		CloseableHttpResponse response = null;
		SSLContext sslContext = null;
		CloseableHttpClient httpClient = null;

		try {
			sslContext = SSLContexts.custom()
					.loadTrustMaterial(new AtomicReference<KeyStore>().get(), new TrustSelfSignedStrategy()).build();

			HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
			SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
					new String[] { "TLSv1.1", "TLSv1.2", "TLSv1.3" }, null, hostnameVerifier);

			// HttpHost proxy = new HttpHost(CEDGE_PROXY_HOST, CEDGE_PROXY_PORT);

			/*
			 * RequestConfig config =
			 * RequestConfig.custom().setProxy(proxy).setConnectTimeout(TollConstant.
			 * REQUEST_TIMEOUT)
			 * .setConnectionRequestTimeout(TollConstant.REQUEST_TIMEOUT).build();
			 */

			RequestConfig config = RequestConfig.custom().setConnectTimeout(TollConstant.REQUEST_TIMEOUT)
					.setConnectionRequestTimeout(TollConstant.REQUEST_TIMEOUT).build();

			HttpClientBuilder builder = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory)
					.setRetryHandler(new DefaultHttpRequestRetryHandler(5, false))
					.setConnectionTimeToLive(TollConstant.REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
					.setDefaultRequestConfig(config);

			httpClient = builder.build();

			for (String ipAddress : healthCheckIps) {

				String url = "https://" + ipAddress + ":" + healthCheckPort + healthCheckEndPoint;
				HttpGet request = new HttpGet(url);

				if (StringUtils.isNotEmpty(TollHttpsServices.bankIin)) {
					request.addHeader(HttpHeaders.X_FORWARDED_FOR, getXForwardedIp(TollHttpsServices.bankIin, traceId));
					request.addHeader(HttpHeaders.ORIGIN, getXForwardedIp(TollHttpsServices.bankIin, traceId));
					LOG.debug(LogFormatter.instance(traceId).message("Virtual IP Set In Header")
							.data(HttpHeaders.X_FORWARDED_FOR, getXForwardedIp(TollHttpsServices.bankIin, traceId))
							.data(HttpHeaders.ORIGIN, getXForwardedIp(TollHttpsServices.bankIin, traceId)).format());
				}

				request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5");
				request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
				request.addHeader(HttpHeaders.ACCEPT, "*/*");

				try {

					response = httpClient.execute(request);

					int status = response.getStatusLine().getStatusCode();

					LOG.debug(LogFormatter.instance(traceId).message("Response Status")
							.data("Response Status: ", status).data("Requested URL: ", url)
							.data("Response Time : ", String.valueOf(DateTime.now(DateTimeZone.UTC))).format());

					if (status == HttpStatus.SC_OK) {
//                        String responseContent = EntityUtils.toString(response.getEntity());

						LOG.debug(LogFormatter.instance(traceId).message("Got OK response from npci health check")
								.format());

						responseIp = ipAddress;
						return responseIp;
					} else {
						LOG.error(LogFormatter.instance(traceId).message("Failed to get 200 response for url : " + url)
								.format());

						continue;
					}
				} catch (Exception e) {
					LOG.error(LogFormatter.instance(traceId).message("Exception Caught While connecting Npci").format(),
							e);
				}
			}

		} catch (Exception e) {
			LOG.error(LogFormatter.instance(traceId).message("Exception Caught While connecting Npci").format(), e);
		} finally {

			try {
				if (response != null)
					response.close();
				if (httpClient != null)
					httpClient.close();
			} catch (Exception e) {
				LOG.error(LogFormatter.instance(traceId).message("Exception Caught While Closing Connection to Npci")
						.format(), e);
			}
		}

		return null;
	}

	public String getXForwardedIp(String iin, String traceId) {
		String virtualIp = StringUtils.EMPTY;
		String[] banks = tollProperties.getBankInfo().split(",");

		for (String bank : banks) {
			String[] bankInfo = bank.split("~");
			if (iin.equalsIgnoreCase(bankInfo[2])) {
				try {
					virtualIp = InetAddress.getByName(new URL(bankInfo[3]).getHost()).getHostAddress();
				} catch (Exception e) {

					LOG.error(
							LogFormatter.instance(traceId).message("Exception Caught While setting origin ip").format(),
							e);
				}
			}
		}
		return virtualIp;
	}

	public String send(String url, String traceId, ByteArrayOutputStream byteArrayOutputStream) throws IOException {

		PartnerRequestContext partnerReqContext = PartnerRequestContext.instance();
		partnerReqContext.setTraceId(traceId);

		LOG.debug(LogFormatter.instance(traceId).message("User requested URL :" + url.toString()).format());

		CloseableHttpResponse response = null;
		SSLContext sslContext = null;
		CloseableHttpClient httpClient = null;
		byte[] rawData = null;
		String data = null;
		TollTransaction tollTransaction = new TollTransaction();
		String reqString = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
		String reqSubString = reqString.substring(NumberUtils.INTEGER_ZERO, reqString.indexOf("<Signature"));

		String[] splitUrl = url.split("/");
		String endPoint = "/" + splitUrl[splitUrl.length - 1];

		String resSubString = null;

		try {
			sslContext = SSLContexts.custom()
					.loadTrustMaterial(new AtomicReference<KeyStore>().get(), new TrustSelfSignedStrategy()).build();

			HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
			SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
					new String[] { "TLSv1.1", "TLSv1.2", "TLSv1.3" }, null, hostnameVerifier);

			RequestConfig config = RequestConfig.custom().setConnectTimeout(TollConstant.REQUEST_TIMEOUT)
					.setConnectionRequestTimeout(TollConstant.REQUEST_TIMEOUT).build();

			HttpClientBuilder builder = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory)
					.setRetryHandler(new DefaultHttpRequestRetryHandler(5, false))
					.setConnectionTimeToLive(TollConstant.REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
					.setDefaultRequestConfig(config);

			httpClient = builder.build();
			HttpPost request = new HttpPost(url.toString());

			if (StringUtils.isNotEmpty(TollHttpsServices.bankIin)) {
				request.addHeader(HttpHeaders.X_FORWARDED_FOR, getXForwardedIp(TollHttpsServices.bankIin, traceId));
				request.addHeader(HttpHeaders.ORIGIN, getXForwardedIp(TollHttpsServices.bankIin, traceId));
				LOG.debug(LogFormatter.instance(traceId).message("Virtual IP Set In Header")
						.data(HttpHeaders.X_FORWARDED_FOR, getXForwardedIp(TollHttpsServices.bankIin, traceId))
						.data(HttpHeaders.ORIGIN, getXForwardedIp(TollHttpsServices.bankIin, traceId)).format());
			}

			request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5");
			request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
			request.addHeader(HttpHeaders.ACCEPT, "*/*");

			LOG.debug(LogFormatter.instance(traceId).message("request Entity: " + reqSubString).format());
			// LOG.debug(LogFormatter.instance(traceId).message("request Entity with
			// signature: " + reqString).format());

			request.setEntity(new ByteArrayEntity(byteArrayOutputStream.toByteArray()));

			response = httpClient.execute(request);
			LOG.debug(LogFormatter.instance(traceId).message(" response " + response).format());

			int status = response.getStatusLine().getStatusCode();

			LOG.debug(LogFormatter.instance(traceId).message("Response Status").data("Response Status: ", status)
					.data("Requested URL: ", url.toString())
					.data("Response Time : ", String.valueOf(DateTime.now(DateTimeZone.UTC))).format());

			rawData = EntityUtils.toByteArray(response.getEntity());
			LOG.debug(LogFormatter.instance(traceId).message(" rawData " + rawData).format());

			data = new String(rawData, StandardCharsets.UTF_8);
			LOG.debug(LogFormatter.instance(traceId).message(" data " + data).format());

			if (status >= HttpStatus.SC_BAD_REQUEST) {
				String desc = "Failed to get 200 or 202 response : " + url.toString();
				LOG.error(LogFormatter.instance(traceId).message(desc).format());

				try {

					// if (!tollProperties.getSyncTimeRequestUrl().equalsIgnoreCase(url)) {
					if (!tollProperties.getSyncTimeRequestUrl().equalsIgnoreCase(endPoint)) {
						resSubString = data != null && data.length() > NumberUtils.INTEGER_ZERO
								&& data.contains("<Signature")
										? data.substring(NumberUtils.INTEGER_ZERO, data.indexOf("<Signature"))
										: data;
						LOG.debug(LogFormatter.instance(traceId).message("Response data " + resSubString).format());

						if (reqSubString.length() > 3000) {
							tollTransaction.setRawRequest(reqSubString.substring(NumberUtils.INTEGER_ZERO, 2999));
						} else {
							tollTransaction.setRawRequest(reqSubString);
						}
						if (resSubString != null && resSubString.length() > 3000) {
							tollTransaction.setRawResponse(resSubString.substring(NumberUtils.INTEGER_ZERO, 2999));
						} else {
							tollTransaction.setRawResponse(resSubString);
						}
						tollTransaction.setRequestTime(String.valueOf(System.currentTimeMillis()));
						tollTransaction.setUrl(url);

						if (this.tollDBService.save(tollTransaction) == null) {
							LOG.error(LogFormatter.instance(traceId).message("Toll txn not updated").format());
						}
					}
				} catch (Exception e) {
					LOG.error(LogFormatter.instance(traceId).message("tolltransaction update exception").format(), e);
				}

				return null;
			}

		} catch (Exception e) {
			LOG.error(LogFormatter.instance(traceId).message("Exception Caught While connecting Npci").format(), e);
			data = null;
		} finally {

			try {
				if (response != null)
					response.close();
				if (httpClient != null)
					httpClient.close();
			} catch (Exception e) {
				LOG.error(LogFormatter.instance(traceId).message("Exception Caught While Closing Connection to Npci")
						.format(), e);
			}
		}

		try {

			// if (!tollProperties.getSyncTimeRequestUrl().equalsIgnoreCase(url)) {
			if (!tollProperties.getSyncTimeRequestUrl().equalsIgnoreCase(endPoint)) {
				resSubString = data != null && data.length() > NumberUtils.INTEGER_ZERO
						? data.substring(NumberUtils.INTEGER_ZERO, data.indexOf("<Signature"))
						: null;
				LOG.debug(LogFormatter.instance(traceId).message("Response data " + resSubString).format());
				if (reqSubString.length() > 3000) {
					tollTransaction.setRawRequest(reqSubString.substring(NumberUtils.INTEGER_ZERO, 2999));
				} else {
					tollTransaction.setRawRequest(reqSubString);
				}
				if (resSubString != null && resSubString.length() > 3000) {
					tollTransaction.setRawResponse(resSubString.substring(NumberUtils.INTEGER_ZERO, 2999));
				} else {
					tollTransaction.setRawResponse(resSubString);
				}
				tollTransaction.setRequestTime(String.valueOf(System.currentTimeMillis()));
				tollTransaction.setUrl(url);

				if (this.tollDBService.save(tollTransaction) == null) {
					LOG.error(LogFormatter.instance(traceId).message("Toll txn not updated").format());
				}
			}
		} catch (Exception e) {
			LOG.error(LogFormatter.instance(traceId).message("tolltransaction update exception").format(), e);
		}

		return data;
	}

}
