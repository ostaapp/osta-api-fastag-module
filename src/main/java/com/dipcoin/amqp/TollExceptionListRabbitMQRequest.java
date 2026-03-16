package com.dipcoin.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TollExceptionListRabbitMQRequest {
  private static ObjectMapper mapper = new ObjectMapper();

  private String traceId;
  private String tollExceptionList;
  
  @Override
  public String toString() {
    try {
      mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
    }
    return null;
  }
}
