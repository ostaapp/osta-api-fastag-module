package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class BillRegisterComplaintRequest extends BillRequest {

  private static final long serialVersionUID = 1L;

  public BillRegisterComplaintRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.REGISTER_COMPLAINT);
  }

  public static interface Fields extends BillRequest.Fields {
    public static String BILL_REGISTER_COMPLAINT = "ComplaintGenerationRequestDetails";
  }

  public final BillComplaintGenerationRequestDetails getBillComplaintGenerationRequestDetails() {
    return (BillComplaintGenerationRequestDetails) this.get(Fields.BILL_REGISTER_COMPLAINT);
  }

  public final void setBillComplaintGenerationRequestDetails(
      BillComplaintGenerationRequestDetails billComplaintGenerationRequestDetails) {
    this.put(Fields.BILL_REGISTER_COMPLAINT, billComplaintGenerationRequestDetails);
  }

  public boolean validate() {
    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
