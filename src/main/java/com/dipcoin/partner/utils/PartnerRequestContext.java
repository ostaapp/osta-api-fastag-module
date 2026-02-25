package com.dipcoin.partner.utils;

public class PartnerRequestContext {

  private String traceId;

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public static PartnerRequestContext instance() {
    return new PartnerRequestContext();
  }
}
