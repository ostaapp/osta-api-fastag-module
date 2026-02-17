package com.dipcoin.api.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Setter
@Getter
public class AspectContext {

  public AspectContext(boolean runAudit) {
    this.runAudit = runAudit;
  }

  private String audit;

  private String userId;

  private RequestMetadata requestMetadata;
  
  private String customerAccountId;
  
  private boolean runAudit;
  
}
