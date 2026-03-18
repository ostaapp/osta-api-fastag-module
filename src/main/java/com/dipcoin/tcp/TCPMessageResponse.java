package com.dipcoin.tcp;

public class TCPMessageResponse {

  public enum Status {
    SUCCESS(0), BAD_REQUEST(1), INTERNAL_ERROR(2);

    int code;

    private Status(int code) {
      this.code = code;
    }

    public int code() {
      return this.code;
    }
  }

  private int code = Status.SUCCESS.code;
  private byte[] data;
  private String partnerReferenceId;
  private String traceId;

  public TCPMessageResponse() {

  }

  public TCPMessageResponse(Status status) {
    this.code = status.code;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public String getPartnerReferenceId() {
    return partnerReferenceId;
  }

  public void setPartnerReferenceId(String partnerReferenceId) {
    this.partnerReferenceId = partnerReferenceId;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }


  public static TCPMessageResponse init(TCPMessageRequest request) {
    TCPMessageResponse response = new TCPMessageResponse();
    response.setPartnerReferenceId(request.getPartnerReferenceId());
    response.setTraceId(request.getTraceId());

    return response;
  }
}
