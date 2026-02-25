package com.dipcoin.partner.recharge.comm;

import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;
import com.fasterxml.jackson.core.JsonProcessingException;

public class RechargeBalanceRequest extends RechargeRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public RechargeBalanceRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.BALANCE_RECHARGE);
  }

  public static interface Fields extends RechargeRequest.Fields {
	  public static String BALANCE_CHECK_REQUEST = "BalanceCheckRequest";
  }

	public final String getBalanceCheckRequest() {
		return this.get(Fields.BALANCE_CHECK_REQUEST);
	}

	public final void setBalanceCheckRequest(BillMerchantValidationDetails billMerchantValidationDetails) {
		this.put(Fields.BALANCE_CHECK_REQUEST, billMerchantValidationDetails.toString());
	}
	
	 @Override
	  public String toString() {
	    try {
	      LOG.debug(objectMapper.writeValueAsString(this));
	      return objectMapper.writeValueAsString(this);
	    } catch (JsonProcessingException e) {
	      // TODO Auto-generated catch block
	    }

	    return null;
	  }
  
}
