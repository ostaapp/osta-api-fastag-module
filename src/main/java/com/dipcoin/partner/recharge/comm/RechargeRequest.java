package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;
import com.dipcoin.partner.recharge.services.RechargeServicesImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class RechargeRequest extends HashMap<String, String> {


  public static final Logger LOG = LogManager.getLogger(RechargeServicesImpl.class);
  public static ObjectMapper objectMapper = new ObjectMapper();
  private static final long serialVersionUID = 1L;

  public static interface Fields {
    public static String AMOUNT = "Amount";
    public static String CHANNEL_CODE = "ChannelCode";
    public static String CONSUMER_NUMBER = "ConsumerNo";
    public static String CUSTOMER_ID = "CustomerID";
    public static String EXTERNAL_DATA_ONE = "ExternalData1";
    public static String EXTERNAL_DATA_TWO = "ExternalData2";
    public static String EXTERNAL_DATA_THREE = "ExternalData3";
    public static String MERCHANT_CODE = "MerchantCode";
    public static String MERCHANT_REFERENCE_NUMBER = "MerchantRefNo";
    public static String REQUESTER_IP = "RequesterIP";
    public static String SERVICE_CODE = "ServiceCode";
    public static String SERVICE_PROVIDER_CODE = "SpCode";
    public static String CIRCLE = "Circle";
    public static String SERVICE_PROVIDER_AND_CIRCLE_CODE = "SspCode";
    public static String STORE_CODE = "StoreCode";
    public static String USER_NAME = "UserName";
    public static String USER_PASS = "UserPass";
    public static String AGENT_ID = "AgentId";
    public static String BILLER_ID = "BillerId";
    public static String REQUEST_TYPE = "RequestType";
    public static String SOURCE = "Source";
    public static String PARTNER_TRANSACTION_REFERENCE_ID = "partnerTransRefId";
  }

  public OperationType operationType;
  public String merchantReferenceId;

  public RechargeRequest(String merchantRefId, OperationType operationType) {
    this.operationType = operationType;
    this.merchantReferenceId = merchantRefId;
  }

  final public String getMerchantReferenceId() {
    return this.merchantReferenceId;
  }

  public final String getAmount() {
    return this.get(Fields.AMOUNT);
  }

  public final void setAmount(String Amount) {
    this.put(Fields.AMOUNT, Amount);
  }

  public final OperationType getOperationType() {
    return this.operationType;
  }

  public final String getRequestType() {
    return this.get(Fields.REQUEST_TYPE);
  }

  public final void setRequestType(String requestType) {
    this.put(Fields.REQUEST_TYPE, requestType);
  }

  public final String getChannelCode() {
    return this.get(Fields.CHANNEL_CODE);
  }

  public final void setChannelCode(String channelCode) {
    this.put(Fields.CHANNEL_CODE, channelCode);
  }

  public final String getConsumerNo() {
    return this.get(Fields.CONSUMER_NUMBER);
  }

  public final void setConsumerNo(String consumerNo) {
    this.put(Fields.CONSUMER_NUMBER, consumerNo);
  }

  public final String getCustomerID() {
    return this.get(Fields.CUSTOMER_ID);
  }

  public final void setCustomerID(String customerID) {
    this.put(Fields.CUSTOMER_ID, customerID);
  }

  public final String getExternalData1() {
    return this.get(Fields.EXTERNAL_DATA_ONE);
  }

  public final void setExternalData1(String externalData1) {
    this.put(Fields.EXTERNAL_DATA_ONE, externalData1);
  }

  public final String getExternalData2() {
    return this.get(Fields.EXTERNAL_DATA_TWO);
  }

  public final void setExternalData2(String externalData2) {
    this.put(Fields.EXTERNAL_DATA_TWO, externalData2);
  }

  public final String getExternalData3() {
    return this.get(Fields.EXTERNAL_DATA_THREE);
  }

  public final void setExternalData3(String externalData3) {
    this.put(Fields.EXTERNAL_DATA_THREE, externalData3);
  }

  public final String getMerchantCode() {
    return this.get(Fields.MERCHANT_CODE);
  }

  public final void setMerchantCode(String merchantCode) {
    this.put(Fields.MERCHANT_CODE, merchantCode);
  }

  public final String getMerchantRefNo() {
    return this.get(Fields.MERCHANT_REFERENCE_NUMBER);
  }

  public final void setMerchantRefNo(String merchantRefNo) {
    this.put(Fields.MERCHANT_REFERENCE_NUMBER, merchantRefNo);
  }

  public final String getRequesterIP() {
    return this.get(Fields.EXTERNAL_DATA_THREE);
  }

  public final void setRequesterIP(String requesterIP) {
    this.put(Fields.REQUESTER_IP, requesterIP);
  }

  public final String getServiceCode() {
    return this.get(Fields.SERVICE_CODE);
  }

  public final void setServiceCode(String serviceCode) {
    this.put(Fields.SERVICE_CODE, serviceCode);
  }

  public final String getAgentId() {
    return this.get(Fields.AGENT_ID);
  }

  public final void setAgentId(String agentId) {
    this.put(Fields.AGENT_ID, agentId);
  }

  public final void setSource(String source) {
    this.put(Fields.SOURCE, source);
  }

  public final String getSource() {
    return this.get(Fields.SOURCE);
  }

  public final void setBillerId(String billerId) {
    this.put(Fields.BILLER_ID, billerId);
  }

  public final String getBillerId() {
    return this.get(Fields.BILLER_ID);
  }

  public final String getSpCode() {
    return this.get(Fields.SERVICE_PROVIDER_CODE);
  }

  public final void setSpCode(String spCode) {
    this.put(Fields.SERVICE_PROVIDER_CODE, spCode);
  }

  public final String getCircle() {
    return this.get(Fields.CIRCLE);
  }

  public final void setCircle(String circle) {
    this.put(Fields.CIRCLE, circle);
  }

  public final String getSspCode() {
    return this.get(Fields.SERVICE_PROVIDER_AND_CIRCLE_CODE);
  }

  public final void setSspCode(String sspCode) {
    this.put(Fields.SERVICE_PROVIDER_AND_CIRCLE_CODE, sspCode);
  }

  public final String getStoreCode() {
    return this.get(Fields.STORE_CODE);
  }

  public final void setStoreCode(String storeCode) {
    this.put(Fields.STORE_CODE, storeCode);
  }

  public final String getUserName() {
    return this.get(Fields.USER_NAME);
  }

  public final void setUserName(String userName) {
    this.put(Fields.USER_NAME, userName);
  }

  public final String getUserPass() {
    return this.get(Fields.USER_PASS);
  }

  public final void setUserPass(String userPass) {
    this.put(Fields.USER_PASS, userPass);
  }

  public final String getPartnerTransRefId() {
    return this.get(Fields.PARTNER_TRANSACTION_REFERENCE_ID);
  }

  public final void setPartnerTransRefId(String partnerTransRefId) {
    this.put(Fields.PARTNER_TRANSACTION_REFERENCE_ID, partnerTransRefId);
  }

  public RechargeRequest deepCopy() {
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
