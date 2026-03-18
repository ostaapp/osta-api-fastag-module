package com.dipcoin.mock.bank.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class MockBankResponse {
  private String dipcoinReferenceNumber;
  
  private String ostaReferenceNumber;
  
  private String bankTransactionReferenceNumber;
  
  private String bankResponseCode;
  
  private String bankResponseDesc;
  
  private String transactionTime;
  
  public static Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
  
  public String toString() {
    return gson.toJson(this);
  }
  
  public String getDipcoinReferenceNumber() {
    return this.dipcoinReferenceNumber;
  }
  
  public void setDipcoinReferenceNumber(String dipcoinReferenceNumber) {
    this.dipcoinReferenceNumber = dipcoinReferenceNumber;
  }
  
  public String getOstaReferenceNumber() {
    return (this.ostaReferenceNumber != null) ? this.ostaReferenceNumber : this.dipcoinReferenceNumber;
  }
  
  public void setOstaReferenceNumber(String ostaReferenceNumber) {
    this.ostaReferenceNumber = ostaReferenceNumber;
  }
  
  public String getBankTransactionReferenceNumber() {
    return this.bankTransactionReferenceNumber;
  }
  
  public void setBankTransactionReferenceNumber(String bankTransactionReferenceNumber) {
    this.bankTransactionReferenceNumber = bankTransactionReferenceNumber;
  }
  
  public String getBankResponseCode() {
    return this.bankResponseCode;
  }
  
  public void setBankResponseCode(String bankResponseCode) {
    this.bankResponseCode = bankResponseCode;
  }
  
  public String getBankResponseDesc() {
    return this.bankResponseDesc;
  }
  
  public void setBankResponseDesc(String bankResponseDesc) {
    this.bankResponseDesc = bankResponseDesc;
  }
  
  public String getTransactionTime() {
    return this.transactionTime;
  }
  
  public void setTransactionTime(String transactionTime) {
    this.transactionTime = transactionTime;
  }
}
