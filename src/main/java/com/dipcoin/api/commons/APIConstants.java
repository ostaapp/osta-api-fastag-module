package com.dipcoin.api.commons;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.dipcoin.commons.CoreUtils;
//import com.dipcoin.commons.qrcode.ColoredDecorator;
//import com.dipcoin.commons.qrcode.Decorator;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;

public final class APIConstants {

  // Request type
  public enum RequestType {
    GET("get"), POST("post"), DELETE("delete"), OPTIONS("options"), HEAD("head");

    private String type;

    private RequestType(String type) {
      this.type = type;
    }

    public String value() {
      return this.type;
    }
  }

  public static final String USER_AGENT = "User-Agent";
  public static final String DISABLE_HEADER = "disableHeader";
  public static final String HOST = "Host";
  public static final String X_FWD_PROTO = "X-Forwarded-Proto";
  public static final String X_FWD_PORT = "X-Forwarded-Port";
  public static final String X_FWD_FOR = "X-Forwarded-For";
  public static final String X_DEVICE_ID = "X-Device-Id";
  public static final String API_KEY = "api_key"; // for swagger ui
  public static final String REQUEST_TIMER_CONTEXT = "request_timer_context";
  public static final String API_TIMER_CONTEXT = "api_timer_context";

  // cookie fields
  public static final String DC_LOGIN_COOKIE = "dcl";
  public static final String USER_ID = "u";
  public static final String AUTH_TOKEN = "at";
  public static final String MERCHANT_ID = "m";
  public static final String BANK_ID = "b";

  public static final String SESSION = "SESSION";
  public static final String USER = "USER";
  public static final String PARTNER_USER = "PARTNER_USER";
  public static final String CUSTOMER = "CUSTOMER";
  public static final String MERCHANT = "MERCHANT";
  public static final String BANK = "BANK";
  public static final String BRONTOO = "BRONTOO";

  public static final String VIRTUAL_CUSTOMER = "VIRTUAL_CUSTOMER";
  public static final String VIRTUAL_MERCHANT = "VIRTUAL_MERCHANT";
  public static final String VIRTUAL = "v";

  public static final String REQUEST_TIME = "RequestTime";
  public static final String RESPONSE_TIME = "ResponseTime";

  public static final String ORIGIN = "origin";
  public static final String X_PARTNER_ID = "X-Partner-Id";
  public static final String FORMAT = "format";
  public static final String FORMAT_XML = "xml";
  public static final String X_REDIRECT_PATH = "X-Redirected-Path";
  public static final String X_REDIRECT_PARAMS = "X-Redirected-Params";

  public static final String X_DIPCOIN_TRANSACTION_ID = "X-Osta-TransactionId";

  public static final String BRONTOO_PARTNER_REFERENCE_ID = "BrontooReferenceId";
  // Session
  public static final String AUTHORIZATION_TOKEN = "Token";
  public static final int SESSION_TIMEOUT = 36000;
  public static final int SESSION_COOLDOWN = 6000;

  public static final String CLIENT_TRANSACTION_ID = "clientTransactionId";
  public static final String PARTNER = "/partner";
  public static final String OSTAAPP_COM = "ostaapp.com";

  public static final String SERVER_UPLOAD_LOCATION_FOLDER = "/tmp/";
  public static final String MERCHANT_INDIVIDUAL_PATH = "Merchant/Individual/";
  public static final String MERCHANT_CORPORATE_PATH = "Merchant/Corporate/";
  public static final String MERCHANT_ONLINE_PATH = "Merchant/Online/";

  public static final String CUSTOMER_TOLL_PATH = "Customer/toll/";
  public static final String OFFLINE_BASE_PACKAGE = "com.dipcoin.scheduler.job.";
  public static final String OFFLINE_JOB_PATH = "com.dipcoin.scheduler.job.SetupPartnerJob";
  public static final String OFFLINE_JOB_PATH_TAG_SIGNATURE_GENERATION = "com.dipcoin.scheduler.job.TagSignatureGenerationJob";
  public static final String OFFLINE_JOB_PATH_RECON_FILE_UPLOAD = "com.dipcoin.scheduler.job.PartnerUploadFileToSftpJob";
  public static final String OFFLINE_JOB_PATH_REINITIATE = "com.dipcoin.scheduler.job.ReinitiateDipcoinAccountingJob";
  public static final String OFFLINE_JOB_PATH_TOLL_TAG_INVOICE = "com.dipcoin.scheduler.job.TollMonitoringAndReportsJob";
  public static final String OFFLINE_JOB_PATH_TOLL_USAGE = "com.dipcoin.scheduler.job.TollUsageJob";
  public static final String OFFLINE_SETTLEMENT_JOB_PATH = "com.dipcoin.scheduler.job.SettlementFileGenerateJob";
  public static final String OFFLINE_JOB_PATH_TOLL_LOW_BALANCE= "com.dipcoin.scheduler.job.TollLowBalanceTopupJob";
  public static final String OFFLINE_JOB_PATH_BANK_TRANSACTIONS_REPORT = "com.dipcoin.scheduler.job.BankTransactionReportJob";
  public static final String OFFLINE_JOB_PATH_NEFT_ACCOUNT_FUND_TRANSFER = "com.dipcoin.scheduler.job.NEFTAccountFundTransferJob";
  public static final String OFFLINE_JOB_PATH_BILLPAYMENT_INFO_UPDATE_JOB = "com.dipcoin.scheduler.job.BillPaymentInfoUpdateJob";
  public static final String OFFLINE_JOB_BULK_HOTLIST_JOB = "com.dipcoin.scheduler.job.BulkHotListJob";
  public static final String OFFLINE_JOB_PATH_TOLL_DEEMED_TRANSACTION_ACCOUNTING_JOB = "com.dipcoin.scheduler.job.TollDeemedTransactionAccountingJob";
  public static final String OFFLINE_JOB_BULK_LIEN_REMOVE_CBI_JOB = "com.dipcoin.scheduler.job.BulkLienRemoveCBIJob";
  public static final String OFFLINE_MAIL_CBI_LIENMARK_TXNS_JOB = "com.dipcoin.scheduler.job.MailCBIActiveAccountLienMarkDetailJob";
  public static final String OFFLINE_TAGWISE_ACCOUNT_LIEN_REMOVAL_JOB = "com.dipcoin.scheduler.job.TagWiseAccountLienRemovalJob";


  public static final String RECON ="RECON";
  public static final String SETTLEMENT ="SETTLEMENT";
  
  public static final long MIN_TIMESTAMP = 1440354599999L; // Company origin
  
  public static int REQUEST_TIMEOUT = 10000; // millisec
  
  public static final Integer MAX_PAGINATE_COUNT = 100;
  public static final String OAUTH2 = "/v1/oauth2";
  public static final Integer OAUTH2_ACCESS_TOKEN_EXPIRY = 3600;
  public static final Integer OAUTH2_AUTHORIZATION_TOKEN_EXPIRY = 3600 * 24;
  public static final String HEALTHCHECK_API = "/v1/system/healthcheck";
  public static final String HEAPCHECK_API = "/v1/system/heapMemoryCheck";
  public static final String DEPLOYMENT_API = "/v1/system/deployment";
  public static final String SWAGGER_API = "swagger.json";
  public static final String INTERNAL_PARTNER_EMAIL_SUFFIX = "@partner.dipcoin.com";
  public static final String INTERNAL_VIRTUAL_BANK_EMAIL_SUFFIX = "@virtualBank.dipcoin.com";
  public static final String EMPTY_RESPONSE = "{}";
  public static final String ANDROID = "Android";
  public static final String IOS = "Ios";
  public static final String WEB = "Web";
  public static final String IVR = "Ivr";
  public static final String CHECKSUM_KEY = "_checksum";
  public static final BigDecimal GST = new BigDecimal(18);
  public static final BigDecimal zeroAmount = new BigDecimal(0.00);
  public static final String PIN = CoreUtils.randomAlphaString(64);
  public static final BigDecimal APPROX_MERCHANT_CANCELLATION_VALUE = new BigDecimal(0.98);
  
  public static final String ENTER_TPIN_NOTE = "Enter TPIN of the Bank Account ";
  
  public static final Integer ROUND = 2;
  
  public static final String TOLL_TYPE_NAME = "FASTag";

  public static final String NPCI_SUCCESS = "SUCCESS"; 
  
  public static final String COPY_MARKING_EMAIL_FOR_SUBUSER = "internal.osta@gmail.com";

  public static final List<String> MERCHANT_INTERNAL_USER_LIST =
      new ArrayList<String>(Arrays.asList(UserRoles.MERCHANT_INTERNAL.value()));
  public static final List<String> BANK_INTERNAL_USER_LIST =
      new ArrayList<String>(Arrays.asList(UserRoles.BANK_INTERNAL.value()));

  public static int AMQP_DEFAULT_TIMEOUT = 60;
  
  public static final BigDecimal GST_TAX = BigDecimal.valueOf(0.18);
  public static final BigDecimal TDS_TAX = BigDecimal.valueOf(0.10);
  public static final BigDecimal CEDGE_COMISSION = BigDecimal.valueOf(0.4);
  public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  

//  public enum DipcoinColor {
//    CUSTOMER_GENERATED(0x800080), CUSTOMER_EXPIRING(0xFF88000), CUSTOMER_EXPIRED(
//        0xFF3333), CUSTOMER_DELETED(0x000000);
//
////    private Decorator<BufferedImage> color;
////
////    private DipcoinColor(int rgb) {
////      this.color = ColoredDecorator.colorizeQRCode(new Color(rgb));
////    }
////
////    public Decorator<BufferedImage> value() {
////      return this.color;
////    }
//  }

  public enum TransactionRequestType {

    WALLET_TOP_UP("WALLETTOPUP"), CREATEOSTA("CREATEOSTA"), PAYMENT("PAYMENT");

    private String transactionRequestType;

    private TransactionRequestType(String transactionRequestType) {
      this.transactionRequestType = transactionRequestType;
    }

    public String value() {
      return this.transactionRequestType;
    }

    public Boolean equals(String transactionReq) {
      return this.transactionRequestType.equalsIgnoreCase(transactionReq);
    }
  }
  public enum TransactionType {
    // @formatter:off
		MERCHANT_TYPE(0), WALLET_TYPE(1), PARTNER_TYPE(2);
		// @formatter:on

    private int status;

    private TransactionType(int s) {
      this.status = s;
    }

    public int value() {
      return this.status;
    }

    public boolean equals(int status) {
      return this.status == status;
    }
  }

  public enum RequestSource {

    WEB("0"), ANDROID("1"), IOS("2"), WEB2("3");

    private String source;

    private RequestSource(String source) {
      this.source = source;
    }

    public String value() {
      return this.source;
    }

    public Boolean equals(String transactionReq) {
      return this.source.equalsIgnoreCase(transactionReq);
    }
    
    public static boolean contains(String source) {
        if (StringUtils.isEmpty(source)) {
          return false;
        }
        for (RequestSource sourceEnum : RequestSource.values()) {
          if (sourceEnum.source.equals(source)) {
            return true;
          }
        }
        return false;
      }
  }
  
  public enum RemoveColumns {
    COIN("Coin"), PIN("Pin"), PINSALT("PinSalt"), TPIN("Tpin"), TPINSALT("TpinSalt"),
    PASSWORD("Password"), SALT("Salt");

    private String columns;

    private RemoveColumns(String columns) {
      this.columns = columns;
    }

    public String value() {
      return this.columns;
    }

    public Boolean equals(String transactionReq) {
      return this.columns.equalsIgnoreCase(transactionReq);
    }
    
    public final static Map<Integer, String> removeColumnsMap = new HashMap<>();
    static {
      removeColumnsMap.put(0, COIN.value());
      removeColumnsMap.put(1, PIN.value());
      removeColumnsMap.put(2, PINSALT.value());
      removeColumnsMap.put(3, TPIN.value());
      removeColumnsMap.put(4, TPINSALT.value());
      removeColumnsMap.put(3, PASSWORD.value());
      removeColumnsMap.put(4, SALT.value());
    }
  }
  
  public enum ReportType {
    // @formatter:off
        BANK("Bank"), VENDOR("Vendor");
     // @formatter:on

    private String reportType;

    private ReportType(String reportType) {
      this.reportType = reportType;
    }

    public String value() {
      return this.reportType;
    }

    public Boolean equals(String reportType) {
      return this.reportType.equalsIgnoreCase(reportType);
    }

  }
  
  public enum DateFormatter {
    DDMMYYYY("ddMMyyyy"), DD_MM_YYYY("dd/MM/yyyy"), YYYYMMDD("yyyyMMdd"), DATE_TIME_SEC(
        "yyyy-MM-dd HH:mm:ss"), DDMMYYYY_DASH(
            "dd-MM-yyyy"), TIME_hhmmss("hhmmss"), YYYYMMDDHHMMSS("yyyyMMddHHmmss"), DD_MM_YYYY_TIME(
                "dd/MM/yyyy HH:mm:ss"), TIME_hhmmss_COLONS("HH:mm:ss"), YYYY_MM_DD(
                    "yyyy-MM-dd"), DD_MM_YYYY_HH_MM_SS("dd-MM-yyyy HH:mm:ss"), YYMMDD("yyMMdd");

    private String date;

    private DateFormatter(String date) {
      this.date = date;
    }

    public String value() {
      return this.date;
    }

    public boolean equals(String date) {
      return this.date.equals(date.toUpperCase());
    }
  }
  
  public enum EpcDataUpdateRequest {

    // @formatter:off
        SINGLE_UPDATE(0), BULK_UPDATE(1);
        // @formatter:on

    private Integer req;

    private EpcDataUpdateRequest(Integer s) {
      this.req = s;
    }

    public Integer value() {
      return this.req;
    }

    public boolean equals(Integer status) {
      return this.req == status;
    }

  }
  
  public enum FilePath {
	    BANK_PATH("Bank/"), MERCHANT_PATH("Merchant/"), PARTNER_PATH(
	        "Partner/"), PARTNER_ANALYSIS_FILE_PATH(
	            "Partner/PartnerFileAnalysis"), PAYMNET_GATEWAY_PATH(
	                "PaymentGateway"), TID_PATH("Tid/Bank/"), TAG_DETAILS_PATH("TagDetails/Bank/"),
	    TOLL_USAGE("Toll/");

	    private String filePath;

	    private FilePath(String filePath) {
	      this.filePath = filePath;
	    }

	    public String value() {
	      return this.filePath;
	    }

	    public boolean equals(String filePath) {
	      return this.filePath.equals(filePath.toUpperCase());
	    }
	  }

}
