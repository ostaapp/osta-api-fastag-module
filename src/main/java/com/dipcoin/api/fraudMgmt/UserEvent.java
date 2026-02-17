package com.dipcoin.api.fraudMgmt;

import java.util.Map;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIRequest;

public class UserEvent extends APIRequest {

  private Map<String, EventStatus> state;

  private Metadata metadata;

  public Map<String, EventStatus> getState() {
    return state;
  }

  public void setState(Map<String, EventStatus> state) {
    this.state = state;
  }



  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }


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
