package com.dipcoin.tcp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.dipcoin.amqp.RabbitMqUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.tcp.request.RequestProcessor;
import com.dipcoin.tcp.utils.TCPConfig;
import com.dipcoin.tcp.utils.TCPConfigFactory;

@Service
/*
 * @RabbitListener(bindings = @QueueBinding( // value = @Queue(value = TCPMessageListener.Queue,
 * durable = "true"), // exchange = @Exchange(value = TCPMessageListener.Exchange, type = "direct",
 * durable = "true")))
 */
@RabbitListener(queues = TCPMessageListener.QUEUE)
public class TCPMessageListener {

  private static final Logger LOG = LogManager.getLogger(TCPMessageListener.class);

  public final static String QUEUE = "tcpMessageQueue";
  public final static String EXCHANGE = "tcpMessageExchange";
  public final static String ROUTINGKEY = RabbitMqUtils.routingKey("tcpMessageRoute");

  @Autowired
  private TCPConfigFactory tcpConfigFactory;

  @Autowired
  private RequestProcessorFactory tcpRequestProcessorFactory;

  public static String configReference(String partnerReferenceId) {
    return String.format("config%s", partnerReferenceId);
  }

  @RabbitHandler
  public TCPMessageResponse handle(@Payload TCPMessageRequest request) {

    try {
      if (request == null || !request.validate()) {
        return new TCPMessageResponse(TCPMessageResponse.Status.BAD_REQUEST);
      }

      TCPConfig config =
          tcpConfigFactory.getConfig(configReference(request.getPartnerReferenceId()));
      if (config == null) {
        LOG.info(LogFormatter.instance(request.getTraceId()).message("Partner not configured")
            .data("PartnerReferenceId", request.getPartnerReferenceId()).format());
        TCPMessageResponse response =
            new TCPMessageResponse(TCPMessageResponse.Status.INTERNAL_ERROR);
        response.setTraceId(request.getTraceId());
        return response;
      }

      LOG.info(LogFormatter.instance(request.getTraceId()).data("Config", config)
          .data("message", request.getData()).format());

      RequestProcessor<?> processor = tcpRequestProcessorFactory
          .getProcessor(config.getRequestProcessorClass().getSimpleName());
      processor.setPartnerReferenceId(request.getPartnerReferenceId());
      processor.setFieldMapper(config.getFieldMapper());
      processor.setHeaderLength(config.getHeaderLength());

      return processor.process(request);
    } catch (Exception e) {
      LOG.error(LogFormatter.instance(request.getTraceId()).message("Exception caught").format(),
          e);

      TCPMessageResponse response =
          new TCPMessageResponse(TCPMessageResponse.Status.INTERNAL_ERROR);
      response.setTraceId(request.getTraceId());
      return response;
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
