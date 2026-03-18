package com.dipcoin.api.model;

import com.dipcoin.api.filter.HttpServletContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class ReconHeadersFieldMapperRequest extends APIRequest {

  private String partnerReferenceId;
  private Long startTime;
  private Long endTime;
  private int count;
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
