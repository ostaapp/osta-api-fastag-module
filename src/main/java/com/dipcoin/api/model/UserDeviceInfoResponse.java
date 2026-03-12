package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class UserDeviceInfoResponse extends APIResponse {

  private String deviceType;
  private String userAgent;
  private String imeiNo;
  private String status;
  private String registrationToken;
  private String clientTransactionId;

  public String getDeviceType() {
    return deviceType;
  }

  public void setDeviceType(String deviceType) {
    this.deviceType = deviceType;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getImeiNo() {
    return imeiNo;
  }

  public void setImeiNo(String imeiNo) {
    this.imeiNo = imeiNo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRegistrationToken() {
    return registrationToken;
  }

  public void setRegistrationToken(String registrationToken) {
    this.registrationToken = registrationToken;
  }

  public String getClientTransactionId() {
    return clientTransactionId;
  }

  public void setClientTransactionId(String clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
  }
}
