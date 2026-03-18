package com.dipcoin.tcp.utils;

import com.dipcoin.tcp.request.RequestProcessor;

public class TCPConfig {

  private String partnerReferenceId;
  private FieldMapper fieldMapper;
  private int headerLength;
  private Class<RequestProcessor<?>> requestProcessorClass;

  public String getPartnerReferenceId() {
    return partnerReferenceId;
  }

  public void setPartnerReferenceId(String partnerReferenceId) {
    this.partnerReferenceId = partnerReferenceId;
  }

  public FieldMapper getFieldMapper() {
    return fieldMapper;
  }

  public void setFieldMapper(FieldMapper fieldMapper) {
    this.fieldMapper = fieldMapper;
  }

  public int getHeaderLength() {
    return headerLength;
  }

  public void setHeaderLength(int headerLength) {
    this.headerLength = headerLength;
  }

  public Class<RequestProcessor<?>> getRequestProcessorClass() {
    return requestProcessorClass;
  }

  public void setRequestProcessorClass(Class<RequestProcessor<?>> requestProcessorClass) {
    this.requestProcessorClass = requestProcessorClass;
  }

}
