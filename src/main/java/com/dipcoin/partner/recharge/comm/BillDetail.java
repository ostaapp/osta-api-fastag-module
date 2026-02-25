package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.List;

public class BillDetail extends HashMap<String, Object> {


  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public BillDetail() {}


  /**
   * 
   */
  public static interface Fields extends BillResponse.Fields {
    public static String ADDITIONAL_AMOUNT = "AdditionalAmount";
    public static String CUSTOMER_NAME = "CustomerName";
    public static String AMOUNT = "Amount";
    public static String DUE_DATE = "DueDate";
    public static String CUSTOMER_CONVENIENCE_FEE = "CustConvFee";
    public static String CUSTOMER_CONVENIENCE_DESCRIPTION = "CustConvDesc";
    public static String BILL_DATE = "BillDate";
    public static String BILL_NUMBER = "BillNumber";
    public static String BILL_PERIOD = "BillPeriod";
    public static String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
    public static String BILL_PAYMENT_TOKEN = "BillPaymentToken";
  }

  @SuppressWarnings("unchecked")
  public final List<Details> getAdditionalAmount() {
    return (List<Details>) this.get(Fields.ADDITIONAL_AMOUNT);
  }

  public final void setAdditionalAmount(List<Details> additionalAmount) {
    this.put(Fields.ADDITIONAL_AMOUNT, additionalAmount);
  }

  public final String getCustomerName() {
    return (String) this.get(Fields.CUSTOMER_NAME);
  }

  public final void setCustomerName(String customerName) {
    this.put(Fields.CUSTOMER_NAME, customerName);
  }

  public final String getDueDate() {
    return (String) this.get(Fields.DUE_DATE);
  }

  public final void setDueDate(String dudeDate) {
    this.put(Fields.DUE_DATE, dudeDate);
  }

  public final String getCustConvFee() {
    return (String) this.get(Fields.CUSTOMER_CONVENIENCE_FEE);
  }

  public final void setCustConvFee(String custConvFee) {
    this.put(Fields.CUSTOMER_CONVENIENCE_FEE, custConvFee);
  }

  public final String getCustConvDesc() {
    return (String) this.get(Fields.CUSTOMER_CONVENIENCE_DESCRIPTION);
  }

  public final void setCustConvDesc(String custConvDesc) {
    this.put(Fields.CUSTOMER_CONVENIENCE_DESCRIPTION, custConvDesc);
  }

  public final String getBillDate() {
    return (String) this.get(Fields.BILL_DATE);
  }

  public final void setBillDate(String billDate) {
    this.put(Fields.BILL_DATE, billDate);
  }

  public final String getBillNo() {
    return (String) this.get(Fields.BILL_NUMBER);
  }

  public final void setBillNo(String billNumber) {
    this.put(Fields.BILL_NUMBER, billNumber);
  }

  public final String getBillPeriod() {
    return (String) this.get(Fields.BILL_PERIOD);
  }

  public final void setBillPeriod(String billPeriod) {
    this.put(Fields.BILL_PERIOD, billPeriod);
  }

  public final String getAmount() {
    return (String) this.get(Fields.AMOUNT);
  }

  public final void setAmount(String amount) {
    this.put(Fields.AMOUNT, amount);
  }

  public final String getBillPaymentToken() {
    return (String) this.get(Fields.BILL_PAYMENT_TOKEN);
  }

  public final void setBillPaymentToken(String billPaymentToken) {
    this.put(Fields.BILL_PAYMENT_TOKEN, billPaymentToken);
  }

  public final String getEuronetRefNo() {
    return (String) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEuronetRefNo(String euronetRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, euronetRefNo);
  }
}
