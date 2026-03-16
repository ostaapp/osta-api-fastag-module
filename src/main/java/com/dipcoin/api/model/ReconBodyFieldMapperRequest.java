package com.dipcoin.api.model;

import java.math.BigDecimal;
import com.dipcoin.api.filter.HttpServletContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class ReconBodyFieldMapperRequest extends APIRequest {

  private String partnerTransactionReferenceId;
  private String orderId ;
  private String timeStamp;
  private String ostaTransactionReferenceId ;
  private BigDecimal amount;
  private int status;
  private int type;
  
  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub
    
  }
  
}
