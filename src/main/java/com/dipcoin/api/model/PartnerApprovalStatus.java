package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class PartnerApprovalStatus {

  @NoArgsConstructor
  @Accessors(chain = true)
  @Setter
  @Getter
  public static class Approval {
    private Integer status;
    private String description;
    private Integer updatedBy;
    private String updatedTime;
  }

  @JsonIgnore
  private Approval approval = new Approval();

  public Approval getApproval() {
    return approval;
  }

  public void setApproval(Integer status, Integer userId, String description) {
    this.approval.setStatus(status);
    this.approval.setUpdatedBy(userId);
    this.approval.setUpdatedTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
    this.approval.setDescription(description);
  }

  @JsonProperty("LEAD")
  private Approval lead;

  @JsonProperty("FORM")
  private Approval form;

  @JsonProperty("DOCUMENT")
  private Approval document;

  @JsonProperty("BANK_ACCOUNT")
  private Approval bankAccount;

  @JsonProperty("PAYMENT")
  private Approval payment;

  @JsonProperty("INTEGRATION")
  private Approval integration;
}
