package com.dipcoin.api.model;

import com.dipcoin.api.model.APIResponse;

public class PaymentTopupWalletResponse extends APIResponse {

  private String partnerTransactionReferenceId;
  private String ostaTransactionReferenceId;
  private String amount;
  private String currency;
  private String walletId;
  private String orderId;

  public String getPartnerTransactionReferenceId() {
    return partnerTransactionReferenceId;
  }

  public void setPartnerTransactionReferenceId(String partnerTransactionReferenceId) {
    this.partnerTransactionReferenceId = partnerTransactionReferenceId;
  }

  public String getOstaTransactionReferenceId() {
    return ostaTransactionReferenceId;
  }

  public void setOstaTransactionReferenceId(String ostaTransactionReferenceId) {
    this.ostaTransactionReferenceId = ostaTransactionReferenceId;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getWalletId() {
    return walletId;
  }

  public void setWalletId(String walletId) {
    this.walletId = walletId;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }


}
