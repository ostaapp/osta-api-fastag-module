package com.dipcoin.partner.recharge.comm;

import java.util.List;

public class BillStatusResponse extends BillResponse {

  /**
     * 
     */
  private static final long serialVersionUID = 1L;

  public static interface Fields extends BillResponse.Fields {
    public static final String CHARGES = "Charges";
    public static final String CUSTOMER_MOBILE_NUMER = "CustomerMobileNumber";
    public static final String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
    public static final String BILL_TRANSACTION_STATUS_DETAIL = "TxnList";
  }

  @SuppressWarnings("unchecked")
  public final List<BillTransactionStatusDetail> getBillTransactionStatusDetail() {
    return (List<BillTransactionStatusDetail>) this.get(Fields.BILL_TRANSACTION_STATUS_DETAIL);
  }

  public final void setBillTransactionStatusDetail(
      List<BillTransactionStatusDetail> billTransactionStatusDetail) {
    this.put(Fields.BILL_TRANSACTION_STATUS_DETAIL, billTransactionStatusDetail);
  }

  public final Integer getCharges() {
    return (Integer) this.get(Fields.CHARGES);
  }

  public final void setCharges(Integer charges) {
    this.put(Fields.CHARGES, charges);
  }


  public final String getCustomerMobileNumber() {
    return (String) this.get(Fields.CUSTOMER_MOBILE_NUMER);
  }

  public final void setCustomerMobileNumber(String customerMobileNumber) {
    this.put(Fields.CUSTOMER_MOBILE_NUMER, customerMobileNumber);
  }

  public final String getEuronetRefNo() {
    return (String) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEuronetRefNo(Integer euronetRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, euronetRefNo);
  }
}
