package com.dipcoin.partner.recharge.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONObject;
import org.json.XML;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;

import com.dipcoin.api.utils.RechargeConfigurations;
import com.dipcoin.api.utils.RechargeConstants;
import com.dipcoin.api.utils.RechargeConstants.RechargeErrorResponse;
import com.dipcoin.api.utils.RechargeConstants.RechargeResponseStatus;
import com.dipcoin.api.utils.RechargeConstants.SupportedHttpMethod;
import com.dipcoin.api.utils.RechargeServiceException;
import com.dipcoin.api.utils.RechargeUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.recharge.comm.BillRequest;
import com.dipcoin.partner.recharge.comm.BillResponse;
import com.dipcoin.partner.recharge.comm.PartnerResponse;
import com.dipcoin.partner.recharge.comm.RechargePlanResponse;
import com.dipcoin.partner.recharge.comm.RechargeRequest;
import com.dipcoin.partner.recharge.comm.RechargeResponse;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.dipcoin.partner.utils.PartnerTrustStoreManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;

public class RechargeHTTPSClient extends RechargeClient {

  private static final Logger LOG = LogManager.getLogger(RechargeHTTPSClient.class);
  private SupportedHttpMethod httpMethod = SupportedHttpMethod.POST;


  @Autowired
  private PartnerTrustStoreManager partnerTrustStore;
  
  @Autowired
  private RechargeConfigurations rechargeConfigurations;

  private String uri;

  public RechargeHTTPSClient(String host) {
    super(RechargeClient.Protocol.HTTPS, host);
  }
  
  private boolean fetchPlanRequest;

  public boolean isFetchPlanRequest() {
    return fetchPlanRequest;
  }

  public void setFetchPlanRequest(boolean fetchPlanRequest) {
    this.fetchPlanRequest = fetchPlanRequest;
  }
  
  private boolean httpGetMethod;
  
  public boolean isHttpGetMethod() {
    return httpGetMethod;
  }

  public void setHttpGetMethod(boolean httpGetMethod) {
    this.httpGetMethod = httpGetMethod;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  /*
   * Method to execute the HTTPS request.
   */
  @Override
  protected void executeRequest(PartnerRequestContext requestContext, RechargeRequest rcRequest,
      RechargeResponse rcResponse) throws RechargeServiceException, IOException {

    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Request parameters " + rcRequest.toString()).format());

    StringBuffer uriBuf = new StringBuffer("https://");
    uriBuf.append(this.getHost());

    String uri =
        RechargeUtils.getRequestURI(requestContext, getProtocol(), this.uri, getPartnerRequest());
    if (uri != null)
      uriBuf.append("/").append(uri);

    RequestConfig config =
        RequestConfig.custom().setConnectTimeout(rechargeConfigurations.getRequestTimeOut())
            .setConnectionRequestTimeout(rechargeConfigurations.getRequestTimeOut()).build();

    int retries = 0;
    CloseableHttpClient httpClient = null;
    CloseableHttpResponse httpResponse = null;
    
    boolean isResponseError = true;
    
    try {
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Request Time : " + String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()))
          .format());

      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("User requested URL :" + uriBuf.toString()).format());

      HttpClientBuilder builder = HttpClients.custom()
          .setSSLSocketFactory(partnerTrustStore.getSSLConnectionSocketFactory(requestContext,
              rcRequest.getMerchantReferenceId()))
          .disableAutomaticRetries()
          .setConnectionTimeToLive(rechargeConfigurations.getRequestTimeOut(), TimeUnit.MILLISECONDS)
          .setDefaultRequestConfig(config);

      LOG.debug(LogFormatter.instance(requestContext.getTraceId()).message("Proxy settings")
          .data("Host", getProxy().getHost()).data("Port", getProxy().getPort())
          .data("Enabled", getProxy().isEnabled()).format());

      if (getProxy().isEnabled()) {
        builder.setProxy(new HttpHost(getProxy().getHost(), getProxy().getPort()));
      }

      httpClient = builder.build();

      HttpUriRequest request = null;
      if (SupportedHttpMethod.GET == httpMethod) {
        request = new HttpGet(uriBuf.toString());
      } else if (SupportedHttpMethod.POST == httpMethod) {
        request = new HttpPost(uriBuf.toString());

        byte[] postData = this.getPartnerRequest().getPayload(requestContext, getProtocol(),
            rcRequest.deepCopy(), retries);

        ((HttpPost) request).setEntity(new ByteArrayEntity(postData));
      }

      if (request == null) {
        throw new Exception("Unsupported Request Type");
      }

      request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5");
      request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
      request.addHeader(RechargeConstants.HEADER_TRACEID, requestContext.getTraceId());
      
      // process response
      httpResponse = httpClient.execute(request);

      isResponseError = false;
      
      // @TODO - Need to handle REDIRECTS explicitly if not
      int status = httpResponse.getStatusLine().getStatusCode();
      
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
              .message("Response Status Code: "+status).format());

      byte[] rawData = EntityUtils.toByteArray(httpResponse.getEntity());
      String payload = new String(rawData, StandardCharsets.UTF_8);

      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Response data from server " + payload).format());
      this.getPartnerResponse().processResults(requestContext, getProtocol(), rcRequest, rawData,
          rcResponse);

      if (status != HttpsURLConnection.HTTP_OK) {
        rcResponse.setErrorMsg(payload);
      } else {
        rcResponse.setRawData(payload);
      }
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Mapped Response data to Recharge Response : " + rcResponse).format());

    } catch (Exception e) {
      
      if(isResponseError) {
    	 rcResponse.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    	 rcResponse.setResponseError(RechargeErrorResponse.YES.value());
      }
      
      String desc = "[" + retries + "]: Failed to process request for URL : " + uriBuf.toString();
      LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format(), e);

    } finally {
      httpResponse.close();
      httpClient.close();

    }
  }

  @Override
  protected void executeBillRequest(PartnerRequestContext requestContext, BillRequest rcRequest,
      BillResponse rcResponse) throws RechargeServiceException, IOException {

    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Request parameters " + rcRequest).format());

    StringBuffer uriBuf = new StringBuffer("https://");
    uriBuf.append(this.getHost());

    String uri = RechargeUtils.getRequestedURI(requestContext, getProtocol(), this.uri, rcRequest,
        this.getPartnerRequest());
    if (uri != null)
      uriBuf.append("/").append(uri);

    RequestConfig config =
        RequestConfig.custom().setConnectTimeout(rechargeConfigurations.getRequestTimeOut())
            .setConnectionRequestTimeout(rechargeConfigurations.getRequestTimeOut()).build();

    int retries = 0;
    CloseableHttpResponse httpResponse = null;
    CloseableHttpClient httpClient = null;
    
    boolean isResponseError = true;
    
    try {
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Request Time : " + String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()))
          .format());

      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("User requested URL :" + uriBuf.toString()).format());

      HttpClientBuilder builder = HttpClients.custom()
          .setSSLSocketFactory(partnerTrustStore.getSSLConnectionSocketFactory(requestContext,
              rcRequest.getMerchantReferenceId()))
          .disableAutomaticRetries()
          .setConnectionTimeToLive(rechargeConfigurations.getRequestTimeOut(), TimeUnit.MILLISECONDS)
          .setDefaultRequestConfig(config);

      LOG.debug(LogFormatter.instance(requestContext.getTraceId()).message("Proxy settings")
          .data("Host", getProxy().getHost()).data("Port", getProxy().getHost())
          .data("Enabled", getProxy().isEnabled()).format());

      if (getProxy().isEnabled()) {
        builder.setProxy(new HttpHost(getProxy().getHost(), getProxy().getPort()));
      }

      httpClient = builder.build();

      HttpUriRequest request = null;
      if (this.isHttpGetMethod()) {
        request = new HttpGet(uriBuf.toString());
      } else if (SupportedHttpMethod.POST == httpMethod) {
        request = new HttpPost(uriBuf.toString());

        byte[] postData = this.getPartnerRequest().getBillPayload(requestContext, getProtocol(),
            rcRequest, retries);

        ((HttpPost) request).setEntity(new ByteArrayEntity(postData));
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5");
        request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        request.addHeader(RechargeConstants.HEADER_TRACEID, requestContext.getTraceId());
      }

      if (request == null) {
        throw new Exception("Unsupported Request Type");
      }
      
      // process response
      httpResponse = httpClient.execute(request);
      
      isResponseError = false;  // if controll came here that means we got response res from euronet without any exception

      // @TODO - Need to handle REDIRECTS explicitly if not
      int status = httpResponse.getStatusLine().getStatusCode();

      byte[] rawData = EntityUtils.toByteArray(httpResponse.getEntity());
      String payload = new String(rawData, StandardCharsets.UTF_8);

      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Response data from server " + payload.length()).format());
      
      if (this.isFetchPlanRequest()) {
        
        rcResponse.put(RechargePlanResponse.Fields.RESPONSE, payload);
        LOG.debug(LogFormatter.instance(requestContext.getTraceId())
            .data("RechargePlanResponse", rcResponse.size()).format());
      } else {
        this.getPartnerResponse().processBillResults(requestContext, getProtocol(), rcRequest,
            rawData, rcResponse);
      }
      
      if (status != HttpsURLConnection.HTTP_OK) {
        rcResponse.setErrorMsg(payload);
      } else {
        rcResponse.setRawData(payload);
      }
      LOG.debug(LogFormatter.instance(requestContext.getTraceId())
          .message("Mapped Response data to Recharge Response : " + rcResponse.size()).format());
      
    } catch (Exception e) {

		if (isResponseError) {
			rcResponse.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
			rcResponse.setResponseError(RechargeErrorResponse.YES.value());
		}

      String desc =
          "Failed to process request for URL as no response was found: " + uriBuf.toString();
      LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format(), e);

    } finally {
      httpClient.close();
      httpResponse.close();
    }
  }
}
