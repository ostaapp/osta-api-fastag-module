package com.dipcoin.api.model;

import com.dipcoin.api.filter.HttpServletContext;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class BrontooUserRegisterRequest extends UserRegisterRequest {

  @ApiModelProperty(required = true)
  private String role;

  public final String getRole() {
    return role;
  }

  public final void setRole(String role) {
    this.role = role;
  }

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    if (StringUtils.isEmpty(role)) {
      return false;
    }
    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {}
}
