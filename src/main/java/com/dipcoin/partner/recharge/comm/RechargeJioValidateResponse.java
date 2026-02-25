package com.dipcoin.partner.recharge.comm;

public class RechargeJioValidateResponse extends RechargeResponse {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public RechargeJioValidateResponse() {}

  public static interface Fields extends RechargeResponse.Fields {
    public static final String CUSTOMER_TYPE = "CustomerType";
    public static final String SERVICE_PROVIDER_CODE = "SPCode";
    public static String SERVICE_PROVIDER_AND_CIRCLE_CODE = "SSPCode";
    public static String SERVICE_TYPE = "ServiceType";

  }

  public final String getSpCode() {
    return this.get(Fields.SERVICE_PROVIDER_CODE);
  }

  public final void setSpCode(String spCode) {
    this.put(Fields.SERVICE_PROVIDER_CODE, spCode);
  }

  public final String getSspCode() {
    return this.get(Fields.SERVICE_PROVIDER_AND_CIRCLE_CODE);
  }

  public final void setSspCode(String sspCode) {
    this.put(Fields.SERVICE_PROVIDER_AND_CIRCLE_CODE, sspCode);
  }

}
