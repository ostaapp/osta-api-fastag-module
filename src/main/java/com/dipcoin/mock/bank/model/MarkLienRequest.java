package com.dipcoin.mock.bank.model;

import com.dipcoin.mock.bank.model.MockBankRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MarkLienRequest extends MockBankRequest {
  private BigDecimal amount;
  
  private String comment;
  
  private String currency;
  
  private String bankUID;
  
  public BigDecimal getAmount() {
    return this.amount;
  }
  
  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
  
  public String getComment() {
    return this.comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  public String getCurrency() {
    return this.currency;
  }
  
  public void setCurrency(String currency) {
    this.currency = currency;
  }
  
  public String getBankUID() {
    return this.bankUID;
  }
  
  public void setBankUID(String bankUID) {
    this.bankUID = bankUID;
  }
}
