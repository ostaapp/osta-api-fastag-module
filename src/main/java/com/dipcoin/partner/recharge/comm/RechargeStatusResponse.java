package com.dipcoin.partner.recharge.comm;

public class RechargeStatusResponse extends RechargeServiceResponse {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private String originalMerchantRefNo;

  public static interface Fields extends RechargeServiceResponse.Fields {
    public static final String ORIGINAL_EURONET_REFERENCE_NUMBER = "OriginalEnRefNo";
  }

  public final String getOriginalMerchantRefNo() {
    return originalMerchantRefNo;
  }

  public final void setOriginalMerchantRefNo(String originalMerchantRefNo) {
    this.originalMerchantRefNo = originalMerchantRefNo;
  }

  public final String getOriginalEnRefNo() {
    return this.get(Fields.ORIGINAL_EURONET_REFERENCE_NUMBER);
  }

  public final void setOriginalEnRefNo(String originalEnRefNo) {
    this.put(Fields.ORIGINAL_EURONET_REFERENCE_NUMBER, originalEnRefNo);
  }

}
