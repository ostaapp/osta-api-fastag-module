package com.dipcoin.partner.recharge.comm;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;

public class RechargePlanRequest extends BillRequest {

  private static final long serialVersionUID = 1L;

  public RechargePlanRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.RECHARGE_PLAN);
  }

  public static interface Fields extends RechargeRequest.Fields {
    public static String SERVICE_CODE = "servicecode";
    public static String CONSUMER_NO = "consumerno";
    public static String SP_CODE = "spcode";
    public static String SSP_CODE = "sspcode";
    public static String MERCHANT_CODE = "merchantcode";
  }
  
  public String getServicecode() {
    return (String) this.get(Fields.SERVICE_CODE);
  }
  public void setServicecode(String servicecode) {
    this.put(Fields.SERVICE_CODE, servicecode);
  }
  public String getConsumerno() {
    return (String) this.get(Fields.CONSUMER_NO);
  }
  public void setConsumerno(String consumerno) {
    this.put(Fields.CONSUMER_NO, consumerno);
  }
  public String getSpcode() {
    return (String) this.get(Fields.SP_CODE);
  }
  public void setSpcode(String spcode) {
    this.put(Fields.SP_CODE, spcode);
  }
  public String getSspcode() {
    return (String) this.get(Fields.SSP_CODE);
  }
  public void setSspcode(String sspcode) {
    this.put(Fields.SSP_CODE, sspcode);
  }
  public String getMerchantcode() {
    return (String) this.get(Fields.MERCHANT_CODE);
  }
  public void setMerchantcode(String merchantcode) {
    this.put(Fields.MERCHANT_CODE, merchantcode);
  }

  public boolean validate() {

    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
