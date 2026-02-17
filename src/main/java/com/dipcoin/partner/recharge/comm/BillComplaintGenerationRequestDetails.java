package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.List;

public class BillComplaintGenerationRequestDetails extends HashMap<String, Object> {

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
    public static String COMPLAINT_TYPE = "ComplaintType";
    public static String DESCRIPTION = "Description";
    public static String PAYMENT_REFERENCE_NUMBER = "PaymentRefNo";
    public static String BILLER_ID = "BillerId";
    public static String DISPOSITION = "Disposition";
    public static String COMPLAINT_REASON = "ComplaintReason";
    public static String REQUESTER_IP = "RequesterIP";
    public static String PARTICIPATION_TYPE = "ParticipationType";
    public static String CUSTOMER_PROFILE = "CustomerProfile";
    public static String STORE_CODE = "StoreCode";
    public static String EURONET_REF_NO = "EuronetRefNo"; //epay3.0
    public static String ACTUAL_MERCHANT_REF_NO = "ActualMerchantRefNo"; //epay3.0
  }

  public final String getComplaintType() {
    return (String) this.get(Fields.COMPLAINT_TYPE);
  }

  public final void setComplaintType(String complaintType) {
    this.put(Fields.COMPLAINT_TYPE, complaintType);
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

  public final Object getCustomerProfiles() {
    return this.get(Fields.CUSTOMER_PROFILE);
  }

  public final void setCustomerProfiles(List<Details> customerProfiles) {
    this.put(Fields.CUSTOMER_PROFILE, customerProfiles);
  }

  public final Object getMerchantRefNo() {
    return this.get(Fields.MERCHANT_REFERENCE_NUMBER);
  }

  public final void setMerchantRefNo(String merchantRefNo) {
    this.put(Fields.MERCHANT_REFERENCE_NUMBER, merchantRefNo);
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

  public final ChannelDetails getChannelDetails() {
    return (ChannelDetails) this.get(Fields.CHANNEL_DETAILS);
  }

  public final void setChannelDetails(ChannelDetails channelDetails) {
    this.put(Fields.CHANNEL_DETAILS, channelDetails);
  }

  public final Object getComplaintReason() {
    return this.get(Fields.COMPLAINT_REASON);
  }

  public final void setComplaintReason(String complaintReason) {
    this.put(Fields.COMPLAINT_REASON, complaintReason);
  }

  public final String getParticipationType() {
    return (String) this.get(Fields.PARTICIPATION_TYPE);
  }

  public final void setParticipationType(String participationType) {
    this.put(Fields.PARTICIPATION_TYPE, participationType);
  }

  public final String getRequesterIP() {
    return (String) this.get(Fields.REQUESTER_IP);
  }

  public final void setRequesterIP(String requesterIP) {
    this.put(Fields.REQUESTER_IP, requesterIP);
  }

  public final String getPaymentReferenceNumber() {
    return (String) this.get(Fields.PAYMENT_REFERENCE_NUMBER);
  }

  public final void setPaymentReferenceNumber(String paymentReferenceNumber) {
    this.put(Fields.PAYMENT_REFERENCE_NUMBER, paymentReferenceNumber);
  }

  public final Object getStoreCode() {
    return this.get(Fields.STORE_CODE);
  }

  public final void setStoreCode(String storeCode) {
    this.put(Fields.STORE_CODE, storeCode);
  }

  public final String getDescription() {
    return (String) this.get(Fields.DESCRIPTION);
  }

  public final void setDescription(String description) {
    this.put(Fields.DESCRIPTION, description);
  }

  public final String getDisposition() {
    return (String) this.get(Fields.DISPOSITION);
  }

  public final void setDisposition(String disposition) {
    this.put(Fields.DISPOSITION, disposition);
  }
  
  public final void setEuronetRefNo(String euronetRefNo) {
	  this.put(Fields.EURONET_REF_NO, euronetRefNo);
  }
  
  public final String getEuronetRefNo() {
	 return  (String) this.get(Fields.EURONET_REF_NO);
  }

  public final void setActualMerchantRefNo(String actualMerchantRefNo) {
	  this.put(Fields.ACTUAL_MERCHANT_REF_NO, actualMerchantRefNo);
  }
  
  public final String getActualMerchant() {
	  return (String) this.get(Fields.ACTUAL_MERCHANT_REF_NO);
  }

  public static class ChannelDetails extends HashMap<String, Object> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static interface Fields {
      public static String CHANNEL_CODE = "ChannelCode";
      public static String CHANNEL_PARAMS = "ChannelParams";
    }

    public String getChannelCode() {
      return (String) this.get(Fields.CHANNEL_CODE);
    }

    public void setChannelCode(String channelCode) {
      this.put(Fields.CHANNEL_CODE, channelCode);
    }

    @SuppressWarnings("unchecked")
    public List<Details> getChannelParams() {
      return (List<Details>) this.get(Fields.CHANNEL_PARAMS);
    }

    public void setChannelParams(List<Details> channelParams) {
      this.put(Fields.CHANNEL_PARAMS, channelParams);
    }
  }
}
