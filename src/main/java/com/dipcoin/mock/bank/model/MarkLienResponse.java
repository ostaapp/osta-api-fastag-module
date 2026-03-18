package com.dipcoin.mock.bank.model;

import com.dipcoin.mock.bank.model.MockBankResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarkLienResponse extends MockBankResponse {
  private String cbsJournalNumber;
  
  private String cbsDate;
  
  public String getCbsJournalNumber() {
    return this.cbsJournalNumber;
  }
  
  public void setCbsJournalNumber(String cbsJournalNumber) {
    this.cbsJournalNumber = cbsJournalNumber;
  }
  
  public String getCbsDate() {
    return this.cbsDate;
  }
  
  public void setCbsDate(String cbsDate) {
    this.cbsDate = cbsDate;
  }
}
