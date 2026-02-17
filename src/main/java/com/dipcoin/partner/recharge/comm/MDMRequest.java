package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class MDMRequest extends BillRequest {

  private static final long serialVersionUID = 1L;

  public MDMRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.MDM);
  }

  public static interface Fields extends BillRequest.Fields {
    public static String MDM_REQUEST_DETAILS = "MDMRequestDetails";
  }

  public final MDMRequestDetails getMDMRequestDetails() {
    return (MDMRequestDetails) this.get(Fields.MDM_REQUEST_DETAILS);
  }

  public final void setMDMRequestDetails(MDMRequestDetails mdmRequestDetails) {
    this.put(Fields.MDM_REQUEST_DETAILS, mdmRequestDetails);
  }

  public boolean validate() {

    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
