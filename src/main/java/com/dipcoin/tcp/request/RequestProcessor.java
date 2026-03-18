package com.dipcoin.tcp.request;

import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIFeatureFlags;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.filter.HttpServletContext.ClientFeatureFlags;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.tcp.TCPMessageRequest;
import com.dipcoin.tcp.TCPMessageResponse;
import com.dipcoin.tcp.utils.FieldMapper;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public abstract class RequestProcessor<T> {
  private static final Logger LOG = LogManager.getLogger(RequestProcessor.class);

  @Autowired
  private APIFeatureFlags apiFeatureFlags;

  @Autowired
  private Environment environment;

  private String partnerReferenceId;
  private FieldMapper fieldMapper;
  private int headerLength = 0;

  private HttpServletContext initRequestContext(String origin) {
    final ClientFeatureFlags clientFeatureFlags = ClientFeatureFlags.instance();
    clientFeatureFlags.setNotificationEnabled(apiFeatureFlags.notificationEnabled());
    HttpServletContext httpServletContext = HttpServletContext.instance();
    httpServletContext.setClientFeatureFlags(clientFeatureFlags);
    httpServletContext.setEnvironment(environment);
    httpServletContext.setOriginIp(origin);

    return httpServletContext;
  }

  public final TCPMessageResponse process(TCPMessageRequest tcpRequest) {
    TCPMessageResponse tcpResponse = TCPMessageResponse.init(tcpRequest);

    if (tcpRequest == null || !tcpRequest.validate()) {
      LOG.error("Socket/PartnerReferenceId not set");
      return tcpResponse;
    }

    HttpServletContext httpServletContext = initRequestContext(tcpRequest.getOriginIP());
    String traceId = tcpRequest.getTraceId();
    httpServletContext.setTraceId(traceId);

    if (!init()) {
      LOG.error("RequestProcessor initialization failed");
      return tcpResponse;
    }

    T request = null;
    try {

      byte[] message = tcpRequest.getData();
      int messageLength = message.length;

      LOG.info(LogFormatter.instance(traceId)
          .data("PartnerReferenceId", tcpRequest.getPartnerReferenceId())
          .data("length", messageLength).data("headerLength", headerLength)
          .data("message", new String(message, StandardCharsets.UTF_8)).format());


      if (headerLength > 0) {
        byte[] header = Arrays.copyOfRange(message, 0, headerLength);
        // @NOTE - for now we assume we getting only header as data length
        messageLength = Integer.parseInt(BaseEncoding.base16().lowerCase().encode(header), 16);

        // get the message bytes
        message = Arrays.copyOfRange(message, headerLength, message.length);
      }

      LOG.info(LogFormatter.instance(traceId).data("messageLength", messageLength)
          .data("message.length", message.length).format());

      byte[] response = new byte[0];

      // if valid message received
      if (messageLength == message.length) {
        request = parseRequest(message, messageLength);
        response = handleRequest(request);
        LOG.debug(LogFormatter.instance(traceId)
            .message("Response " + new String(response, StandardCharsets.UTF_8)).format());
      }

      tcpResponse.setData(response);
    } catch (Exception e) {
      LOG.error(LogFormatter.instance(traceId).message("Exception caught").format(), e);
      byte[] response = handleError(e, request);
      tcpResponse.setData(response);
    }

    return tcpResponse;
  }


  protected final String getPartnerReferenceId() {
    return partnerReferenceId;
  }

  public void setPartnerReferenceId(String partnerReferenceId) {
    this.partnerReferenceId = partnerReferenceId;
  }

  public FieldMapper getFieldMapper() {
    return fieldMapper;
  }

  public void setFieldMapper(FieldMapper fieldMapper) {
    this.fieldMapper = fieldMapper;
  }

  public void setHeaderLength(int headerLength) {
    this.headerLength = headerLength;
  }

  protected abstract boolean init();

  protected abstract T parseRequest(byte[] data, int size) throws Exception, APIException;

  protected abstract byte[] handleRequest(final T request) throws Exception, APIException;

  protected abstract byte[] handleError(Throwable e, T request);
}
