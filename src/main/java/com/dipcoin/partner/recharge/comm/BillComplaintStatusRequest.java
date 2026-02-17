package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class BillComplaintStatusRequest extends BillRequest {


  private static final long serialVersionUID = 1L;

  public BillComplaintStatusRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.COMPLAINT_STATUS);
  }

  public static interface Fields extends BillRequest.Fields {
    public static String BILL_COMPLAINT_STATUS = "ComplaintStatusRequestDetails";
  }

  public final BillComplaintStatus getBillComplaintStatus() {
    return (BillComplaintStatus) this.get(Fields.BILL_COMPLAINT_STATUS);
  }

  public final void setBillComplaintStatus(BillComplaintStatus billComplaintStatus) {
    this.put(Fields.BILL_COMPLAINT_STATUS, billComplaintStatus);
  }

  public boolean validate() {
    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
