package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class BillStatusRequest extends BillRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public BillStatusRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.FETCH_STATUS);
  }

  public static interface Fields extends BillRequest.Fields {
    public static String BILL_STATUS_REQUEST_DETAILS = "BBPSStatusRequestDetails";
  }

  public final BillMerchantStatusDetails getBillMerchantStatusDetails() {
    return (BillMerchantStatusDetails) this.get(Fields.BILL_STATUS_REQUEST_DETAILS);
  }

  public final void setBillMerchantStatusDetails(
      BillMerchantStatusDetails billMerchantStatusDetails) {
    this.put(Fields.BILL_STATUS_REQUEST_DETAILS, billMerchantStatusDetails);
  }

  public boolean validate() {
    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }

}
