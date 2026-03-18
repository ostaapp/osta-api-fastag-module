package com.dipcoin.amqp;

import com.dipcoin.api.model.CreditRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreditRabbitMQRequest {
	 private static ObjectMapper mapper = new ObjectMapper();

	  private String traceId;
	  private CreditRequest creditReq;
	  
	  @Override
	  public String toString() {
	    try {
	      mapper.writeValueAsString(this);
	    } catch (JsonProcessingException e) {
	    }
	    return null;
	  }
}
