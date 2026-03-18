package com.dipcoin.amqp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.resource.BrontooResource;
import com.dipcoin.api.resource.TollNetcResource;
import com.dipcoin.commons.LogFormatter;


@Service
@RabbitListener(queues = FastagDebitListener.QUEUE)
public class FastagDebitListener {
	 
	 private static final Logger LOG = LogManager.getLogger(FastagDebitListener.class);

	  public final static String QUEUE = "debitsReqpayQueue";
	  public final static String EXCHANGE = "debitsReqpayExchange";
	  public final static String ROUTINGKEY = RabbitMqUtils.routingKey("debitsReqpayRoute");

	  @Autowired
	  private BrontooResource brontooResource;

	  @RabbitHandler
	  public void handle(@Payload FastagDebitRabbitMQRequest request)
	      throws APIException {

	    try {
            LOG.debug(LogFormatter.instance(request.getTraceId()).data("request", request).format());

	      if (request != null) {
	    	  
	    	  brontooResource.fastagDebitJob(request.getPartnerTransactionId(),request.getTraceId());
	            LOG.debug(LogFormatter.instance(request.getTraceId()).data("Sendingrequest", request).format());

	        }
	      
	    } catch (Exception e) {
	      LOG.error(
	          LogFormatter.instance().data("Error processing request", e).format());
	    }
	  }

	  @PostConstruct
	  public void construct() {
	    LOG.info(LogFormatter.instance()
	        .message("Constructing " + this.getClass().getSimpleName() + " ... ").data("Queue", QUEUE)
	        .data("Exchange", EXCHANGE).data("RoutingKey", ROUTINGKEY).format());
	  }

	  @PreDestroy
	  public void cleanUp() {
	    LOG.info(LogFormatter.instance()
	        .message("Cleaning up " + this.getClass().getSimpleName() + " ...").format());
	  }

}
