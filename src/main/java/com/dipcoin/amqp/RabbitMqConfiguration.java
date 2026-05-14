package com.dipcoin.amqp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.RequestContextListener;
import com.dipcoin.commons.EncryptedPropertiesUtils;
import com.dipcoin.commons.HttpsUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.LogFormatter.Mask;
import com.dipcoin.tcp.TCPMessageListener;
import com.fasterxml.jackson.databind.ObjectMapper;


@Profile("!unit")
@Configuration
@EnableRabbit
public class RabbitMqConfiguration {

  private static final Logger LOG = LogManager.getLogger(RabbitMqConfiguration.class);

  @Profile("dev")
  public class DevProfile {
    @Bean("amqpEncryptedPropertiesUtils")
    public EncryptedPropertiesUtils amqpEncryptedPropertiesUtils() {
      return new EncryptedPropertiesUtils();
    }
  }

  @Profile("!dev")
  public class ProdProfile {
    @Bean("amqpEncryptedPropertiesUtils")
    public EncryptedPropertiesUtils amqpEncryptedPropertiesUtils() {
      return new EncryptedPropertiesUtils(System.getProperty("ASP"));
    }
  }

  /*
   * Base Config
   */

  @Value("${com.dipcoin.amqp.RabbitMqConfiguration.hostname}")
  private String hostname;
  @Value("${com.dipcoin.amqp.RabbitMqConfiguration.user}")
  private String user;
  @Value("${com.dipcoin.amqp.RabbitMqConfiguration.vhost}")
  private String vhost;

  @Value("${com.dipcoin.amqp.RabbitMqConfiguration.uri:}")
  private String uri;
  @Value("${com.dipcoin.amqp.RabbitMqConfiguration.validate-server-certificate:false}")
  private boolean validateServerCertificate;
  @Value("${com.dipcoin.amqp.listener.auto-startup:true}")
  private boolean listenerAutoStartup;

  @Value("#{amqpEncryptedPropertiesUtils.decrypt('${com.dipcoin.amqp.RabbitMqConfiguration.password}')}")
  private String password;
  @Bean
  public RequestContextListener requestContextListener() {
      return new RequestContextListener();
  }
  
  @Bean
  public CachingConnectionFactory connectionFactory() {
    LOG.debug(LogFormatter.instance().message("Configuring RabbitMQ ConnectionFactory")
        .data("hostname", hostname).data("user", user).data("vhost", vhost)
        .data("uriConfigured", hasText(uri))
        .data("validateServerCertificate", validateServerCertificate)
        .data("listenerAutoStartup", listenerAutoStartup)
        .maskedData("password", Mask.PASSWORD_MASKED.value()).format());
    try {
      com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory =
          new com.rabbitmq.client.ConnectionFactory();
      if (hasText(uri)) {
        rabbitConnectionFactory.setUri(uri);
      } else {
        rabbitConnectionFactory.setHost(hostname);
      }
      rabbitConnectionFactory.setUsername(user);
      rabbitConnectionFactory.setPassword(password);
      rabbitConnectionFactory.setVirtualHost(vhost);
      if (isAmqpsUri()) {
        configureSsl(rabbitConnectionFactory);
      }
      CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(rabbitConnectionFactory);
      if (rabbitConnectionFactory.getHost() != null) {
        cachingConnectionFactory.setHost(rabbitConnectionFactory.getHost());
      }
      cachingConnectionFactory.setPort(rabbitConnectionFactory.getPort());
      return cachingConnectionFactory;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to configure RabbitMQ ConnectionFactory", e);
    }
  }

  private void configureSsl(com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory)
      throws Exception {
    if (validateServerCertificate) {
      rabbitConnectionFactory.useSslProtocol();
      return;
    }

    javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
    sslContext.init(null, HttpsUtils.TRUST_ALL_CERTS, null);
    rabbitConnectionFactory.useSslProtocol(sslContext);
  }

  private boolean isAmqpsUri() {
    return hasText(uri) && uri.trim().regionMatches(true, 0, "amqps://", 0, 8);
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  @Bean(name = "rabbitListenerContainerFactory")
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerlistenerFactory() {
    LOG.debug(LogFormatter.instance().message("Configuring RabbitListenerContainerlistenerFactory")
        .format());
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory());
    factory.setMessageConverter(jsonMessageConverter()); // need for deserialization
    factory.setConcurrentConsumers(5);
    factory.setMaxConcurrentConsumers(20);
    return factory;
  }

  @Bean
  public AmqpAdmin amqpAdmin() {
    LOG.debug("Initializing Notification RabbitTemplate");
    return new RabbitAdmin(connectionFactory());
  }

  @Bean
  public Jackson2JsonMessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter(new ObjectMapper());
  }


  /*
   * Notification
   */
  public final static String notificationRouteKey = RabbitMqUtils.routingKey("notificationRoute");
  public final static String notificationQueue = "notificationQueue";
  public final static String notificationDirect = "notificationDirect";


  @Bean
  public Queue notificationAmqpQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing notificationQueue").format());
    return new Queue(notificationQueue, true);
  }

  @Bean
  public DirectExchange notificationAmqpDirect() {
    LOG.debug(LogFormatter.instance().message("Constructing notificationDirect").format());
    return new DirectExchange(notificationDirect);
  }

  @Bean
  public Binding notificationAmqpBinding(DirectExchange notificationAmqpDirect,
      Queue notificationAmqpQueue) {
    LOG.debug(LogFormatter.instance().message("Init Notification Binding")
        .data("notificationAmqpQueue", notificationAmqpQueue)
        .data("notificationAmqpDirect", notificationAmqpDirect)
        .data("notificationRouteKey", notificationRouteKey).format());
    return BindingBuilder.bind(notificationAmqpQueue).to(notificationAmqpDirect)
        .with(notificationRouteKey);
  }

  @Bean
  public RabbitTemplate notificationAmqpTemplate() {
    LOG.debug("Initializing Notification RabbitTemplate");
    RabbitTemplate template = new RabbitTemplate(connectionFactory());
    template.setReplyTimeout(20000L);
    template.setMessageConverter(jsonMessageConverter()); // need for serialization
    // template.setQueue(notificationQueue); getting error when java 13 and springboot 2.2.4
    return template;
  }

  /*
   * Autogenerate
   */

  public final static String ASYNC_REQUEST_QUEUE = "AsyncRequestQueue";

  @Bean
  public RabbitTemplate asyncRequestAmqpTemplate() {
    LOG.debug("Initializing Async Request RabbitTemplate");
    RabbitTemplate template = new RabbitTemplate(connectionFactory());
    template.setReplyTimeout(20000L);
    template.setMessageConverter(jsonMessageConverter()); // need for serialization
    // template.setQueue(ASYNC_REQUEST_QUEUE);
    return template;
  }

  @Bean
  public Queue asyncRequestQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing AsyncRequestQueue").format());
    return new Queue(ASYNC_REQUEST_QUEUE, true);
  }

  /*
   * TCP
   */
  @Bean
  public Queue tcpMessageAmqpQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing tcpMessageQueue").format());
    return new Queue(TCPMessageListener.QUEUE, true);
  }

  @Bean
  public DirectExchange tcpMessageAmqpDirect() {
    LOG.debug(LogFormatter.instance().message("Constructing tcpMessageDirect").format());
    return new DirectExchange(TCPMessageListener.EXCHANGE);
  }

  @Bean
  public Binding tcpMessageAmqpBinding(DirectExchange tcpMessageAmqpDirect,
      Queue tcpMessageAmqpQueue) {
    LOG.debug(LogFormatter.instance().message("Init TCPMessageRequest Binding")
        .data("tcpMessageAmqpQueue", tcpMessageAmqpQueue)
        .data("tcpMessageAmqpDirect", tcpMessageAmqpDirect)
        .data("tcpMessageRouteKey", TCPMessageListener.ROUTINGKEY).format());
    return BindingBuilder.bind(tcpMessageAmqpQueue).to(tcpMessageAmqpDirect)
        .with(TCPMessageListener.ROUTINGKEY);
  }

  @Bean
  public RabbitTemplate tcpMessageAmqpTemplate(ConnectionFactory connectionFactory,
      Jackson2JsonMessageConverter jsonMessageConverter) {
    LOG.debug("Initializing TCP Message RabbitTemplate");
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setReplyTimeout(20000L);
    template.setMessageConverter(jsonMessageConverter); // need for deserialization
    // template.setQueue(TCPMessageListener.QUEUE);
    return template;
  }

  @Bean
  public AsyncRabbitTemplate tcpMessageAsyncAmqpTemplate(RabbitTemplate tcpMessageAmqpTemplate) {
    LOG.debug("Initializing TCP Message AsyncRabbitTemplate");
    AsyncRabbitTemplate asyncRabbitTemplate = new AsyncRabbitTemplate(tcpMessageAmqpTemplate);
    asyncRabbitTemplate.setReceiveTimeout(10000);

    return asyncRabbitTemplate;
  }
  
  /*
   * Toll
   */
  
  @Bean
  public Queue tollRequestQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing TollRequestQueue").format());
      return new Queue(TollReqpayListener.QUEUE, true);
  }

  @Bean
  public DirectExchange tollExchange() {
    LOG.debug(LogFormatter.instance().message("Constructing TollExchange").format());
      return new DirectExchange(TollReqpayListener.EXCHANGE);
  }
  
  @Bean
  public Binding tollBinding(Queue tollRequestQueue, DirectExchange tollExchange) {
      return BindingBuilder.bind(tollRequestQueue).to(tollExchange).with(TollReqpayListener.ROUTINGKEY);
  }
  
  @Bean
  public RabbitTemplate tollReqpayRabbitTemplate() {
    LOG.debug("Initializing  tollReqpayRabbitTemplate");
      final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
      rabbitTemplate.setReplyTimeout(20000L);
      rabbitTemplate.setMessageConverter(jsonMessageConverter());
      return rabbitTemplate;
  }
  
	/* CreditAmount */
  
  @Bean
  public Queue creditRequestQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing creditRequestQueue").format());
      return new Queue(CreditListener.QUEUE, true);
  }

  @Bean
  public DirectExchange creditExchange() {
    LOG.debug(LogFormatter.instance().message("Constructing CreditExchange").format());
      return new DirectExchange(CreditListener.EXCHANGE);
  }
  
  @Bean
  public Binding creditBinding(Queue creditRequestQueue, DirectExchange creditExchange) {
      return BindingBuilder.bind(creditRequestQueue).to(creditExchange).with(CreditListener.ROUTINGKEY);
  }
  
  @Bean
  public RabbitTemplate creditRabbitTemplate() {
    LOG.debug("Initializing  creditRabbitTemplate");
      final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
      rabbitTemplate.setReplyTimeout(20000L);
      rabbitTemplate.setMessageConverter(jsonMessageConverter());
      return rabbitTemplate;
  }
  
  
  /*
   * Toll exception list
   */
  
  @Bean
  public Queue tollExceptionListQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing TollExceptionListQueue").format());
      return new Queue(TollExceptionListListener.QUEUE, true);
  }

  @Bean
  public DirectExchange tollExceptionListExchange() {
    LOG.debug(LogFormatter.instance().message("Constructing TollExceptionListExchange").format());
      return new DirectExchange(TollExceptionListListener.EXCHANGE);
  }
  
  @Bean
  public Binding tollExceptionListBinding(Queue tollExceptionListQueue, DirectExchange tollExceptionListExchange) {
      return BindingBuilder.bind(tollExceptionListQueue).to(tollExceptionListExchange).with(TollExceptionListListener.ROUTINGKEY);
  }
  
  @Bean
  public RabbitTemplate tollExceptionListRabbitTemplate() {
    LOG.debug("Initializing  tollExceptionListRabbitTemplate");
      final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
      rabbitTemplate.setReplyTimeout(20000L);
      rabbitTemplate.setMessageConverter(jsonMessageConverter());
      return rabbitTemplate;
  }
  
  /*
   * respVehicleDetalils req vehicle detail response for vahan
   */
  
  @Bean
  public Queue respVehicleDetalilsQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing RespVehicleDetalilsQueue").format());
      return new Queue(RespVehicleDetalilsListner.QUEUE, true);
  }

  @Bean
  public DirectExchange respVehicleDetalilsExchange() {
    LOG.debug(LogFormatter.instance().message("Constructing RespVehicleDetalilsExchange").format());
      return new DirectExchange(RespVehicleDetalilsListner.EXCHANGE);
  }
  
  @Bean
  public Binding respVehicleDetalilsBinding(Queue respVehicleDetalilsQueue, DirectExchange respVehicleDetalilsExchange) {
      return BindingBuilder.bind(respVehicleDetalilsQueue).to(respVehicleDetalilsExchange).with(RespVehicleDetalilsListner.ROUTINGKEY);
  }
  
  @Bean
  public RabbitTemplate respVehicleDetalilsRabbitTemplate() {
    LOG.debug("Initializing  respVehicleDetalilsRabbitTemplate");
      final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
      rabbitTemplate.setReplyTimeout(20000L);
      rabbitTemplate.setMessageConverter(jsonMessageConverter());
      return rabbitTemplate;
  }

  
  
  /*
   * Bank Callback
   */

  public final static String ASYNC_CALLBACK_REQUEST_QUEUE = "AsyncCallbackRequestQueue";

  @Bean
  public RabbitTemplate asyncCallbackRequestAmqpTemplate() {
    LOG.debug("Initializing Async callback Request RabbitTemplate");
    RabbitTemplate template = new RabbitTemplate(connectionFactory());
    template.setReplyTimeout(20000L);
    template.setMessageConverter(jsonMessageConverter()); // need for serialization
    // template.setQueue(ASYNC_REQUEST_QUEUE);
    return template;
  }

  @Bean
  public Queue asyncCallbackRequestQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing AsyncRequestQueue").format());
    return new Queue(ASYNC_CALLBACK_REQUEST_QUEUE, true);
  }
  
  
  /*
   * User Events for fraud MGMT
   */

  public final static String AsyncUserEventQueue = "AsyncUserEventQueue";

  @Bean
  public RabbitTemplate asyncUserEventAmqpTemplate() {
    LOG.debug("Initializing Async Request RabbitTemplate");
    RabbitTemplate template = new RabbitTemplate(connectionFactory());
    template.setReplyTimeout(20000L);
    template.setMessageConverter(jsonMessageConverter());
    return template;
  }
  
  
  @Bean
  public Queue asyncUserEventQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing AsyncRequestQueue").format());
    return new Queue(AsyncUserEventQueue, true);
  }

  /*
   * 
   */
  @PostConstruct
  public void construct() {
    LOG.debug(LogFormatter.instance()
        .message("Constructing " + this.getClass().getSimpleName() + " ...").format());
  }

  @PreDestroy
  public void cleanUp() {
    LOG.debug(LogFormatter.instance()
        .message("Cleaning up " + this.getClass().getSimpleName() + " ...").format());
  }
  
  
  /*
   * Fastag Debit sheet 
   */
  
  @Bean
  public Queue debitsRequestQueue() {
    LOG.debug(LogFormatter.instance().message("Constructing debitsRequestQueue").format());
      return new Queue(FastagDebitListener.QUEUE, true);
  }

  @Bean
  public DirectExchange debitsExchange() {
    LOG.debug(LogFormatter.instance().message("Constructing debitsExchange").format());
      return new DirectExchange(FastagDebitListener.EXCHANGE);
  }
  
  @Bean
  public Binding debitsBinding(Queue debitsRequestQueue, DirectExchange debitsExchange) {
      return BindingBuilder.bind(debitsRequestQueue).to(debitsExchange).with(FastagDebitListener.ROUTINGKEY);
  }
  

  @Bean("debitsReqpayRabbitTemplate")
  @Qualifier("debitsReqpayRabbitTemplate")
  public RabbitTemplate debitsReqpayRabbitTemplate() {
    LOG.debug("Initializing  debitsReqpayRabbitTemplate");
      final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
      rabbitTemplate.setReplyTimeout(20000L);
      rabbitTemplate.setMessageConverter(jsonMessageConverter());
      return rabbitTemplate;
  }
  

}
