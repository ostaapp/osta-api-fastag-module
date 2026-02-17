package com.dipcoin.api.model;

import java.math.BigDecimal;
import java.util.List;
import com.dipcoin.db.services.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;


@Accessors(chain = true)
@Setter
@Getter
public class RequestMetadata {

  private String bankTransaction;

  private String dipcoinTransaction;

  private String dipcoin;
  
  private String bankRawRequest;
  
  private String bankRawResponse;
  
  private String methodName;
  
  private String bankTransactionType;
  
  private String dipcoinTransactionType;
  
  private BigDecimal amount;

  public RequestMetadata(String methodName) {
    this.methodName = methodName;
  }
  
}
