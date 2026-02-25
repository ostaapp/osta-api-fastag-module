package com.dipcoin.partner.recharge.comm;

public class BillPaymentServiceResponse extends BillResponse {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static interface Fields extends BillResponse.Fields {
    public static final String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
  }

  public final String getEuronetRefNo() {
    return (String) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEuronetRefNo(String euronetRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, euronetRefNo);
  }

}
