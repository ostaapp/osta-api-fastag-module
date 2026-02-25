package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class BillPaymentServiceRequest extends BillRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public BillPaymentServiceRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.SERVICE_BILL);
  }

  public static interface Fields extends BillRequest.Fields {
    public static String BILL_MERCHANT_SERVICE_DETAILS = "MerchantServiceRequestDetails";
  }

  public final BillMerchantServiceDetails getBillMerchantServiceDetails() {
    return (BillMerchantServiceDetails) this.get(Fields.BILL_MERCHANT_SERVICE_DETAILS);
  }

  public final void setBillMerchantServiceDetails(
      BillMerchantServiceDetails billMerchantServiceDetails) {
    this.put(Fields.BILL_MERCHANT_SERVICE_DETAILS, billMerchantServiceDetails);
  }

  public boolean validate() {
    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
