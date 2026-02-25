package com.dipcoin.api.utils;

public class RechargeServiceException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private ErrorCode code;

  public static enum ErrorCode {
    // list of error codes


  }

  public RechargeServiceException(String msg) {
    super(msg);
  }

  public RechargeServiceException(ErrorCode code, String msg) {
    super(msg);
    this.code = code;
  }

  public ErrorCode getErrorCode() {
    return this.code;
  }

}
