package com.dipcoin.api.async;

import com.dipcoin.api.model.APIRequest;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AsyncRequest<T extends APIRequest> {
  private static ObjectMapper mapper = new ObjectMapper();

  /*
   * @NOTE - need to use JsonTypeInfo CLASS, to help Jackson serializer to add @class metadata to
   * serialized object and assist deserializer to map to correct derived APIReqests.
   */
  @JsonTypeInfo(use = Id.CLASS)
  private T rawRequest;

  private Integer userId;
  private String traceId;
  private String tollReqPay;

  public String getTollReqPay() {
    return tollReqPay;
  }

  public void setTollReqPay(String tollReqPay) {
    this.tollReqPay = tollReqPay;
  }

  public T getRawRequest() {
    return rawRequest;
  }

  public void setRawRequest(T rawRequest) {
    this.rawRequest = rawRequest;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  @Override
  public String toString() {
    try {
      mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
    }
    return null;
  }
}
