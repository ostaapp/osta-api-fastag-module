package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Getter
@Setter
public class CustomerBankVehicleVerificationResponse extends APIResponse {

  private String bankName;
  private String serialNumber;
  private String regNumber;
  private String vehicleClass;
  private String tagStatus;
  private String issueDate;
  private String excCode;
  private String vehicleType;

}
