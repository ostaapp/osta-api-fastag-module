package com.dipcoin.partner.paymentGateway.model;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@JsonIgnoreProperties(ignoreUnknown = true)
public class AggrepayPaymentStatusResponse {
  private static final Logger LOG = LogManager.getLogger(AggrepayPaymentStatus.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();


  private ArrayList<AggrepayPaymentStatus> data;
  private String hash;
  private Error error;

  public class Error {
    private String code;
    private String message;

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

  }

  public ArrayList<AggrepayPaymentStatus> getData() {
    return data;
  }

  public void setData(ArrayList<AggrepayPaymentStatus> data) {
    this.data = data;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Error getError() {
    return error;
  }

  public void setError(Error error) {
    this.error = error;
  }

  public String toString() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      LOG.error("Exception", e);
    }

    return null;
  }

}
