package com.dipcoin.api.model;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class BankTransactionReport extends APIRequest {

  private List<String> mailIds;

  private Long startTime;

  private Long endTime;
  
  private Boolean isCommission = Boolean.FALSE;
  
  private Integer bankId;
  
  private Integer type;
  
  List<Integer> statuses;
  
  String bankTransactionId;

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    
    if(this.getMailIds().isEmpty()) {
      this.setErrorCode(HeaderCode.EMPTY_REQUEST);
      return false;
    }
    
    if (this.startTime == null || this.startTime <= 0) {
      this.setErrorCode(HeaderCode.EMPTY_REQUEST);
      return false;
    }
    
    if (this.endTime == null || this.endTime <= 0) {
      this.setErrorCode(HeaderCode.EMPTY_REQUEST);
      return false;
    }

    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub

  }
}
