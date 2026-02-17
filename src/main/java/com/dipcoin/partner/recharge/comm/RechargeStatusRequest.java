package com.dipcoin.partner.recharge.comm;

import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class RechargeStatusRequest extends RechargeRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;


  public RechargeStatusRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.STATUS_RECHARGE);
  }


  public static interface Fields extends RechargeRequest.Fields {
    public String ORIGINAL_MERCHANT_REFERENCE_NUMBER = "OriginalMerchantRefNo";
    public String EURONET_REFERENCE_NUMBER = "EnRefNo";
    public String RECHARGE_TYPE = "RechargeType";
  }

  public final String getOriginalMerchantRefNo() {
    return this.get(Fields.ORIGINAL_MERCHANT_REFERENCE_NUMBER);

  }

  public final void setOriginalMerchantRefNo(String originalMerchantRefNo) {
    this.put(Fields.ORIGINAL_MERCHANT_REFERENCE_NUMBER, originalMerchantRefNo);
  }

  public final String getEnRefno() {
    return this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEnRefno(String enRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, enRefNo);
  }

  public final String getRechargeType() {
    return this.get(Fields.RECHARGE_TYPE);
  }

  public final void setRechargeType(String rechargeType) {
    this.put(Fields.RECHARGE_TYPE, rechargeType);
  }

}
