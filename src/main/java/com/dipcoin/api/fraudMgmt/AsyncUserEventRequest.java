package com.dipcoin.api.fraudMgmt;

import com.dipcoin.api.model.APIRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AsyncUserEventRequest<T extends APIRequest> {
  private static ObjectMapper mapper = new ObjectMapper();

  private EventStatus rawRequest;

  private String status;

  private Integer userId;

  private String IpAddress;

  private String traceId;

  private String userEvent;

  private String requestTime;

  private String responseTime;

  private String ostaId;



  public String getStatus() {
    return status;
  }



  public void setStatus(String status) {
    this.status = status;
  }



  public String getIpAddress() {
    return IpAddress;
  }



  public void setIpAddress(String ipAddress) {
    IpAddress = ipAddress;
  }



  public String getTraceId() {
    return traceId;
  }



  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }



  public String getUserEvent() {
    return userEvent;
  }



  public void setUserEvent(String userEvent) {
    this.userEvent = userEvent;
  }



  public String getRequestTime() {
    return requestTime;
  }



  public void setRequestTime(String requestTime) {
    this.requestTime = requestTime;
  }



  public String getResponseTime() {
    return responseTime;
  }



  public void setResponseTime(String responseTime) {
    this.responseTime = responseTime;
  }



  public Integer getUserId() {
    return userId;
  }



  public void setUserId(Integer userId) {
    this.userId = userId;
  }


  public EventStatus getRawRequest() {
    return rawRequest;
  }



  public void setRawRequest(EventStatus rawRequest) {
    this.rawRequest = rawRequest;
  }



  public String getOstaId() {
    return ostaId;
  }



  public void setOstaId(String ostaId) {
    this.ostaId = ostaId;
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
