package com.dipcoin.mock.bank.model;

import com.dipcoin.mock.bank.model.MockBankRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoveLienRequest extends MockBankRequest {
  private String comment;
  
  private BigDecimal amount;
  
  private String currency;
  
  private String cbsJournalNumber;
  
  private String bankUID;
  
  private String cbsDate;
  
  public String getComment() {
    return this.comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  public BigDecimal getAmount() {
    return this.amount;
  }
  
  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
  
  public String getCurrency() {
    return this.currency;
  }
  
  public void setCurrency(String currency) {
    this.currency = currency;
  }
  
  public String getCbsJournalNumber() {
    return this.cbsJournalNumber;
  }
  
  public void setCbsJournalNumber(String cbsJournalNumber) {
    this.cbsJournalNumber = cbsJournalNumber;
  }
  
  public String getBankUID() {
    return this.bankUID;
  }
  
  public void setBankUID(String bankUID) {
    this.bankUID = bankUID;
  }
  
  public String getCbsDate() {
    return this.cbsDate;
  }
  
  public void setCbsDate(String cbsDate) {
    this.cbsDate = cbsDate;
  }
}
