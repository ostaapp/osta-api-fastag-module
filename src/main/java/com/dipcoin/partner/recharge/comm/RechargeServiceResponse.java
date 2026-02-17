package com.dipcoin.partner.recharge.comm;

public class RechargeServiceResponse extends RechargeResponse {

  private static final long serialVersionUID = 1L;

  public RechargeServiceResponse() {

  }

  public static interface Fields extends RechargeResponse.Fields {
    public static final String OPERATOR_REFERENCE_NUMBER = "OperatorRefNo";
    public static final String OSTA_TRANSACTION_REFERENCEID = "ostaTransactionReferenceId";
  }

  public final String getOperatorRefNo() {
    return this.get(Fields.OPERATOR_REFERENCE_NUMBER);
  }

  public final void setOperatorRefNo(String operatorRefNo) {
    this.put(Fields.OPERATOR_REFERENCE_NUMBER, operatorRefNo);
  }

  public String getOstaTransactionReferenceId() {
    return this.get(Fields.OSTA_TRANSACTION_REFERENCEID);
  }

  public void setOstaTransactionReferenceId(String ostaTransactionReferenceId) {
    this.put(Fields.OSTA_TRANSACTION_REFERENCEID, ostaTransactionReferenceId);
  }
}
