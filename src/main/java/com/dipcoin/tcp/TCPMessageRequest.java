package com.dipcoin.tcp;

import org.apache.commons.lang3.StringUtils;

public class TCPMessageRequest {

  private String traceId;
  private byte[] data;
  private String partnerReferenceId;
  private String originIP;

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public String getPartnerReferenceId() {
    return partnerReferenceId;
  }

  public void setPartnerReferenceId(String partnerReferenceId) {
    this.partnerReferenceId = partnerReferenceId;
  }


  public String getOriginIP() {
    return originIP;
  }

  public void setOriginIP(String originIP) {
    this.originIP = originIP;
  }

  public boolean validate() {
    if (StringUtils.isEmpty(partnerReferenceId) || data == null || data.length == 0)
      return false;

    return true;

  }
}
