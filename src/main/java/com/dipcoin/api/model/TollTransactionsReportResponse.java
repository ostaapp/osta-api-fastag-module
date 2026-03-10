package com.dipcoin.api.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(Include.NON_NULL)
@Setter
@Getter
public class TollTransactionsReportResponse extends APIResponse{
  
  private List<TollTransactionsReport> transactions;
  private Pagination pagination;
  private Map<String,Object> vehicleInfo;

}
