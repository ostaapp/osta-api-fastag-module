package com.dipcoin.partner.recharge.comm;

public class RechargeValidateResponse extends RechargeResponse {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static interface Fields extends RechargeResponse.Fields {
  }

  private String operatorRechargeType;

  public final String getOperatorRechargeType() {
    return operatorRechargeType;
  }

  public final void setOperatorRechargeType(String operatorRechargeType) {
    this.operatorRechargeType = operatorRechargeType;
  }
}
