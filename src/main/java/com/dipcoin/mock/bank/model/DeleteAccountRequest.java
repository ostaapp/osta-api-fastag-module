package com.dipcoin.mock.bank.model;

import com.dipcoin.mock.bank.model.MockBankRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteAccountRequest extends MockBankRequest {
  private String bankUID;
  
  public String getBankUID() {
    return this.bankUID;
  }
  
  public void setBankUID(String bankUID) {
    this.bankUID = bankUID;
  }
}
