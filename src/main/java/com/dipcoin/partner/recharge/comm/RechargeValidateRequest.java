package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class RechargeValidateRequest extends RechargeRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public RechargeValidateRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.VALIDATE_RECHARGE);
  }

  public static interface Fields extends RechargeRequest.Fields {
    public String RECHARGE_TYPE = "RechargeType";

  }

  public final String getRechargeType() {
    return this.get(Fields.RECHARGE_TYPE);
  }

  public final void setRechargeType(String rechargeType) {
    this.put(Fields.RECHARGE_TYPE, rechargeType);
  }

  public boolean validate() {
    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
