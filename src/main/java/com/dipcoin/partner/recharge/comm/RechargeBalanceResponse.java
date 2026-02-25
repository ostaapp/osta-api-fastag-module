package com.dipcoin.partner.recharge.comm;

public class RechargeBalanceResponse extends RechargeResponse {

  private static final long serialVersionUID = 1L;

  public static interface Fields extends RechargeResponse.Fields {
    public static final String AMOUNT = "Balance";
  }

  public final String getAmount() {
    return this.get(Fields.AMOUNT);
  }

  public final void setAmount(String amount) {
    this.put(Fields.AMOUNT, amount);
  }
}
