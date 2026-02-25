package com.dipcoin.partner.recharge.comm;

public class BillPaymentValidateResponse extends BillResponse {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * 
   */

  public static interface Fields extends BillResponse.Fields {
    public static final String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
    public static final String RECHARGE_PLANS = "RechargePlans";
    public static final String BILL_DETAIL = "BillDetail";

  }

  public final Long getEuronetRefNo() {
    return (Long) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEuronetRefNo(Long euronetRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, euronetRefNo);
  }
  
  public final String getRechargePlans() {
	return (String) this.get(Fields.RECHARGE_PLANS);
  }

  public final void setRechargePlans(String rehargePlans) {
	this.put(Fields.RECHARGE_PLANS, rehargePlans);
  }  
  
  public final String getBillDetail() {
		return (String) this.get(Fields.BILL_DETAIL);
	  }

	  public final void setBillDetail(String billDetail) {
		this.put(Fields.BILL_DETAIL, billDetail);
	  }  

  
}
