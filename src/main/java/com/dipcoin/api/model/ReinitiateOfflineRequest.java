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
public class ReinitiateOfflineRequest extends APIRequest {

  private List<Integer> dipcoinIds;

  private Long startTime;

  private Long endTime;

  private Boolean invoice = Boolean.FALSE;


  @Override
  public boolean validate(HttpServletContext httpServletContext) {

    if (!this.invoice && CollectionUtils.isEmpty(dipcoinIds)) {
      this.setErrorCode(HeaderCode.EMPTY_REQUEST);
      return false;
    }
    
    if(this.invoice) {
      
      if (this.startTime == null || this.startTime <= 0) {
        this.setErrorCode(HeaderCode.EMPTY_REQUEST);
        return false;
      }
      
      if (this.endTime == null || this.endTime <= 0) {
        this.setErrorCode(HeaderCode.EMPTY_REQUEST);
        return false;
      }
      
    }

    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub

  }
}
