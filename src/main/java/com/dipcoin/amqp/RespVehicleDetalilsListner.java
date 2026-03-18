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
import com.dipcoin.api.resource.TollNetcResource;
import com.dipcoin.commons.LogFormatter;

@Service
@RabbitListener(queues = RespVehicleDetalilsListner.QUEUE)
public class RespVehicleDetalilsListner {

  private static final Logger LOG = LogManager.getLogger(RespVehicleDetalilsListner.class);

  public final static String QUEUE = "respVehicleDetalilsQueue";
  public final static String EXCHANGE = "respVehicleDetalilsExchange";
  public final static String ROUTINGKEY = RabbitMqUtils.routingKey("respVehicleDetalilsRoute");

  @Autowired
  private TollNetcResource tollNetcResource;

  @RabbitHandler
  public void handle(@Payload RespVehicleDetalilsRabbitMQRequest request)
      throws APIException {

    try {
      if (request != null) {
        //tollNetcResource.respVehicleDetalils(request.getRespVehicleDetalils(), request.getTraceId());
        }
      
    } catch (Exception e) {
      LOG.error(
          LogFormatter.instance(request.getTraceId()).data("Error processing request", e).format());
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
