package com.dipcoin.api.commons;

import com.dipcoin.api.model.APIResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

public class APIException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static ObjectMapper objectMapper = new ObjectMapper();

  private HttpStatus code;
  private APIResponse response;

  public APIException(HttpStatus code, APIResponse response) {
    super();

    this.code = code;
    this.response = response;
  }

  public APIException(HttpStatus code, APIResponse response, Throwable t) {
    super(t);

    this.code = code;
    this.response = response;
  }

  public HttpStatus getCode() {
    return this.code;
  }

  public APIResponse getResponse() {
    return this.response;
  }

  @Override
  public String toString() {
    String data = null;
    try {
      data = objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
    }

    return new StringBuffer("[APIException]: Code - ").append(code).append(", Response - ")
        .append(data).append("\n").append(this.getCause() != null ? this.getCause().toString() : "")
        .toString();
  }
}
