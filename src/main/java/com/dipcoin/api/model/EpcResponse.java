package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(Include.NON_NULL)
@Getter
@Setter
public class EpcResponse extends APIResponse {
  private String iin;
  private String serialNumber;
  private String rfidTag;
  private String tid;
  private String epcScheme;
  private String applicationIdentifier;
  private String tagSize;
  private String filterValue;
  private String partitionValue;
  private String prefixLength;
  private String companyPrefix;
  private String individualAssetReference;
  private String epcPureIdentityURI;
  private String epcTagURI;
  private String epcRawURI;
  private String binaryDetail;
  private String status;
  private String category;
  private Integer visibilityStatus;
  private Integer lotNumber;
  private String createdDateTime;
  private String updateTime;
  private String startSerialNumber;
  private String endSerialNumber;
  private int totalCount;
}
