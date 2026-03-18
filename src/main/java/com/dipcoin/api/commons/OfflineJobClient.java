package com.dipcoin.api.commons;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.dipcoin.api.commons.OfflineJobClient.JobScheduleRequest.RequestData;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.model.BankTransactionReport;
import com.dipcoin.api.model.ReconBodyFieldMapperRequest;
import com.dipcoin.api.model.ReconHeadersFieldMapperRequest;
import com.dipcoin.api.model.ReinitiateOfflineRequest;
import com.dipcoin.api.model.TollGetExceptionListResponse;
import com.dipcoin.api.model.TollTagRequest;
import com.dipcoin.bank.services.utils.BankConstants;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

@Component("offlineJobClient")
public class OfflineJobClient {

  private static final Logger LOG = LogManager.getLogger(OfflineJobClient.class);

  private final static ObjectMapper objectMapper = new ObjectMapper();

  private final static String USER_AGENT = "Mozilla/5.0";

  @Autowired
  private ApplicationProperties applicationProperties;

  public String initPartnerSetupRequest(Merchant merchant, Bank bank, String traceId)
      throws ClientProtocolException {
    RequestData partnerSetupRequest = new RequestData();
    if (merchant != null) {
      partnerSetupRequest.setEmail(merchant.getEmailId());
      partnerSetupRequest.setReferenceId(merchant.getReferenceId());
    } else {
      partnerSetupRequest.setEmail(bank.getEmailId());
      partnerSetupRequest.setReferenceId(bank.getCode());
    }

    return this.callOffline(null, partnerSetupRequest, traceId, APIConstants.OFFLINE_JOB_PATH,
        applicationProperties.getOfflineApi(), null, null);
  }

  public String initOfflineJobRequest(User user, String traceId) throws ClientProtocolException {

    return this.callOffline(null, null, traceId, APIConstants.OFFLINE_SETTLEMENT_JOB_PATH,
        applicationProperties.getOfflineApi(), null, null);
  }

  public String initSignatureGenerationRequest(Bank bank, MultipartFile doc, String traceId)
      throws IOException {


    RequestData requestData = new RequestData();
    requestData.setReferenceId(bank.getReferenceId());
    requestData.setIin(bank.getIin());
    requestData.setAlias(bank.getAlias());
    return this.callOffline(doc, requestData, traceId,
        APIConstants.OFFLINE_JOB_PATH_TAG_SIGNATURE_GENERATION,
        applicationProperties.getOfflineApiUploadFile(), null, null);
  }
  
  public String initExceptionListUpdation( TollGetExceptionListResponse tollGetExceptionListResponse,String traceId)
	      throws IOException {


	    RequestData requestData = new RequestData();
	    requestData.setTollGetExceptionListResponse(tollGetExceptionListResponse);
	   
	    return this.callOffline(null, requestData, traceId,
	        APIConstants.OFFLINE_JOB_PATH_TOLL_LOW_BALANCE,
	        applicationProperties.getOfflineApiUploadFile(), null, null);
	  }

      public String initMailingTagSignDataRequest(Bank bank, Integer lotNumber,
          String startSerialNumber, String endSerialNumber, String traceId) throws IOException {


    RequestData requestData = new RequestData();
    requestData.setReferenceId(bank.getReferenceId());
    requestData.setIin(bank.getIin());
    requestData.setAlias(bank.getAlias());
    requestData.setLotNumber(lotNumber);
    requestData.setStartSerialNumber(startSerialNumber);
    requestData.setEndSerialNumber(endSerialNumber);;
    requestData.setBankName(bank.getName());
    requestData.setEmail(bank.getEmailId());
    return this.callOffline(null, requestData, traceId,
        APIConstants.OFFLINE_JOB_PATH_TAG_SIGNATURE_GENERATION,
        applicationProperties.getOfflineApi(), null, null);
  }
  
  public String initMailTollStatement(Bank bank, User user, TollTagRequest statementRequest, String traceId)
      throws IOException {


    RequestData requestData = new RequestData();
    requestData.setVehicleNumber(statementRequest.getRegistrationNo().trim());
    requestData.setStartTime(statementRequest.getStartTime());
    requestData.setEndTime(statementRequest.getEndTime());
    requestData.setTransactionFileType(statementRequest.getTransactionFileType());
    requestData.setEmail(StringUtils.isNotBlank(statementRequest.getEmailId()) ? statementRequest.getEmailId(): user.getEmail()); 
    return this.callOffline(null, requestData, traceId,
        APIConstants.OFFLINE_JOB_PATH_TOLL_USAGE,
        applicationProperties.getOfflineApi(), null, null);
  }
  
  public String initMailTollAccountingRecon(Bank bank, User user, TollTagRequest accountingReconRequest, String traceId)
	      throws IOException {

	    RequestData requestData = new RequestData();	    
	    requestData.setStartTime(accountingReconRequest.getStartTime());
	    requestData.setEndTime(accountingReconRequest.getEndTime());
	    requestData.setTransactionFileType(accountingReconRequest.getTransactionFileType());
	    requestData.setEmail(StringUtils.isNotBlank(accountingReconRequest.getEmailId()) ? accountingReconRequest.getEmailId(): user.getEmail()); 
	    requestData.setReferenceId(bank.getReferenceId());
	    return this.callOffline(null, requestData, traceId,
	        APIConstants.OFFLINE_JOB_PATH_TOLL_TAG_INVOICE,
	        applicationProperties.getOfflineApi(), null, null);
	  }

  public String initBulkHotlist(User user, MultipartFile doc, String operation,
      String exception,Boolean isForceClose, String traceId) throws IOException {

          RequestData requestData = new RequestData();
          requestData.setOperation(operation);
          requestData.setException(exception);
          requestData.setIsForceClose(isForceClose);
          
          return this.callOffline(doc, requestData, traceId,
              APIConstants.OFFLINE_JOB_BULK_HOTLIST_JOB,
              applicationProperties.getOfflineApiUploadFile(), null, null);
  }
  
  public String initBulkLienRemoveCBI(User user, MultipartFile file, String traceId) {
//
//      RequestData requestData = new RequestData();
//      requestData.setOperation(operation);
//      requestData.setException(exception);
//  
      return this.callOffline(file, null, traceId,
          APIConstants.OFFLINE_JOB_BULK_LIEN_REMOVE_CBI_JOB,
          applicationProperties.getOfflineApiUploadFile(), null, null);
}
     
  public String callOffline(MultipartFile doc, RequestData requestData, String traceId,
      String jobPath, String url, String startTime, String endTime) {

    StringBuffer uriBuf = new StringBuffer(url);
    if (StringUtils.isEmpty(uriBuf)) {
      LOG.error(LogFormatter.instance(traceId).message("Url is not set").data("traceId", traceId)
          .data("uriBuf", uriBuf).format());
      return null;
    }

    HttpPost post = new HttpPost(uriBuf.toString());

    HttpClient client = HttpClientBuilder.create()
        .setConnectionTimeToLive(APIConstants.REQUEST_TIMEOUT, TimeUnit.MILLISECONDS).build();

    try {
      startTime = StringUtils.isNoneBlank(startTime) ? startTime
          : String.valueOf(new DateTime(DateTime.now(DateTimeZone.UTC)).getMillis());
      endTime = StringUtils.isNoneBlank(endTime) ? endTime
          : String.valueOf(new DateTime(DateTime.now(DateTimeZone.UTC)).getMillis());

      LOG.error(LogFormatter.instance(traceId).message("Request data in call offline ").data("requestData", requestData).format());
      
      JobScheduleRequest jobScheduleRequest = new JobScheduleRequest();
      jobScheduleRequest.setStartTime(startTime);
      jobScheduleRequest.setEndTime(endTime);
      jobScheduleRequest.setJobClass(jobPath);
      jobScheduleRequest.setStartDelayInSec(0);
      jobScheduleRequest.setRepeatIntervalInSec(0);
      jobScheduleRequest.setRepeatForever(false);
      jobScheduleRequest.setTrigger(0);
      if (requestData != null) {
        jobScheduleRequest.setData(requestData);
      }
      String postData = objectMapper.writeValueAsString(jobScheduleRequest);

      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
      if (doc != null) {
        builder.addBinaryBody("doc", doc.getBytes(), ContentType.DEFAULT_BINARY, doc.getName());
      }

      builder.addTextBody("jobScheduleRequest", postData, ContentType.DEFAULT_BINARY);
      org.apache.http.HttpEntity entity = builder.build();
      post.setEntity(entity);
      post.setHeader(BankConstants.HEADER_TRACEID, traceId);
      HttpResponse response = client.execute(post);

      int status = response.getStatusLine().getStatusCode();
      LOG.debug(LogFormatter.instance(traceId).message("Response info from offline")
          .data("status", status).data("requestedURL", uriBuf.toString())
          .data("responseTime", String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()))
          .format());

      if (HttpStatus.SC_OK != status) {
        return null;
      }

      String body = EntityUtils.toString(response.getEntity(), "UTF-8");

      LOG.debug(LogFormatter.instance(traceId).message("Response data from server")
          .data("data", body).format());

      return body;
    } catch (Exception e) {
      LOG.error(LogFormatter.instance(traceId).message("Exception caught").format(), e);
      return null;
    }
  }
  
  

	public String initNEFT(User user, String bankReferenceId, String traceId) throws IOException {

		RequestData requestData = new RequestData();
		requestData.setReferenceId(bankReferenceId);
		return this.callOffline(null, requestData, traceId, APIConstants.OFFLINE_JOB_PATH_NEFT_ACCOUNT_FUND_TRANSFER,
				applicationProperties.getOfflineApi(), null, null);
	}
  
    public String initiateBillPaymentInfoJobWithBiller(User user, String billerId, String traceId) {
      RequestData requestData = new RequestData();
      requestData.setBillerId(billerId);
      return this.callOffline(null, requestData, traceId,
          APIConstants.OFFLINE_JOB_PATH_BILLPAYMENT_INFO_UPDATE_JOB,
          applicationProperties.getOfflineApi(), null, null);
    }
    
    
    public String initTollDeemedTransactionAccountingJob(List<Integer> dtxIds,String traceId)
  	      throws IOException {
        RequestData requestData = new RequestData();
        requestData.setDtxIds(dtxIds); 	   
  	    return this.callOffline(null, requestData, traceId,
  	        APIConstants.OFFLINE_JOB_PATH_TOLL_DEEMED_TRANSACTION_ACCOUNTING_JOB,
  	        applicationProperties.getOfflineApi(), null,null);
  	  }
    
    public String initLienMarkMailJob(User user, String customerAccountNumber, String traceId) {
        RequestData requestData = new RequestData();
        requestData.setCustomerAccountNo(customerAccountNumber);
        return this.callOffline(null, requestData, traceId,
            APIConstants.OFFLINE_MAIL_CBI_LIENMARK_TXNS_JOB,
            applicationProperties.getOfflineApi(), null, null);
      }
    
    public String initiateTagWiseAccountLienRemovalJob(User user, String tagId, String traceId) {
        RequestData requestData = new RequestData();
        requestData.setTagId(tagId);
        return this.callOffline(null, requestData, traceId,
            APIConstants.OFFLINE_TAGWISE_ACCOUNT_LIEN_REMOVAL_JOB,
            applicationProperties.getOfflineApi(), null, null);
      }
	
  public static class JobScheduleRequest {
    private RequestData data;
    private String jobClass;
    private int trigger = 0;
    private int startDelayInSec;
    private int repeatIntervalInSec;
    private String cronExpression;
    private String startTime;
    private String endTime;
    private boolean repeatForever = false;

    @Getter
    @Setter
    public static class RequestData {
      private String referenceId;
      private String email;
      private String iin;
      private String alias;
      private Integer lotNumber;
      private String startSerialNumber;
      private String endSerialNumber;
      private String bankName;
      private String processFileType;
      private ReconHeadersFieldMapperRequest header;
      private List<ReconBodyFieldMapperRequest> body;
      private String partner;
      private ReinitiateOfflineRequest reinitiateOfflineRequest;
      private Boolean invoice;
      private Long startTime;
      private Long endTime;
      private int transactionFileType;
      private String vehicleNumber;
      private TollGetExceptionListResponse tollGetExceptionListResponse;
      private BankTransactionReport bankTransactionReport;
      private String billerId; // this Biller Id will be used in Offline system to update the Biller Data in DB after hitting Euronet MDM API
      private List<Integer> dtxIds;
      private String operation;
      private String exception;
      private Boolean isForceClose;
      private String customerAccountNo;
      private String tagId;

      public String getReferenceId() {
        return referenceId;
      }

      public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
      }

      public String getEmail() {
        return email;
      }

      public void setEmail(String email) {
        this.email = email;
      }
    }

    public RequestData getData() {
      return data;
    }

    public void setData(RequestData data) {
      this.data = data;
    }

    public String getJobClass() {
      return jobClass;
    }

    public void setJobClass(String jobClass) {
      this.jobClass = jobClass;
    }

    public int getStartDelayInSec() {
      return startDelayInSec;
    }

    public void setStartDelayInSec(int startDelayInSec) {
      this.startDelayInSec = startDelayInSec;
    }

    public int getRepeatIntervalInSec() {
      return repeatIntervalInSec;
    }

    public void setRepeatIntervalInSec(int repeatIntervalInSec) {
      this.repeatIntervalInSec = repeatIntervalInSec;
    }

    public String getCronExpression() {
      return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
      this.cronExpression = cronExpression;
    }

    public String getStartTime() {
      return startTime;
    }

    public void setStartTime(String startTime) {
      this.startTime = startTime;
    }

    public String getEndTime() {
      return endTime;
    }

    public void setEndTime(String endTime) {
      this.endTime = endTime;
    }

    public boolean isRepeatForever() {
      return repeatForever;
    }

    public void setRepeatForever(boolean repeatForever) {
      this.repeatForever = repeatForever;
    }

    public int getTrigger() {
      return trigger;
    }

    public void setTrigger(int trigger) {
      this.trigger = trigger;
    }
  }

}
