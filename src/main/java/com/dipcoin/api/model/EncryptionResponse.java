package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class EncryptionResponse extends APIResponse {

  private String key;
  private String algo;
  private String padding;
  private String expiryTime;

  public final String getKey() {
    return key;
  }

  public final void setKey(String key) {
    this.key = key;
  }

  public final String getAlgo() {
    return algo;
  }

  public final void setAlgo(String algo) {
    this.algo = algo;
  }

  public final String getPadding() {
    return padding;
  }

  public final void setPadding(String padding) {
    this.padding = padding;
  }

  public final String getExpiryTime() {
    return expiryTime;
  }

  public final void setExpiryTime(String expiryTime) {
    this.expiryTime = expiryTime;
  }

}
