package com.dipcoin.amqp;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class FastagDebitRabbitMQRequest {

private static ObjectMapper mapper = new ObjectMapper();

	private List <String>partnerTransactionId;
	private String traceId;
	
	@Override
	public String toString() {
		try {
			mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
		}
		return null;
	}
}
