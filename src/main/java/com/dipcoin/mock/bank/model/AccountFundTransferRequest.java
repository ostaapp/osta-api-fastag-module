package com.dipcoin.mock.bank.model;

import com.dipcoin.mock.bank.model.MockBankRequest;

public class AccountFundTransferRequest extends MockBankRequest {
  private String amount;
  
  private String currency;
  
  private String toAccount;
  
  private String fromAccount;
  
  private String toAccountIFSCCode;
  
  private String fromAccountIFSCCode;
  
  private String comment;
  
  public String getAmount() {
    return this.amount;
  }
  
  public void setAmount(String amount) {
    this.amount = amount;
  }
  
  public String getCurrency() {
    return this.currency;
  }
  
  public void setCurrency(String currency) {
    this.currency = currency;
  }
  
  public String getToAccount() {
    return this.toAccount;
  }
  
  public void setToAccount(String toAccount) {
    this.toAccount = toAccount;
  }
  
  public String getFromAccount() {
    return this.fromAccount;
  }
  
  public void setFromAccount(String fromAccount) {
    this.fromAccount = fromAccount;
  }
  
  public String getToAccountIFSCCode() {
    return this.toAccountIFSCCode;
  }
  
  public void setToAccountIFSCCode(String toAccountIFSCCode) {
    this.toAccountIFSCCode = toAccountIFSCCode;
  }
  
  public String getFromAccountIFSCCode() {
    return this.fromAccountIFSCCode;
  }
  
  public void setFromAccountIFSCCode(String fromAccountIFSCCode) {
    this.fromAccountIFSCCode = fromAccountIFSCCode;
  }
  
  public String getComment() {
    return this.comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
}
