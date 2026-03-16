package com.dipcoin.partner.paymentGateway.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AggrepayPaymentStatus {
  private static final Logger LOG = LogManager.getLogger(AggrepayPaymentStatus.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private String transaction_id;
  private String bank_code;
  private String payment_mode;
  private String payment_channel;
  private String payment_datetime;
  private String response_code;
  private String response_message;
  private String authorization_staus;
  private String captured;
  private String order_id;
  private String amount;
  private String amount_orig;
  private String tdr_amount;
  private String tax_on_tdr_amount;
  private String description;
  private String error_desc;
  private String customer_phone;
  private String customer_name;
  private String customer_email;
  private error Error;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class error {
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


  public error getError() {
    return Error;
  }

  public void setError(error error) {
    Error = error;
  }

  public String getTransaction_id() {
    return transaction_id;
  }

  public void setTransaction_id(String transaction_id) {
    this.transaction_id = transaction_id;
  }

  public String getBank_code() {
    return bank_code;
  }

  public void setBank_code(String bank_code) {
    this.bank_code = bank_code;
  }

  public String getPayment_mode() {
    return payment_mode;
  }

  public void setPayment_mode(String payment_mode) {
    this.payment_mode = payment_mode;
  }

  public String getPayment_channel() {
    return payment_channel;
  }

  public void setPayment_channel(String payment_channel) {
    this.payment_channel = payment_channel;
  }

  public String getPayment_datetime() {
    return payment_datetime;
  }

  public void setPayment_datetime(String payment_datetime) {
    this.payment_datetime = payment_datetime;
  }

  public String getResponse_code() {
    return response_code;
  }

  public void setResponse_code(String response_code) {
    this.response_code = response_code;
  }

  public String getResponse_message() {
    return response_message;
  }

  public void setResponse_message(String response_message) {
    this.response_message = response_message;
  }

  public String getAuthorization_staus() {
    return authorization_staus;
  }

  public void setAuthorization_staus(String authorization_staus) {
    this.authorization_staus = authorization_staus;
  }

  public String getCaptured() {
    return captured;
  }

  public void setCaptured(String captured) {
    this.captured = captured;
  }

  public String getOrder_id() {
    return order_id;
  }

  public void setOrder_id(String order_id) {
    this.order_id = order_id;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String getAmount_orig() {
    return amount_orig;
  }

  public void setAmount_orig(String amount_orig) {
    this.amount_orig = amount_orig;
  }

  public String getTdr_amount() {
    return tdr_amount;
  }

  public void setTdr_amount(String tdr_amount) {
    this.tdr_amount = tdr_amount;
  }

  public String getTax_on_tdr_amount() {
    return tax_on_tdr_amount;
  }

  public void setTax_on_tdr_amount(String tax_on_tdr_amount) {
    this.tax_on_tdr_amount = tax_on_tdr_amount;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getError_desc() {
    return error_desc;
  }

  public void setError_desc(String error_desc) {
    this.error_desc = error_desc;
  }

  public String getCustomer_phone() {
    return customer_phone;
  }

  public void setCustomer_phone(String customer_phone) {
    this.customer_phone = customer_phone;
  }

  public String getCustomer_name() {
    return customer_name;
  }

  public void setCustomer_name(String customer_name) {
    this.customer_name = customer_name;
  }

  public String getCustomer_email() {
    return customer_email;
  }

  public void setCustomer_email(String customer_email) {
    this.customer_email = customer_email;
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
