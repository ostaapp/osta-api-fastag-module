package com.dipcoin.api.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DipcoinsResponse extends APIResponse {

  private List<DipcoinResponse> ostas;
  private Pagination pagination;

  public List<DipcoinResponse> getOstas() {
    return ostas;
  }

  public void setOstas(List<DipcoinResponse> ostas) {
    this.ostas = ostas;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

}
