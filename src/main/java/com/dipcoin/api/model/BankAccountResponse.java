package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class BankAccountResponse extends APIResponse {

  private String accountHolderName;
  private String accountNumber;
  private Integer isPrimaryAccount;
  private Integer accountType; //Refers to BankAccountCodes
  private String ifscCode;
  private Integer status;
  private String encAccountId;
  private Integer bankAccountType;  //Refers to BankAccountType
  private String clientTransactionId;

  public final String getAccountHolderName() {
    return accountHolderName;
  }

  public final BankAccountResponse setAccountHolderName(String accountHolderName) {
    this.accountHolderName = accountHolderName;
    return this;
  }

  public final String getAccountNumber() {
    return accountNumber;
  }

  public final BankAccountResponse setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
    return this;
  }

  public final Integer getIsPrimaryAccount() {
    return isPrimaryAccount;
  }

  public final BankAccountResponse setIsPrimaryAccount(Integer isPrimaryAccount) {
    this.isPrimaryAccount = isPrimaryAccount;
    return this;
  }

  public final Integer getAccountType() {
    return accountType;
  }

  public final BankAccountResponse setAccountType(Integer accountType) {
    this.accountType = accountType;
    return this;
  }

  public final String getIfscCode() {
    return ifscCode;
  }

  public final BankAccountResponse setIfscCode(String ifscCode) {
    this.ifscCode = ifscCode;
    return this;
  }

  public Integer getBankAccountType() {
    return bankAccountType;
  }

  public void setBankAccountType(Integer bankAccountType) {
    this.bankAccountType = bankAccountType;
  }
  
  public final Integer getStatus() {
    return status;
  }

  public final BankAccountResponse setStatus(Integer status) {
    this.status = status;
    return this;
  }

  public String getEncAccountId() {
    return encAccountId;
  }

  public BankAccountResponse setEncAccountId(String encAccountId) {
    this.encAccountId = encAccountId;
    return this;
  }

  public final String getClientTransactionId() {
    return clientTransactionId;
  }

  public final BankAccountResponse setClientTransactionId(String clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
    return this;
  }
}
