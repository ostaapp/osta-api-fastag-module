package com.dipcoin.api.model;

import com.dipcoin.api.filter.HttpServletContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class BankSourceValidator extends APIRequest {
  private String url;
  private String iosBunddleId;
  private String androidBunddleId;

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
