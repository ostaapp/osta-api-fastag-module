package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class MerchantDipcoinResponse extends DipcoinResponse {
  private String orderId;
  private String merchantTransactionReferenceId;
  private String partnerReferenceId;

  public String getOrderId() {
    return orderId;
  }

  public MerchantDipcoinResponse setOrderId(String orderId) {
    this.orderId = orderId;
    return this;
  }

  public String getMerchantTransactionReferenceId() {
    return merchantTransactionReferenceId;
  }

  public MerchantDipcoinResponse setMerchantTransactionReferenceId(
      String merchantTransactionReferenceId) {
    this.merchantTransactionReferenceId = merchantTransactionReferenceId;
    return this;
  }

  public String getPartnerReferenceId() {
    return partnerReferenceId;
  }

  public MerchantDipcoinResponse setPartnerReferenceId(String partnerReferenceId) {
    this.partnerReferenceId = partnerReferenceId;
    return this;
  }

}
