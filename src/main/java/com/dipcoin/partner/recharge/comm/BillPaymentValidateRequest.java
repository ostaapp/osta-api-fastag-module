package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class BillPaymentValidateRequest extends BillRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public BillPaymentValidateRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.VALIDATE_BILL);
  }

  public static interface Fields extends BillRequest.Fields {
    public static String BILL_MERCHANT_VALIDATION_DETAILS = "MerchantValidationRequestDetails";
  }

  public final BillMerchantValidationDetails getBillMerchantValidationDetails() {
    return (BillMerchantValidationDetails) this.get(Fields.BILL_MERCHANT_VALIDATION_DETAILS);
  }

  public final void setBillMerchantValidationDetails(
      BillMerchantValidationDetails billMerchantValidationDetails) {
    this.put(Fields.BILL_MERCHANT_VALIDATION_DETAILS, billMerchantValidationDetails);
  }

  public boolean validate() {
    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
