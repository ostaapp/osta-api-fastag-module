package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;

public class MDMBillers extends HashMap<String, Object> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static interface Fields {

    public static final String BILLER_ID = "billerId";
    public static final String BILLER_NAME = "billerName";
    public static final String BILLER_ALIAS_NAME = "billerAliasName";
    public static final String BILLER_CATEGORY_NAME = "billerCategoryName";
    public static final String BILLER_MODE = "billerMode";
    public static final String BILLER_ACCEPTS_ADHOC = "billerAcceptsAdhoc";
    public static final String PARENT_BILLER = "parentBiller";
    public static final String BILLER_OWNER_SHP = "billerOwnerShp";
    public static final String BILLER_COVERAGE = "billerCoverage";
    public static final String FETCH_REQUIREMENT = "fetchRequirement";
    public static final String PAYMENT_AMOUNT_EXACTNESS = "paymentAmountExactness";
    public static final String SUPPORT_BILL_VALIDATION = "supportBillValidation";
    public static final String BILLER_EFFCTV_FROM = "billerEffctvFrom";
    public static final String BILLER_EFFCTV_TO = "billerEffctvTo";
    public static final String BILLER_TEMP_DEACTIVATION_START = "billerTempDeactivationStart";
    public static final String BILLER_TEMP_DEACTIVATION_END = "billerTempDeactivationEnd";
    public static final String PARENT_BILLER_ID = "parentBillerId";
    public static final String INTERCHANGE_FEE_CONF = "interchangeFeeConf";
    public static final String INTERCHANGE_FEE = "interchangeFee";
    public static final String STATUS = "Status";

    public static final String BILLER_PAYMENT_MODES = "billerPaymentModes";
    public static final String BILLER_PAYMENT_CHANNELS = "billerPaymentChannels";
    public static final String BILLER_CUSTOMER_PARAMS = "billerCustomerParams";
    public static final String BILLER_RESPONSE_PARAMS = "billerResponseParams";
    public static final String BILLER_ADDITIONAL_INFO = "billerAdditionalInfo";
  }

  public final String getBillerId() {
    return (String) this.get(Fields.BILLER_ID);
  }

  public final void setBillerId(String billerId) {
    this.put(Fields.BILLER_ID, billerId);
  }

  public final String getBillerName() {
    return (String) this.get(Fields.BILLER_NAME);
  }

  public final void setBillerName(String billerName) {
    this.put(Fields.BILLER_NAME, billerName);
  }

  public final String getBillerAliasName() {
    return (String) this.get(Fields.BILLER_ALIAS_NAME);
  }

  public final void setBillerAliasName(String billerAliasName) {
    this.put(Fields.BILLER_ALIAS_NAME, billerAliasName);
  }

  public final String getBillerCategoryName() {
    return (String) this.get(Fields.BILLER_CATEGORY_NAME);
  }

  public final void setBillerCategoryName(String billerCategoryName) {
    this.put(Fields.BILLER_CATEGORY_NAME, billerCategoryName);
  }

  public final String getBillerMode() {
    return (String) this.get(Fields.BILLER_MODE);
  }

  public final void setBillerMode(String billerMode) {
    this.put(Fields.BILLER_MODE, billerMode);
  }

  public final String getBillerAcceptsAdhoc() {
    return (String) this.get(Fields.BILLER_ACCEPTS_ADHOC);
  }

  public final void setBillerAcceptsAdhoc(String billerAcceptsAdhoc) {
    this.put(Fields.BILLER_ACCEPTS_ADHOC, billerAcceptsAdhoc);
  }

  public final String getParentBiller() {
    return (String) this.get(Fields.PARENT_BILLER);
  }

  public final void setParentBiller(String parentBiller) {
    this.put(Fields.PARENT_BILLER, parentBiller);
  }

  public final String getBillerOwnerShp() {
    return (String) this.get(Fields.BILLER_OWNER_SHP);
  }

  public final void setBillerOwnerShp(String billerOwnerShp) {
    this.put(Fields.BILLER_OWNER_SHP, billerOwnerShp);
  }

  public final String getBillerCoverage() {
    return (String) this.get(Fields.BILLER_COVERAGE);
  }

  public final void setBillerCoverage(String billerCoverage) {
    this.put(Fields.BILLER_COVERAGE, billerCoverage);
  }

  public final String getFetchRequirement() {
    return (String) this.get(Fields.FETCH_REQUIREMENT);
  }

  public final void setFetchRequirement(String fetchRequirement) {
    this.put(Fields.FETCH_REQUIREMENT, fetchRequirement);
  }

  public final String getPaymentAmountExactness() {
    return (String) this.get(Fields.PAYMENT_AMOUNT_EXACTNESS);
  }

  public final void setPaymentAmountExactness(String paymentAmountExactness) {
    this.put(Fields.PAYMENT_AMOUNT_EXACTNESS, paymentAmountExactness);
  }

  public final String getSupportBillValidation() {
    return (String) this.get(Fields.SUPPORT_BILL_VALIDATION);
  }

  public final void setSupportBillValidation(String supportBillValidation) {
    this.put(Fields.SUPPORT_BILL_VALIDATION, supportBillValidation);
  }

  public final String getBillerEffctvFrom() {
    return (String) this.get(Fields.BILLER_EFFCTV_FROM);
  }

  public final void setBillerEffctvFrom(String billerEffctvFrom) {
    this.put(Fields.BILLER_EFFCTV_FROM, billerEffctvFrom);
  }

  public final String getBillerEffctvTo() {
    return (String) this.get(Fields.BILLER_EFFCTV_TO);
  }

  public final void setBillerEffctvTo(String billerEffctvTo) {
    this.put(Fields.BILLER_EFFCTV_TO, billerEffctvTo);
  }

  public final String getBillerTempDeactivationStart() {
    return (String) this.get(Fields.BILLER_TEMP_DEACTIVATION_START);
  }

  public final void setBillerTempDeactivationStart(String billerTempDeactivationStart) {
    this.put(Fields.BILLER_TEMP_DEACTIVATION_START, billerTempDeactivationStart);
  }

  public final String getBillerTempDeactivationEnd() {
    return (String) this.get(Fields.BILLER_TEMP_DEACTIVATION_END);
  }

  public final void setBillerTempDeactivationEnd(String billerTempDeactivationEnd) {
    this.put(Fields.BILLER_TEMP_DEACTIVATION_END, billerTempDeactivationEnd);
  }

  public final String getParentBillerId() {
    return (String) this.get(Fields.PARENT_BILLER_ID);
  }

  public final void setParentBillerId(String parentBillerId) {
    this.put(Fields.PARENT_BILLER_ID, parentBillerId);
  }

  public final Object getInterchangeFeeConf() {
    return this.get(Fields.INTERCHANGE_FEE_CONF);
  }

  public final void setInterchangeFeeConf(String interchangeFeeConf) {
    this.put(Fields.INTERCHANGE_FEE_CONF, interchangeFeeConf);
  }

  public final Object getInterchangeFee() {
    return this.get(Fields.INTERCHANGE_FEE);
  }

  public final void setInterchangeFee(String interchangeFee) {
    this.put(Fields.INTERCHANGE_FEE, interchangeFee);
  }

  public final String getStatus() {
    return (String) this.get(Fields.STATUS);
  }

  public final void setStatus(String Status) {
    this.put(Fields.STATUS, Status);
  }

  public final String getBillerPaymentModes() {
    return (String) this.get(Fields.BILLER_PAYMENT_MODES);
  }

  public final void setBillerPaymentModes(String billerPaymentModes) {
    this.put(Fields.BILLER_PAYMENT_MODES, billerPaymentModes);
  }

  public final String getBillerPaymentChannels() {
    return (String) this.get(Fields.BILLER_PAYMENT_CHANNELS);
  }

  public final void setBillerPaymentChannels(String billerPaymentChannels) {
    this.put(Fields.STATUS, billerPaymentChannels);
  }

  public final String getBillerCustomerParams() {
    return (String) this.get(Fields.BILLER_CUSTOMER_PARAMS);
  }

  public final void setBillerCustomerParams(String billerCustomerParams) {
    this.put(Fields.BILLER_CUSTOMER_PARAMS, billerCustomerParams);
  }

  public final String getBillerResponseParams() {
    return (String) this.get(Fields.BILLER_RESPONSE_PARAMS);
  }

  public final void setBillerResponseParams(String billerResponseParams) {
    this.put(Fields.BILLER_RESPONSE_PARAMS, billerResponseParams);
  }

  public final String getBillerAdditionalInfo() {
    return (String) this.get(Fields.BILLER_ADDITIONAL_INFO);
  }

  public final void setBillerAdditionalInfo(String billerAdditionalInfo) {
    this.put(Fields.STATUS, billerAdditionalInfo);
  }
}
