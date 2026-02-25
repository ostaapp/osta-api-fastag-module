package com.dipcoin.partner.recharge.comm;

public class MDMResponse extends BillResponse {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static interface Fields extends BillResponse.Fields {
    public static final String MDM = "MDM";
  }

  public final MDMResponseDetails getMDM() {
    return (MDMResponseDetails) this.get(Fields.MDM);
  }

  public final void setMDM(MDMResponseDetails MDM) {
    this.put(Fields.MDM, MDM);
  }

}
