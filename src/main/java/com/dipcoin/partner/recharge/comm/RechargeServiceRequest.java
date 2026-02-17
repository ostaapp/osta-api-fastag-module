package com.dipcoin.partner.recharge.comm;


import org.apache.commons.lang3.StringUtils;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RechargeServiceRequest extends RechargeRequest {

  public String dipcoinTransactionRefId;
  public int dipcoinId;
  public static ObjectMapper objectMapper = new ObjectMapper();
  private static final long serialVersionUID = 1L;

  public RechargeServiceRequest(String merchantReferenceId) {
    super(merchantReferenceId, OperationType.SERVICE_RECHARGE);
  }

  public static interface Fields extends RechargeRequest.Fields {
    public String ENGUID = "ENGUID";
    public String RECHARGE_TYPE = "RechargeType";
  }

  public final String getENGUID() {
    return this.get(Fields.ENGUID);

  }

  public final void setENGUID(String ENGUID) {
    this.put(Fields.ENGUID, ENGUID);
  }

  public final String getDipcoinTransactionRefId() {
    return this.dipcoinTransactionRefId;
  }

  public final void setDipcoinTransactionRefId(String dipcoinTransactionRefId) {
    this.dipcoinTransactionRefId = dipcoinTransactionRefId;
  }

  public final String getRechargeType() {
    return this.get(Fields.RECHARGE_TYPE);
  }

  public final void setRechargeType(String rechargeType) {
    this.put(Fields.RECHARGE_TYPE, rechargeType);
  }

  public final int getDipcoinId() {
    return this.dipcoinId;
  }

  public final void setDipcoinId(int dipcoinId) {
    this.dipcoinId = dipcoinId;
  }

  public boolean validate() {
    if (StringUtils.isEmpty(getOperationType().toString())) {
      return false;
    }
    return true;
  }
}
