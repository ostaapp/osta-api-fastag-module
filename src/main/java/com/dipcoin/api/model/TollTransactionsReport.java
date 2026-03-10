package com.dipcoin.api.model;

import java.math.BigDecimal;
import com.dipcoin.db.services.commons.AggregateSnapshot;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(Include.NON_NULL)
@Setter
@Getter
public class TollTransactionsReport {
  
   
  private int dipcoinStatus;
  private String laneDirection;
  private String reconDate; 
  private String reconTime;
  private String transactionId;
  private String tollAmount;
  private String transactionDate;
  private String transactionTime;  
  private String tollPlazaName;
  private String ostaTransactionReferenceId;

}
