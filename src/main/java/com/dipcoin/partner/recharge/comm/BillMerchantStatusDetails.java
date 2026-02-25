package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.List;

public class BillMerchantStatusDetails extends HashMap<String, Object> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static interface Fields {
    public static String MERCHANT_CODE = "MerchantCode";
    public static String MERCHANT_REFERENCE_NUMBER = "MerchantRefNo";
    public static String USER_NAME = "UserName";
    public static String USER_PASS = "UserPass";
    public static String AGENT_ID = "AgentId";
    public static String CHANNEL_DETAILS = "ChannelDetails";
    public static String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
    public static String CONSUMER_NUMBER = "ConsumerNo";
    public static String TO_DATE = "ToDate";
    public static String FROM_DATE = "FromDate";
    public static String ACTUAL_MERCHANT_REFERENCE_NUMBER = "ActualMerchantRefNo";
    public static String PAYMENT_REFERENCE_NUMBER = "PaymentRefNo";
    public static String OPERATOR_CUSTOMER_CONVENIENCE_FEE = "COUcustConvFee";
    public static String REQUEST_TYPE = "RequestType"; //epay 3.0
    public static String REQUESTER_IP = "RequesterIP"; //epay 3.0
    public static String STORE_CODE = "StoreCode"; //epay 3.0
  }

  public final String getEnRefNo() {
    return (String) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEnRefNo(String enRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, enRefNo);
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

  public final Object getActualMerchantRefNo() {
    return this.get(Fields.ACTUAL_MERCHANT_REFERENCE_NUMBER);
  }

  public final void setActualMerchantRefNo(String actualMerchantRefNo) {
    this.put(Fields.ACTUAL_MERCHANT_REFERENCE_NUMBER, actualMerchantRefNo);
  }

  public final ChannelDetails getChannelDetails() {
    return (ChannelDetails) this.get(Fields.CHANNEL_DETAILS);
  }

  public final void setChannelDetails(ChannelDetails channelDetails) {
    this.put(Fields.CHANNEL_DETAILS, channelDetails);
  }

  public final Object getConsumerNo() {
    return this.get(Fields.CONSUMER_NUMBER);
  }

  public final void setConsumerNo(String consumerNo) {
    this.put(Fields.CONSUMER_NUMBER, consumerNo);
  }

  public final String getCouCustConvFee() {
    return (String) this.get(Fields.OPERATOR_CUSTOMER_CONVENIENCE_FEE);
  }

  public final void setCouCustConvFee(String couCustConvFee) {
    this.put(Fields.OPERATOR_CUSTOMER_CONVENIENCE_FEE, couCustConvFee);
  }

  public final Object getToDate() {
    return this.get(Fields.TO_DATE);
  }

  public final void setToDate(String toDate) {
    this.put(Fields.TO_DATE, toDate);
  }

  public final Object getFromDate() {
    return this.get(Fields.FROM_DATE);
  }

  public final void setFromDate(String fromDate) {
    this.put(Fields.FROM_DATE, fromDate);
  }

   public final Object getPaymentReferenceNumber() {
   return this.get(Fields.PAYMENT_REFERENCE_NUMBER);
   }
  
   public final void setPaymentReferenceNumber(String paymentReferenceNumber) {
   this.put(Fields.PAYMENT_REFERENCE_NUMBER, paymentReferenceNumber);
   }

  public final Object getAgentId() {
    return this.get(Fields.AGENT_ID);
  }

  public final void setAgentId(String agentId) {
    this.put(Fields.AGENT_ID, agentId);
  }

  public final void setRequesterIp(String requesterIp) {
	  this.put(Fields.REQUESTER_IP, requesterIp);
  }

  public final String getRequesterIp() {
	  return (String) this.get(Fields.REQUESTER_IP);
  }
  
  public final void setStoreCode(String storeCode) {
	  this.put(Fields.STORE_CODE, storeCode);
  }

  public final String getStoreCode() {
	  return (String) this.get(Fields.STORE_CODE);
  }
  
  
  public final void setRequestType(String requestType) {
	  this.put(Fields.REQUEST_TYPE, requestType);
  }

  public final String getRequestType() {
	  return (String) this.get(Fields.REQUEST_TYPE);
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

}
