package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.comm.BillResponse.Fields;

public class RechargePlanResponse extends BillResponse{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  public static interface Fields extends BillResponse.Fields {
    public static final String RESPONSE = "planResponse";
  }
  
  public final String getPlanResponse() {
    return (String) this.get(Fields.RESPONSE);
  }

  public final void setPlanResponse(String rechargePlan) {
    this.put(Fields.RESPONSE, rechargePlan);
  }
  
  private String errorMsg;
  private String rawData;

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public String getRawData() {
    return rawData;
  }

  public void setRawData(String rawData) {
    // @NOTE - do not allow any random data in. setting a max limit.
    this.rawData = StringUtils.abbreviate(rawData, 1024);
  }
}
