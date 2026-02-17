package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class RechargeJioValidateRequest extends RechargeRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public RechargeJioValidateRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.RECHARE_JIO_VALIDATION);
  }

  public static interface Fields extends RechargeRequest.Fields {


  }

  public boolean validate() {
    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
