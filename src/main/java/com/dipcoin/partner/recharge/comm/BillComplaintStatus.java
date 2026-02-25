package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.List;

public class BillComplaintStatus extends HashMap<String, Object> {

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
    public static String REQUESTER_IP = "RequesterIP";
    public static String COMPLAINT_ID = "ComplaintId";
    public static String REQUEST_TYPE = "RequestType"; //by me
    public static String EURONET_REF_NO = "EuronetRefNo"; // by me
  }

  public final String getComplaintId() {
    return (String) this.get(Fields.COMPLAINT_ID);
  }

  public final void setComplaintId(String complaintId) {
    this.put(Fields.COMPLAINT_ID, complaintId);
  }

  public final String getComplaintType() {
    return (String) this.get(Fields.COMPLAINT_TYPE);
  }

  public final void setComplaintType(String complaintType) {
    this.put(Fields.COMPLAINT_TYPE, complaintType);
  }

  public final String getUserName() {
    return (String) this.get(Fields.USER_NAME);
  }

  public final void setUserName(String userName) {
    this.put(Fields.USER_NAME, userName);
  }

  public final String getUserPass() {
    return (String) this.get(Fields.USER_PASS);
  }

  public final void setUserPass(String userPass) {
    this.put(Fields.USER_PASS, userPass);
  }

  public final String getMerchantCode() {
    return (String) this.get(Fields.MERCHANT_CODE);
  }

  public final void setMerchantCode(String merchantCode) {
    this.put(Fields.MERCHANT_CODE, merchantCode);
  }

  public final String getMerchantRefNo() {
    return (String) this.get(Fields.MERCHANT_REFERENCE_NUMBER);
  }

  public final void setMerchantRefNo(String merchantRefNo) {
    this.put(Fields.MERCHANT_REFERENCE_NUMBER, merchantRefNo);
  }

  public final String getAgentId() {
    return (String) this.get(Fields.AGENT_ID);
  }

  public final void setAgentId(String agentId) {
    this.put(Fields.AGENT_ID, agentId);
  }

  public final ChannelDetails getChannelDetails() {
    return (ChannelDetails) this.get(Fields.CHANNEL_DETAILS);
  }

  public final void setChannelDetails(ChannelDetails channelDetails) {
    this.put(Fields.CHANNEL_DETAILS, channelDetails);
  }

  public final String getRequesterIP() {
    return (String) this.get(Fields.REQUESTER_IP);
  }

  public final void setRequesterIP(String requesterIP) {
    this.put(Fields.REQUESTER_IP, requesterIP);
  }
  
  public final String getRequestType() {
	return (String) this.get(Fields.REQUEST_TYPE);
  }

  public final void setRequestType(String requestType) {
	this.put(Fields.REQUEST_TYPE, requestType);
  }
  
  public final String getEuronetRefNo() {
	return (String) this.get(Fields.EURONET_REF_NO);
  }

  public final void setEuronetRefNo(String euronetRefNo) {
	this.put(Fields.EURONET_REF_NO, euronetRefNo);
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
