package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;

public class MDMResponseDetails extends HashMap<String, Object> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static interface Fields {
    public static final String BILLER_FETCH_RESPONSE = "BillerFetchResponse";
  }

  public final MDMBillerFetchResponse getBillerFetchResponse() {
    return (MDMBillerFetchResponse) this.get(Fields.BILLER_FETCH_RESPONSE);
  }

  public final void setBillerFetchResponse(MDMBillerFetchResponse billerFetchResponse) {
    this.put(Fields.BILLER_FETCH_RESPONSE, billerFetchResponse);
  }
}
