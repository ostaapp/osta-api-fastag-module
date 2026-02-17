package com.dipcoin.api.model;

public class PartnerResponse extends APIResponse {

  private String encryptedPayload;

  private String checksum;
  
  private String redirectURI;
  
  private Integer status;



  public PartnerResponse() {

  }

  public String getEncryptedPayload() {
    return encryptedPayload;
  }

  public void setEncryptedPayload(String encryptedPayload) {
    this.encryptedPayload = encryptedPayload;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getRedirectURI() {
    return redirectURI;
  }

  public void setRedirectURI(String redirectURI) {
    this.redirectURI = redirectURI;
  }

}
