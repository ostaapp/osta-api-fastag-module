package com.dipcoin.mock.bank.model;

import com.dipcoin.mock.bank.model.MockBankRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerFundTransferRequest extends MockBankRequest {
  private BigDecimal amount;
  
  private String currency;
  
  private String account;
  
  private String bankUID;
  
  private String cbsDate;
  
  private String cbsJournalNumber;
  
  private String comment;
  
  private String lien_amount;
  
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
  
  public String getAccount() {
    return this.account;
  }
  
  public void setAccount(String account) {
    this.account = account;
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
  
  public String getCbsJournalNumber() {
    return this.cbsJournalNumber;
  }
  
  public void setCbsJournalNumber(String cbsJournalNumber) {
    this.cbsJournalNumber = cbsJournalNumber;
  }
  
  public String getComment() {
    return this.comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  public String getLien_amount() {
    return this.lien_amount;
  }
  
  public void setLien_amount(String lien_amount) {
    this.lien_amount = lien_amount;
  }
}
