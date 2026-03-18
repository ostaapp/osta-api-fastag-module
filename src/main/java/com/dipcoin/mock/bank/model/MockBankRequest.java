package com.dipcoin.mock.bank.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class MockBankRequest {
  private String dipcoinReferenceNumber;
  
  private String ostaReferenceNumber;
  
  private String transactionType;
  
  private String transactionTime;
  
  private String bankID;
  
  private String attempt;
  
  public static Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
  
  public String toString() {
    return gson.toJson(this);
  }
  
  public String getDipcoinReferenceNumber() {
    return (this.dipcoinReferenceNumber != null) ? this.dipcoinReferenceNumber : this.ostaReferenceNumber;
  }
  
  public void setDipcoinReferenceNumber(String dipcoinReferenceNumber) {
    this.dipcoinReferenceNumber = dipcoinReferenceNumber;
  }
  
  public String getOstaReferenceNumber() {
    return this.ostaReferenceNumber;
  }
  
  public void setOstaReferenceNumber(String ostaReferenceNumber) {
    this.ostaReferenceNumber = ostaReferenceNumber;
  }
  
  public String getTransactionType() {
    return this.transactionType;
  }
  
  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }
  
  public String getTransactionTime() {
    return this.transactionTime;
  }
  
  public void setTransactionTime(String transactionTime) {
    this.transactionTime = transactionTime;
  }
  
  public String getBankID() {
    return this.bankID;
  }
  
  public void setBankID(String bankID) {
    this.bankID = bankID;
  }
  
  public String getAttempt() {
    return this.attempt;
  }
  
  public void setAttempt(String attempt) {
    this.attempt = attempt;
  }
}
