package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;

public class BillTransactionStatusDetail extends HashMap<String, Object> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public BillTransactionStatusDetail() {}

  public static interface Fields extends BillResponse.Fields {
    public static String ACTUAL_MERCHANT_REFERENCE_NUMBER = "ActualMerchantRefNo";
    public static String AMOUNT = "Amount";
    public static String CUSTOMER_NAME = "CustName";
    public static String TRANSACTION_DATE = "TxnDate";
    public static String TRANSACTION_STATUS = "TxnStatus";
    public static String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
    public static String PAYMENT_REFERENCE_NUMBER = "PaymentRefNo";
    public static String BILLER_ID = "BillerId";
    public static String AGENT_ID = "AgentId";
    public static String TRANSACTION_RESPONSE_CODE = "TxnResponseCode";
    public static String TRANSACTION_RESPONSE_MESSAGE = "TxnResponseMessage";
  }

  public final String getActualMerchantRefNo() {
    return (String) this.get(Fields.ACTUAL_MERCHANT_REFERENCE_NUMBER);
  }

  public final void setActualMerchantRefNo(String actualMerchantRefNo) {
    this.put(Fields.ACTUAL_MERCHANT_REFERENCE_NUMBER, actualMerchantRefNo);
  }

  public final String getCustomerName() {
    return (String) this.get(Fields.CUSTOMER_NAME);
  }

  public final void setCustomerName(String customerName) {
    this.put(Fields.CUSTOMER_NAME, customerName);
  }

  public final String getAmount() {
    return (String) this.get(Fields.AMOUNT);
  }

  public final void setAmount(String amount) {
    this.put(Fields.AMOUNT, amount);
  }

  public final String getEuronetRefNo() {
    return (String) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEuronetRefNo(String euronetRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, euronetRefNo);
  }

  public final String getPaymentReferenceNumber() {
    return (String) this.get(Fields.PAYMENT_REFERENCE_NUMBER);
  }

  public final void setPaymentReferenceNumber(String paymentReferenceNumber) {
    this.put(Fields.PAYMENT_REFERENCE_NUMBER, paymentReferenceNumber);
  }

  public final String getTransactionDate() {
    return (String) this.get(Fields.TRANSACTION_DATE);
  }

  public final void setTransactionDate(String transcationDate) {
    this.put(Fields.TRANSACTION_DATE, transcationDate);
  }

  public final String getTransactionResponseCode() {
    return (String) this.get(Fields.TRANSACTION_RESPONSE_CODE);
  }

  public final void setTransactionResponseCode(String transactionResponseCode) {
    this.put(Fields.TRANSACTION_RESPONSE_CODE, transactionResponseCode);
  }

  public final String getTransactionResponseMessage() {
    return (String) this.get(Fields.TRANSACTION_RESPONSE_MESSAGE);
  }

  public final void setTransactionResponseMessage(String transactionResponseMessage) {
    this.put(Fields.TRANSACTION_RESPONSE_MESSAGE, transactionResponseMessage);
  }

  public final String getTranscationStatus() {
    return (String) this.get(Fields.TRANSACTION_STATUS);
  }

  public final void setTranscationStatus(String transcationStatus) {
    this.put(Fields.TRANSACTION_STATUS, transcationStatus);
  }

  public final String getBillerId() {
    return (String) this.get(Fields.BILLER_ID);
  }

  public final void setBillerId(String billerId) {
    this.put(Fields.BILLER_ID, billerId);
  }

  public final String getAgentId() {
    return (String) this.get(Fields.AGENT_ID);
  }

  public final void setAgentId(String agentId) {
    this.put(Fields.AGENT_ID, agentId);
  }

}
