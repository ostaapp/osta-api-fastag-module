package com.dipcoin.bank.services.comm;

import java.util.LinkedHashMap;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import com.dipcoin.bank.services.client.BankClient.Operation;
import com.dipcoin.bank.services.comm.DipcoinResponse.Fields;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class DipcoinRequest extends LinkedHashMap<String, String> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static ObjectMapper objectMapper = new ObjectMapper();

  // list of base fields associated with all requests
  public static interface Fields {
    public static String BANK_CODE = "bankID"; // bankCode <==> bankID
    public static String DIPCOIN_REFERENCE_NUMBER = "dipcoinReferenceNumber";
    public static String TRANSACTION_TIME = "transactionTime";
    public static String TRANSACTION_TYPE = "transactionType";
    public static String ATTEMPT = "attempt";
    public static final String USER_ID = "userId";
    public static final String CARD_ID = "cardId";
    // Header fields
    public static String HEADER_REQUESTUUID = "headerRequestUUID";
    public static String HEADER_SERVICEREQUESTID = "ServiceRequestId";
    public static String HEADER_SERVICEREQUESTVERSION = "ServiceRequestVersion";
    public static String HEADER_CHANNELID = "ChannelId";
    public static String HEADER_LANGUAGEID = "headerLanguageId";
    public static String HEADER_BANKID = "headerBankId";
    public static String HEADER_TIMEZONE = "headerTimeZone";
    public static String HEADER_ENTITYID = "headerEntityId";
    public static String HEADER_REQUEST_CERT_TOKEN = "FICertToken";
    public static String HEADER_ENTITYTYPE = "headerEntityType";
    public static String HEADER_ARMCORRELATIONID = "headerArmCorrelationId";
    public static String HEADER_MESSAGEDATETIME = "headerMessageDateTime";
    public static String HEADER_PASSWORDTOKEN_USERID = "headerPasswordToken_UserId";
    public static String HEADER_PASSWORDTOKEN_PASSWORD = "headerPasswordToken_Password";
    public static String HEADER_REALUSERLOGINSESSIONID = "headerRealUserLoginSessionId";
    public static String HEADER_REALUSER = "headerRealUser";
    public static String HEADER_REALUSERPWD = "headerRealUserPwd";
    public static String HEADER_SSOTRANSFERTOKEN = "headerSSOTransferToken";
  }

  private String bankReferenceId;

  private Operation operation;

  public DipcoinRequest(String bankRefId, String bankCode, Operation operation) {
    this.bankReferenceId = bankRefId;
    this.put(Fields.BANK_CODE, bankCode);
    // default no operation
    this.operation = operation;
  }

  final public String getBankReferenceId() {
    return this.bankReferenceId;
  }
  
  final public void setBankReferenceId(String bankReferenceId) {
    this.bankReferenceId = bankReferenceId;
  }

  final public String getBankCode() {
    return this.get(Fields.BANK_CODE);
  }

  final public void setDipcoinReferenceNumber(String dipcoinReferenceNumber) {
    this.put(Fields.DIPCOIN_REFERENCE_NUMBER, dipcoinReferenceNumber);
  }

  final public String getDipcoinReferenceNumber() {
    return this.get(Fields.DIPCOIN_REFERENCE_NUMBER);
  }

  final public void setTransactionTime(String transactionTime) {
    this.put(Fields.TRANSACTION_TIME, transactionTime);
  }

  final public String getTransactionTime() {
    return this.get(Fields.TRANSACTION_TIME);
  }

  final public void setTransactionType(String transactionType) {
    this.put(Fields.TRANSACTION_TYPE, transactionType);
  }

  final public String getTransactionType() {
    return this.get(Fields.TRANSACTION_TYPE);
  }

  // @NOTE - keeping this protected so its accessible only in this sub-package
  final protected void setAttempt(String attempt) {
    this.put(Fields.ATTEMPT, attempt);
  }

  final public String getAttempt() {
    return this.get(Fields.ATTEMPT);
  }
  
  public String getUserId() {
    return this.get(Fields.USER_ID);
  }

  public void setUserId(String userId) {
    this.put(Fields.USER_ID, userId);
  }
  
  public String getCardId() {
    return this.get(Fields.CARD_ID);
  }

  public void setCardId(String cardId) {
    this.put(Fields.CARD_ID, cardId);
  }
  
  final public void setHeaderRequestUUID(String headerRequestUUID) {
    this.put(Fields.HEADER_REQUESTUUID, headerRequestUUID);
  }

  final public String getHeaderRequestUUID() {
    return this.get(Fields.HEADER_REQUESTUUID);
  }

  final public void setHeaderServiceRequestId(String headerServiceRequestId) {
    this.put(Fields.HEADER_SERVICEREQUESTID, headerServiceRequestId);
  }

  final public String getHeaderServiceRequestId() {
    return this.get(Fields.HEADER_SERVICEREQUESTID);
  }
  
  final public void setHeaderServiceRequestVersion(String headerServiceRequestVersion) {
    this.put(Fields.HEADER_SERVICEREQUESTVERSION, headerServiceRequestVersion);
  }

  final public String getHeaderServiceRequestVersion() {
    return this.get(Fields.HEADER_SERVICEREQUESTVERSION);
  }

  final public void setHeaderChannelId(String headerChannelId) {
    this.put(Fields.HEADER_CHANNELID, headerChannelId);
  }

  final public String getHeaderChannelId() {
    return this.get(Fields.HEADER_CHANNELID);
  }

  final public void setHeaderLanguageId(String headerLanguageId) {
    this.put(Fields.HEADER_LANGUAGEID, headerLanguageId);
  }

  final public String getHeaderLanguageId() {
    return this.get(Fields.HEADER_LANGUAGEID);
  }

  final public void setHeaderBankId(String headerBankId) {
    this.put(Fields.HEADER_BANKID, headerBankId);
  }

  final public String getHeaderBankId() {
    return this.get(Fields.HEADER_BANKID);
  }

  final public void setHeaderTimeZone(String headerTimeZone) {
    this.put(Fields.HEADER_TIMEZONE, headerTimeZone);
  }

  final public String getHeaderTimeZone() {
    return this.get(Fields.HEADER_TIMEZONE);
  }

  final public void setHeaderEntityId(String headerEntityId) {
    this.put(Fields.HEADER_ENTITYID, headerEntityId);
  }

  final public String getHeaderEntityId() {
    return this.get(Fields.HEADER_ENTITYID);
  }

  final public void setFICertToken(String FICertToken) {
    this.put(Fields.HEADER_REQUEST_CERT_TOKEN, FICertToken);
  }

  final public String getFICertTokenFICertToken() {
    return this.get(Fields.HEADER_REQUEST_CERT_TOKEN);
  }

  final public void setHeaderEntityType(String headerEntityType) {
    this.put(Fields.HEADER_ENTITYTYPE, headerEntityType);
  }

  final public String getHeaderEntityType() {
    return this.get(Fields.HEADER_ENTITYTYPE);
  }
  final public void setHeaderArmCorrelationId(String headerArmCorrelationId) {
    this.put(Fields.HEADER_ARMCORRELATIONID, headerArmCorrelationId);
  }

  final public String getHeaderArmCorrelationId() {
    return this.get(Fields.HEADER_ARMCORRELATIONID);
  }
  final public void setHeaderMessageDateTime(String headerMessageDateTime) {
    this.put(Fields.HEADER_MESSAGEDATETIME, headerMessageDateTime);
  }

  final public String getHeaderMessageDateTime() {
    return this.get(Fields.HEADER_MESSAGEDATETIME);
  }

  final public void setHeaderPasswordToken_UserId(String headerPasswordToken_UserId) {
    this.put(Fields.HEADER_PASSWORDTOKEN_USERID, headerPasswordToken_UserId);
  }

  final public String getHeaderPasswordToken_UserId() {
    return this.get(Fields.HEADER_PASSWORDTOKEN_USERID);
  }

  final public void setHeaderPasswordToken_Password(String headerPasswordToken_Password) {
    this.put(Fields.HEADER_PASSWORDTOKEN_PASSWORD, headerPasswordToken_Password);
  }

  final public String getHeaderPasswordToken_Password() {
    return this.get(Fields.HEADER_PASSWORDTOKEN_PASSWORD);
  }

  final public void setHeaderRealUserLoginSessionId(String headerRealUserLoginSessionId) {
    this.put(Fields.HEADER_REALUSERLOGINSESSIONID, headerRealUserLoginSessionId);
  }

  final public String getHeaderRealUserLoginSessionId() {
    return this.get(Fields.HEADER_REALUSERLOGINSESSIONID);
  }

  final public void setHeaderRealUser(String headerRealUser) {
    this.put(Fields.HEADER_REALUSER, headerRealUser);
  }

  final public String getHeaderRealUser() {
    return this.get(Fields.HEADER_REALUSER);
  }

  final public void setHeaderRealUserPwd(String headerRealUserPwd) {
    this.put(Fields.HEADER_REALUSERPWD, headerRealUserPwd);
  }

  final public String getHeaderRealUserPwd() {
    return this.get(Fields.HEADER_REALUSERPWD);
  }

  final public void setHeaderSSOTransferToken(String headerSSOTransferToken) {
    this.put(Fields.HEADER_SSOTRANSFERTOKEN, headerSSOTransferToken);
  }

  final public String getHeaderSSOTransferToken() {
    return this.get(Fields.HEADER_SSOTRANSFERTOKEN);
  }

  final public Operation getOperation() {
    return this.operation;
  }

  @Override
  public String toString() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
    }

    return null;
  }

  public DipcoinRequest deepCopy() {
    return SerializationUtils.clone(this);
  }

  // method to validate the request
  public boolean validate() {
    if (getOperation() == null || StringUtils.isEmpty(this.getBankReferenceId())
        || StringUtils.isEmpty(this.getBankCode())
        || StringUtils.isEmpty(this.getDipcoinReferenceNumber())) {
      return false;
    }
    return true;
  }
}
