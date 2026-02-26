package com.dipcoin.bank.services.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dipcoin.bank.services.comm.AccountBalanceFetchResponse;
import com.dipcoin.bank.services.comm.AccountFundTransferResponse;
import com.dipcoin.bank.services.comm.AddMoneyInVirtualAccountResponse;
import com.dipcoin.bank.services.comm.BankLoanResponse;
import com.dipcoin.bank.services.comm.BankRequest;
import com.dipcoin.bank.services.comm.BankResponse;
import com.dipcoin.bank.services.comm.BankUIDResponse;
import com.dipcoin.bank.services.comm.CallbackResponse;
import com.dipcoin.bank.services.comm.CreateVirtualAccountResponse;
import com.dipcoin.bank.services.comm.CustomerFundTransferResponse;
import com.dipcoin.bank.services.comm.CustomerInquiryResponse;
import com.dipcoin.bank.services.comm.DeleteAccountResponse;
import com.dipcoin.bank.services.comm.DipcoinRequest;
import com.dipcoin.bank.services.comm.DipcoinResponse;
import com.dipcoin.bank.services.comm.LienInquiryResponse;
import com.dipcoin.bank.services.comm.MarkLienResponse;
import com.dipcoin.bank.services.comm.RemoveLienAndDebitResponse;
import com.dipcoin.bank.services.comm.RemoveLienResponse;
import com.dipcoin.bank.services.comm.ResendOTPResponse;
import com.dipcoin.bank.services.comm.TpinValidationResponse;
import com.dipcoin.bank.services.comm.TransactionStatusResponse;
import com.dipcoin.bank.services.comm.UserAuthenticationResponse;
import com.dipcoin.bank.services.comm.VerifyOTPResponse;
import com.dipcoin.bank.services.utils.BankConstants;
import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.bank.services.utils.BankServiceException;
import com.dipcoin.commons.HttpsUtils.Proxy;
import com.dipcoin.commons.LogFormatter;

/*
 * Base Bank Client. Responsible for managing host, port, various types of requests and responses
 */
public abstract class BankClient {
  private static final Logger LOG = LogManager.getLogger(BankClient.class);

  /*
   * Operations enum is used to help standardize nomencalture. All Request/Response should be
   * <Operation>Request/Response In bank application context follow
   * <BankRefId><Operation>[Request/Response]
   */
  public enum Operation {
    // @formatter:off
    NO_OP("No"), 
    MARK_LIEN("MarkLien"), 
    REMOVE_LIEN("RemoveLien"), 
    USER_AUTH("UserAuthentication"), 
    VERIFY_OTP("VerifyOTP"), 
    RESEND_OTP("ResendOTP"), 
    DELETE_ACCOUNT("DeleteAccount"), 
    CUSTOMER_FUND_TRANSFER("CustomerFundTransfer"), 
    ACCOUNT_FUND_TRANSFER("AccountFundTransfer"), 
    REMOVE_LIEN_AND_DEBIT_TO_ACC("RemoveLienAndDebitToAccount"),
    CREDIT_ACCOUNT("CreditAccount"),
	CREATE_VIRTUAL_ACCOUNT("CreateVirtualAccount"),
	ADD_MONEY_TO_VIRTUAL_ACCOUNT("AddMoneyInVirtualAccount"),
    CUSTOMER_INQUIRY("CustomerInquiry"),
    LIEN_INQUIRY("LienInquiry"),
    MODIFY_LIEN("ModifyLien"),
    TRANSACTION_STATUS("TransactionStatus"),
    BANK_UID("BankUID"),
    CALLBACK("Callback"),
    TPIN_VALIDATION("TpinValidation"),
	BALANCE_FETCH("BalanceFetch"),
	OFFER_CHECK("OfferCheck"),
	DETAILS_MODIFICATION("DetailsModification"),
	NEW_TO_BANK_CUSTOMER("NewToBankCustomer"),
	BANK_STATEMENT_INITIATE("BankStatementInitiate"),
	BANK_STATEMENT_UPLOAD("BankStatementUpload"),
	BANK_STATEMENT_GENERATE_REPORT("BankStatementGenerateReport"),
	INSTANT_SANCTION("InstantSanction"),
	INSTANT_DISBURSEMENT("InstantDisbursement"),
	DATA_FETCH("DataFetch"),
	DISBURSEMENT_STATUS_CHECK("DisbursementStatusCheck"),
	MINI_STATEMENT("MiniStatement"),
	DASHBOARD("Dashboard");
    // @formatter:on

    private String value;

    private Operation(String value) {
      this.value = value;
    }

    public String value() {
      return this.value;
    }
  };

  public static enum Protocol {
    HTTP("http", false), HTTPS("https", true), TCP("tcp", false), STCP("stcp", true);

    private static Map<String, Protocol> lookup = new HashMap<>();
    static {
      lookup.put(HTTP.type, HTTP);
      lookup.put(HTTPS.type, HTTPS);
      lookup.put(TCP.type, TCP);
      lookup.put(STCP.type, STCP);
    }

    private final String type;
    private boolean isSecure;

    private Protocol(String type, boolean isSecure) {
      this.type = type;
      this.isSecure = isSecure;
    }

    public String type() {
      return this.type;
    }

    public boolean isSecure() {
      return isSecure;
    }

    public static Protocol lookup(String protocol) {
      return lookup.get(protocol);
    }
  }

  public enum ChecksumFormat {
    MD5, CRC32;
  }

  // Host/Port associated to a bank
  // @TODO - Need to figure out a way if there are multiple hosts provided by
  // bank
  private String host;
  private int port;
  private int requestTimeout;
  private boolean encryptPayload = false;
  private int maxRetries = 1;

  private boolean taggedResponseData = false;
  
  private boolean amountNonDecimal = false;
  
  private boolean checksumEnabled = false;

  private ChecksumFormat checksumFormat = ChecksumFormat.MD5;

  private int ostaReferenceIdLength = 50;
  
  private int contentType = 0;

  public String headerChecksum = BankConstants.HEADER_CHECKSUM;

  private Proxy proxy = new Proxy();

  // request and responses for operations
  private BankRequest bankRequest;
  private BankResponse bankResponse;
  
  private List<BankRequest> bankRequests;
  
  private List<BankResponse> bankResponses;
  
  private String requestFlow = "DefaultFlow";
  
  private Boolean isCallback = false;
  
  private Boolean isTpinValidation = false;
  
  private String serviceType = "";
  
  private int encryptionIterations = 1;
  
  private Boolean timeout = false;
  
  

  public List<BankRequest> getBankRequests() {
    return bankRequests;
  }

  public void setBankRequests(List<BankRequest> bankRequests) {
    this.bankRequests = bankRequests;
  }

  public List<BankResponse> getBankResponses() {
    return bankResponses;
  }

  public void setBankResponses(List<BankResponse> bankResponses) {
    this.bankResponses = bankResponses;
  }
  
  public String getRequestFlow() {
    return requestFlow;
  }

  public void setRequestFlow(String requestFlow) {
    this.requestFlow = requestFlow;
  }

  private Protocol protocol;

  public BankClient(Protocol protocol, String host, int port) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
  }

  public void setProxy(Proxy proxy) {
    if (proxy != null) {
      LOG.debug(
          LogFormatter.instance().message("BankClient Proxy Setup ").data("host", proxy.getHost())
              .data("port", proxy.getPort()).data("enabled", proxy.isEnabled()).format());
      this.proxy.setHost(proxy.getHost());
      this.proxy.setPort(proxy.getPort());
      this.proxy.setEnabled(proxy.isEnabled());
    }
  }

  protected final Proxy getProxy() {
    return proxy;
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public String getHost() {
    return this.host;
  }

  public Integer getPort() {
    return this.port;
  }

  public BankRequest getBankRequest() {
    return bankRequest;
  }

  public void setBankRequest(BankRequest bankRequest) {
    this.bankRequest = bankRequest;
  }

  public BankResponse getBankResponse() {
    return bankResponse;
  }

  public void setBankResponse(BankResponse bankResponse) {
    this.bankResponse = bankResponse;
  }
  
  public Protocol getProtocol() {
    return protocol;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public int getMaxRetries() {
    return this.maxRetries;
  }

  public boolean isEncryptPayload() {
    return encryptPayload;
  }

  public void setEncryptPayload(boolean encryptPayload) {
    this.encryptPayload = encryptPayload;
  }

  public boolean isAmountNonDecimal() {
    return amountNonDecimal;
  }

  public void setAmountNonDecimal(boolean amountNonDecimal) {
    this.amountNonDecimal = amountNonDecimal;
  }

  public boolean isTaggedResponseData() {
    return taggedResponseData;
  }

  public void setTaggedResponseData(boolean taggedResponseData) {
    this.taggedResponseData = taggedResponseData;
  }

  public ChecksumFormat getChecksumFormat() {
    return checksumFormat;
  }

  public void setChecksumFormat(ChecksumFormat checksumFormat) {
    this.checksumFormat = checksumFormat;
  }

  public Boolean getIsTpinValidation() {
    return isTpinValidation;
  }

  public void setIsTpinValidation(Boolean isTpinValidation) {
    this.isTpinValidation = isTpinValidation;
  }

  public int getOstaReferenceIdLength() {
    return ostaReferenceIdLength;
  }

  public void setOstaReferenceIdLength(int ostaReferenceIdLength) {
    this.ostaReferenceIdLength = ostaReferenceIdLength;
  }

  public int getContentType() {
    return contentType;
  }

  public void setContentType(int contentType) {
    this.contentType = contentType;
  }

  public void setHeaderChecksum(String headerChecksum) {
    this.headerChecksum = headerChecksum;
  }

  public boolean isChecksumEnabled() {
    return checksumEnabled;
  }

  public void setChecksumEnabled(boolean checksumEnabled) {
    this.checksumEnabled = checksumEnabled;
  }

  public Boolean getIsCallback() {
    return isCallback;
  }

  public void setIsCallback(Boolean isCallback) {
    this.isCallback = isCallback;
  }

  public Boolean getTimeout() {
    return timeout;
  }

  public void setTimeout(Boolean timeout) {
    this.timeout = timeout;
  }

  public int getEncryptionIterations() {
    return encryptionIterations;
  }

  public void setEncryptionIterations(int encryptionIterations) {
    this.encryptionIterations = encryptionIterations;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  /*
   * Function to do final execution of the request and gather the response.
   */
  protected abstract void executeRequest(final BankRequestContext requestContext,
      final DipcoinRequest dcRequest, final DipcoinResponse dcResponse)
      throws BankServiceException, IOException;

  // public interface for the permitted operations
  // cannot be overridden
  @SuppressWarnings("unchecked")
  public final <T extends DipcoinResponse> T processRequest(final BankRequestContext requestContext,
      final DipcoinRequest request) throws BankServiceException, IOException {
    // deep clone request object
    DipcoinRequest dcRequest = request.deepCopy();
    DipcoinResponse dcResponse = null;
    switch (dcRequest.getOperation()) {
      case MARK_LIEN:
        dcResponse = new MarkLienResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case REMOVE_LIEN:
        dcResponse = new RemoveLienResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case USER_AUTH:
        dcResponse = new UserAuthenticationResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case CUSTOMER_FUND_TRANSFER:
        dcResponse = new CustomerFundTransferResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case ACCOUNT_FUND_TRANSFER:
        dcResponse = new AccountFundTransferResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case VERIFY_OTP:
        dcResponse = new VerifyOTPResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case RESEND_OTP:
        dcResponse = new ResendOTPResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case DELETE_ACCOUNT:
        dcResponse = new DeleteAccountResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case REMOVE_LIEN_AND_DEBIT_TO_ACC:
        dcResponse = new RemoveLienAndDebitResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case CREATE_VIRTUAL_ACCOUNT:
        dcResponse = new CreateVirtualAccountResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case ADD_MONEY_TO_VIRTUAL_ACCOUNT:
        dcResponse = new AddMoneyInVirtualAccountResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case LIEN_INQUIRY:
        dcResponse = new LienInquiryResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case CUSTOMER_INQUIRY:
        dcResponse = new CustomerInquiryResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case MODIFY_LIEN:
        dcResponse = new MarkLienResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case CALLBACK:
        dcResponse = new CallbackResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case TPIN_VALIDATION:
        dcResponse = new TpinValidationResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case BANK_UID:
        dcResponse = new BankUIDResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case TRANSACTION_STATUS:
        dcResponse = new TransactionStatusResponse();
        executeRequest(requestContext, dcRequest, dcResponse);
        break;
      case BALANCE_FETCH:
          dcResponse = new AccountBalanceFetchResponse();
          executeRequest(requestContext, dcRequest, dcResponse);
          break; 
      case OFFER_CHECK:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case DETAILS_MODIFICATION:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case NEW_TO_BANK_CUSTOMER:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case BANK_STATEMENT_INITIATE:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case BANK_STATEMENT_UPLOAD:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case BANK_STATEMENT_GENERATE_REPORT:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case INSTANT_SANCTION:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case INSTANT_DISBURSEMENT:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case DATA_FETCH:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case DISBURSEMENT_STATUS_CHECK:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case MINI_STATEMENT:
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      case DASHBOARD :
  		dcResponse = new BankLoanResponse();
  		executeRequest(requestContext, dcRequest, dcResponse);
  		break;
      default:
        LOG.error("Invalid Operation: " + dcRequest.getOperation());
    }
    return (T) dcResponse;
  }
}
