package com.dipcoin.api.model;

import java.util.List;

public class ChargeBackList extends APIResponse {

  private List<ChargebackResponse> chargebackResponse;

  private Pagination pagination;


  public List<ChargebackResponse> getChargebackResponse() {
    return chargebackResponse;
  }

  public void setChargebackResponse(List<ChargebackResponse> chargebackResponse) {
    this.chargebackResponse = chargebackResponse;
  }


  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

}
