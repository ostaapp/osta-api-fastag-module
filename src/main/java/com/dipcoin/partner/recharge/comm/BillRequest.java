package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;
import com.dipcoin.partner.recharge.services.RechargeServicesImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BillRequest extends LinkedHashMap<String, Object> {
  public static final Logger LOG = LogManager.getLogger(RechargeServicesImpl.class);
  public static ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 
   */
  public OperationType operationType;
  public String merchantReferenceId;

  public BillRequest(String merchantRefId, OperationType operationType) {
    this.operationType = operationType;
    this.merchantReferenceId = merchantRefId;
  }

  private static final long serialVersionUID = 1L;

  public static interface Fields {
    public static String REQUEST_TYPE = "RequestType";
  }

  final public String getMerchantReferenceId() {
    return this.merchantReferenceId;
  }


  public final String getRequestType() {
    return (String) this.get(Fields.REQUEST_TYPE);
  }

  public final void setRequestType(String requestType) {
    this.put(Fields.REQUEST_TYPE, requestType);
  }

  public void setOperationType(OperationType operatnType) {
    operationType = operatnType;
  }

  public final OperationType getOperationType() {
    return this.operationType;
  }

  public BillRequest deepCopy() {
    return SerializationUtils.clone(this);
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
