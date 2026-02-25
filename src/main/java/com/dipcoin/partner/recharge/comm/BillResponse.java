package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BillResponse extends HashMap<String, Object> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static ObjectMapper objectMapper = new ObjectMapper();
  private final HashMap<String, String> FIELDS_MAPPING = new HashMap<>();

  public static interface Fields {
    public static final String RESPONSE_CODE = "ResponseCode";
    public static final String RESPONSE_MESSAGE = "ResponseMessage";
    public static final String RESPONSE_DESCRIPTION = "ResponseDescription";
    public static final String PAYMENT_REFERENCE_NUMBER = "PaymentRefNo";
    public static final String BILL_PAYMENT_TOKEN = "BillPaymentToken";
    public static final String BILLER_ID = "BillerID";
    public static final String ADDITIONAL_INFORMATION = "AdditionalInformation";
    public static final String MERCHANT_REFERENCE_NUMBER = "MerchantRefNo";
    public static final String APPROVAL_REFERENCE_NUMBER = "ApprovalRefNo";
    public static final String STATE_CODE = "StateCode";
    public static final String RESPONSE_ERROR = "ResponseError";
  }

  private String errorMsg;
  private String rawData;

  public void setFieldsMapping(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (String key : mappings.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(mappings.get(key))) {
          FIELDS_MAPPING.put(key, mappings.get(key));
        }
      }
    }
  }

  public final Map<String, String> getFieldsMapping() {
    return FIELDS_MAPPING;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public String getRawData() {
    return rawData;
  }

  public void setRawData(String rawData) {
    // @NOTE - do not allow any random data in. setting a max limit.
    this.rawData = StringUtils.abbreviate(rawData, 1024);
  }

  public final String getResponseCode() {
    return (String) this.get(Fields.RESPONSE_CODE);
  }

  public final void setResponseCode(String responseCode) {
    this.put(Fields.RESPONSE_CODE, responseCode);
  }

  public final String getResponseMessage() {
    return (String) this.get(Fields.RESPONSE_MESSAGE);
  }

  public final void setResponseMessage(String responseMessage) {
    this.put(Fields.RESPONSE_MESSAGE, responseMessage);
  }

  public final String getResponseDescription() {
    return (String) this.get(Fields.RESPONSE_DESCRIPTION);
  }

  public final void setResponseDescription(String responseDescription) {
    this.put(Fields.RESPONSE_DESCRIPTION, responseDescription);
  }

  public final String getPaymentRefNo() {
    return (String) this.get(Fields.PAYMENT_REFERENCE_NUMBER);
  }

  public final void setPaymentRefNo(String paymentRefNo) {
    this.put(Fields.PAYMENT_REFERENCE_NUMBER, paymentRefNo);
  }

  public final String getBillPaymentToken() {
    return (String) this.get(Fields.BILL_PAYMENT_TOKEN);
  }

  public final void setBillPaymentToken(String billPaymentToken) {
    this.put(Fields.BILL_PAYMENT_TOKEN, billPaymentToken);
  }

  public final String getBillerId() {
    return (String) this.get(Fields.BILLER_ID);
  }

  public final void setBillerId(String billerId) {
    this.put(Fields.BILLER_ID, billerId);
  }

  public final String getMerchantRefNo() {
    return (String) this.get(Fields.MERCHANT_REFERENCE_NUMBER);
  }

  public final void setMerchantRefNo(String merchantRefNo) {
    this.put(Fields.MERCHANT_REFERENCE_NUMBER, merchantRefNo);
  }

  public final String getApprovalRefNoRefNo() {
    return (String) this.get(Fields.APPROVAL_REFERENCE_NUMBER);
  }

  public final void setApprovalRefNo(String approvalRefNo) {
    this.put(Fields.APPROVAL_REFERENCE_NUMBER, approvalRefNo);
  }

  public final List<Details> getAdditionalInformation() {
    return (List<Details>) this.get(Fields.ADDITIONAL_INFORMATION);
  }

  public final void setAdditionalInformation(List<Details> additionalInformation) {
    this.put(Fields.ADDITIONAL_INFORMATION, additionalInformation);
  }
  
  public final String getStateCode() {
	return (String) this.get(Fields.STATE_CODE);
  }

  public final void setStatusCode(String stateCode) {
	this.put(Fields.STATE_CODE, stateCode);
  }

	public final String getResponseError() {
		return (String) this.get(Fields.RESPONSE_ERROR);
	}

	public final void setResponseError(String responseError) {
		this.put(Fields.RESPONSE_ERROR, responseError);
	}
}
