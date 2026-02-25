package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.dipcoin.api.utils.RechargeRequestContext;
import com.dipcoin.partner.utils.PartnerClient.Protocol;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class RechargeResponse extends HashMap<String, String> {

  private static final long serialVersionUID = 1L;
  private static ObjectMapper objectMapper = new ObjectMapper();
  private final HashMap<String, String> FIELDS_MAPPING = new HashMap<>();

  public static interface Fields {
	public static final String MERCHANT_CODE = "MerchantCode";
    public static final String MERCHANT_REFERENCE_NUMBER = "MerchantRefNo";
//    public static final String EURONET_REFERENCE_NUMBER = "EnRefNo"; 
    public static final String EURONET_REFERENCE_NUMBER = "EuronetRefNo"; 
    public static final String AGENT_ID = "AgentId"; 
    public static final String RESPONSE_CODE = "ResponseCode";
    public static final String RESPONSE_MESSAGE = "ResponseMessage";
    public static final String RESPONSE_DESCRIPTION = "ResponseDescription";
    public static final String RECHARGE_TYPE = "OperatorRechargeType";
    public static final String CIRCLE = "Circle";
    public static final String RESPONSE_ERROR = "ResponseError";
  }

  private String errorMsg;
  private String rawData;

  public void setFieldsMapping(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (String key : mappings.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(mappings.get(key))) {
          FIELDS_MAPPING.put(key, mappings.get(key));
        }
      }
    }
  }

  public final Map<String, String> getFieldsMapping() {
    return FIELDS_MAPPING;
  }

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

  public final String getMerchantRefNo() {
    return this.get(Fields.MERCHANT_REFERENCE_NUMBER);
  }

  public final void setMerchantRefNo(String merchantRefNo) {
    this.put(Fields.MERCHANT_REFERENCE_NUMBER, merchantRefNo);
  }

  public final String getEnRefno() {
    return this.get(Fields.EURONET_REFERENCE_NUMBER);
  }

  public final void setEnRefno(String enRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, enRefNo);
  }

  public final String getResponseCode() {
    return this.get(Fields.RESPONSE_CODE);
  }

  public final void setResponseCode(String responseCode) {
    this.put(Fields.RESPONSE_CODE, responseCode);
  }

  public final String getResponseMessage() {
    return this.get(Fields.RESPONSE_MESSAGE);
  }

  public final void setResponseMessage(String responseMessage) {
    this.put(Fields.RESPONSE_MESSAGE, responseMessage);
  }

  public final String getResponseDescription() {
    return this.get(Fields.RESPONSE_DESCRIPTION);
  }

  public final void setResponseDescription(String responseDescription) {
    this.put(Fields.RESPONSE_DESCRIPTION, responseDescription);
  }

  public final String getRechargeType() {
    return this.get(Fields.RECHARGE_TYPE);
  }

  public final void setRechargeType(String rechargeType) {
    this.put(Fields.RECHARGE_TYPE, rechargeType);
  }

  public final String getCircle() {
    return this.get(Fields.CIRCLE);
  }

  public final void setCircle(String circle) {
    this.put(Fields.CIRCLE, circle);
  }
  
  public final String getResponseError() {
		return (String) this.get(Fields.RESPONSE_ERROR);
	}

	public final void setResponseError(String responseError) {
		this.put(Fields.RESPONSE_ERROR, responseError);
	}

  @Override
  public String toString() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
    }

    return null;
  }

  public boolean processResults(RechargeRequestContext requestContext, Protocol protocol,
      final RechargeRequest request, final byte[] rawData, RechargeResponse response)
      throws Exception {

    return true;
  }

}
