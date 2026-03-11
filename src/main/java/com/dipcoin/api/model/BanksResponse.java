package com.dipcoin.api.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class BanksResponse extends APIResponse {

  private List<BankInfoResponse> banks;
  private Pagination pagination;

  public List<BankInfoResponse> getBanks() {
    return banks;
  }

  public void setBanks(List<BankInfoResponse> banks) {
    this.banks = banks;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

}
