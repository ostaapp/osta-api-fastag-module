package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BillMerchantServiceDetails extends HashMap<String, Object> {
  public static final Logger LOG = LogManager.getLogger(BillMerchantServiceDetails.class);
  public static ObjectMapper objectMapper = new ObjectMapper();


  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static interface Fields {
    public static String REQUEST_TYPE = "RequestType";
    public static String REQUESTER_IP = "RequesterIP";
    public static String MERCHANT_CODE = "MerchantCode";
    public static String MERCHANT_REFERENCE_NUMBER = "MerchantRefNo";
    public static String USER_NAME = "UserName";
    public static String USER_PASS = "UserPass";
    public static String AGENT_ID = "AgentId";
    public static String BILLER_ID = "BillerId";
    public static String AMOUNT = "Amount";
    public static String STORE_CODE = "StoreCode";
    public static String OFF_US_PAY = "OFFUSPay";
    public static String LOGIN_NAME = "LoginName";
    public static String HASH = "Hash";
    public static String CHANNEL_DETAILS = "ChannelDetails";
    public static String CUSTOMER_PROFILE = "CustomerProfile";
    public static String SUBSCRIPTION_DETAILS = "SubscriptionDetails";
    public static String BILL_DETAIL = "BillDetail";
    public static String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
    public static String BILL_PAYMENT_TOKEN = "BillPaymentToken";
    public static String ADDITIONAL_INFORMATION = "AdditionalInformation";
    public static String OPERATOR_CUSTOMER_CONVENIENCE_FEE = "COUcustConvFee";
    public static String SPLIT_PAY = "SplitPay";
    public static String SPLIT_PAY_AMOUNT = "SplitPayAmount";
    public static String PAYMENT_INFORMATION = "PaymentInformation";
    public static String IS_BILL_VALIDATED = "IsBillValidated";
    public static String CUSTOMER_CONVINENCE_FEE ="CustConvFee";
    public static String PAYMENT_REF_NO="PaymentRefNo";

  }

  public final BillDetail getBillDetail() {
    return (BillDetail) this.get(Fields.BILL_DETAIL);
  }

  public final void setBillDetail(BillDetail billDetail) {
    this.put(Fields.BILL_DETAIL, billDetail);
  }

  public final String getEnRefNo() {
    return (String) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEnRefNo(String enRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, enRefNo);
  }

  public final String getIsBillValidated() {
    return (String) this.get(Fields.IS_BILL_VALIDATED);
  }

  public final void setIsBillValidated(String enRefNo) {
    this.put(Fields.IS_BILL_VALIDATED, enRefNo);
  }

  public final String getBillPaymentToken() {
    return (String) this.get(Fields.BILL_PAYMENT_TOKEN);
  }

  public final void setBillPaymentToken(String billPaymentToken) {
    this.put(Fields.BILL_PAYMENT_TOKEN, billPaymentToken);
  }

  public final Object getAdditionalInformation() {
    return this.get(Fields.ADDITIONAL_INFORMATION);
  }

  public final void setAdditionalInformation(List<Details> additionalInformation) {
    this.put(Fields.ADDITIONAL_INFORMATION, additionalInformation);
  }

  public final String getCustConvFee() {
    return (String) this.get(Fields.CUSTOMER_CONVINENCE_FEE);
  }

  public final void setCustConvFee(String custConvFee) {
    this.put(Fields.CUSTOMER_CONVINENCE_FEE, custConvFee);
  }
  
  public final String getCouCustConvFee() {
    return (String) this.get(Fields.OPERATOR_CUSTOMER_CONVENIENCE_FEE);
  }

  public final void setCouCustConvFee(String couCustConvFee) {
    this.put(Fields.OPERATOR_CUSTOMER_CONVENIENCE_FEE, couCustConvFee);
  }

  public final Object getRequestType() {
    return this.get(Fields.REQUEST_TYPE);
  }

  public final void setRequestType(String requestType) {
    this.put(Fields.REQUEST_TYPE, requestType);
  }

  public final Object getAgentId() {
    return this.get(Fields.AGENT_ID);
  }

  public final void setAgentId(String agentId) {
    this.put(Fields.AGENT_ID, agentId);
  }

  public final Object getBillerId() {
    return this.get(Fields.BILLER_ID);
  }

  public final void setBillerId(String billerId) {
    this.put(Fields.BILLER_ID, billerId);
  }

  public final Object getCustomerProfiles() {
    return this.get(Fields.CUSTOMER_PROFILE);
  }

  public final void setCustomerProfiles(List<Details> customerProfiles) {
    this.put(Fields.CUSTOMER_PROFILE, customerProfiles);
  }

  public final Object getSubscriptionDetails() {
    return this.get(Fields.SUBSCRIPTION_DETAILS);
  }

  public final void setSubscriptionDetails(List<Details> subscriptionDetails) {
    this.put(Fields.SUBSCRIPTION_DETAILS, subscriptionDetails);
  }

  public final ChannelDetails getChannelDetails() {
    return (ChannelDetails) this.get(Fields.CHANNEL_DETAILS);
  }

  public final void setChannelDetails(ChannelDetails channelDetails) {
    this.put(Fields.CHANNEL_DETAILS, channelDetails);
  }

  public final Object getMerchantCode() {
    return this.get(Fields.MERCHANT_CODE);
  }

  public final void setMerchantCode(String merchantCode) {
    this.put(Fields.MERCHANT_CODE, merchantCode);
  }

  public final Object getMerchantRefNo() {
    return this.get(Fields.MERCHANT_REFERENCE_NUMBER);
  }

  public final void setMerchantRefNo(String merchantRefNo) {
    this.put(Fields.MERCHANT_REFERENCE_NUMBER, merchantRefNo);
  }

  public final Object getStoreCode() {
    return this.get(Fields.STORE_CODE);
  }

  public final void setStoreCode(String storeCode) {
    this.put(Fields.STORE_CODE, storeCode);
  }

  public final Object getUserName() {
    return this.get(Fields.USER_NAME);
  }

  public final void setUserName(String userName) {
    this.put(Fields.USER_NAME, userName);
  }

  public final Object getUserPass() {
    return this.get(Fields.USER_PASS);
  }

  public final void setUserPass(String userPass) {
    this.put(Fields.USER_PASS, userPass);
  }

  public final String getRequesterIP() {
    return (String) this.get(Fields.REQUESTER_IP);
  }

  public final void setRequesterIP(String requesterIP) {
    this.put(Fields.REQUESTER_IP, requesterIP);
  }

  public final Object getAmount() {
    return this.get(Fields.AMOUNT);
  }

  public final void setAmount(String Amount) {
    this.put(Fields.AMOUNT, Amount);
  }

  public final Object getHash() {
    return this.get(Fields.HASH);
  }

  public final void setHash(String hash) {
    this.put(Fields.HASH, hash);
  }

  public final Object getOffUsPay() {
    return this.get(Fields.OFF_US_PAY);
  }

  public final void setOffUsPay(String offUsPay) {
    this.put(Fields.OFF_US_PAY, offUsPay);
  }

  public final Object getLogin() {
    return this.get(Fields.LOGIN_NAME);
  }

  public final void setLogin(String login) {
    this.put(Fields.LOGIN_NAME, login);
  }

  public final Object getSpiltPayAmount() {
    return this.get(Fields.SPLIT_PAY_AMOUNT);
  }

  public final void setSpiltPayAmount(String spiltAmount) {
    this.put(Fields.SPLIT_PAY_AMOUNT, spiltAmount);
  }

  public final Object getSpiltPay() {
    return this.get(Fields.SPLIT_PAY);
  }

  public final void setSpiltPay(String spiltPay) {
    this.put(Fields.SPLIT_PAY, spiltPay);
  }

  public final PaymentInformation getPaymentInformation() {
    return (PaymentInformation) this.get(Fields.PAYMENT_INFORMATION);
  }

  public final void setPaymentInformation(PaymentInformation paymentInformation) {
    this.put(Fields.PAYMENT_INFORMATION, paymentInformation);
  }
  
  public final String getPaymentRefId() {
    return (String) this.get(Fields.PAYMENT_REF_NO);
  }

  public final void setPaymentRefId(String paymentRefNo) {
    this.put(Fields.PAYMENT_REF_NO, paymentRefNo);
  }

  public static class ChannelDetails extends HashMap<String, Object> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static interface Fields {
      public static String CHANNELCODE = "ChannelCode";
      public static String CHANNELPARAMS = "ChannelParams";
    }

    public String getChannelCode() {
      return (String) this.get(Fields.CHANNELCODE);
    }

    public void setChannelCode(String channelCode) {
      this.put(Fields.CHANNELCODE, channelCode);
    }

    @SuppressWarnings("unchecked")
    public List<Details> getChannelParams() {
      return (List<Details>) this.get(Fields.CHANNELPARAMS);
    }

    public void setChannelParams(List<Details> channelParams) {
      this.put(Fields.CHANNELPARAMS, channelParams);
    }
  }

  public static class PaymentInformation extends HashMap<String, Object> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static interface Fields {
      public static String PAYMENTMODE = "PaymentMode";
      public static String PAYMENTPARAMS = "PaymentParams";
    }

    public String getPaymentMode() {
      return (String) this.get(Fields.PAYMENTMODE);
    }

    public void setPaymentMode(String paymentMode) {
      this.put(Fields.PAYMENTMODE, paymentMode);
    }

    @SuppressWarnings("unchecked")
    public List<Details> getPaymentParams() {
      return (List<Details>) this.get(Fields.PAYMENTPARAMS);
    }

    public void setPaymentParams(List<Details> paymentParams) {
      this.put(Fields.PAYMENTPARAMS, paymentParams);
    }
  }

  public BillMerchantServiceDetails deepCopy() {
    return SerializationUtils.clone(this);
  }

  @Override
  public String toString() {
    try {
      LOG.debug(objectMapper.writeValueAsString(this));
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
    }

    return null;
  }
}
