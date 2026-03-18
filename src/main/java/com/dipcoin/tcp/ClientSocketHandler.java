package com.dipcoin.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate.RabbitConverterFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.concurrent.ListenableFutureCallback;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.tcp.utils.TCPConfig;
import com.dipcoin.tcp.utils.TCPConfigFactory;
import com.google.common.primitives.Bytes;

public class ClientSocketHandler implements Runnable {
  private static final Logger LOG = LogManager.getLogger(ClientSocketHandler.class);

  @Autowired
  private TCPConfigFactory tcpConfigFactory;
  
  public static class MessageCallbackListener
      implements ListenableFutureCallback<TCPMessageResponse> {

    private ClientSocketHandler handler;

    public MessageCallbackListener(ClientSocketHandler handler) {
      this.handler = handler;
    }

    @Override
    public void onSuccess(TCPMessageResponse response) {
      String traceId = response.getTraceId();
      try {
        LOG.debug(LogFormatter.instance(traceId)
            .message("response " + new String(response.getData(), StandardCharsets.UTF_8))
            .data("length", response.getData().length).format());

        handler.writeResponse(traceId, response.getData());
      } catch (IOException e) {
        LOG.error(LogFormatter.instance(traceId).message("Exception caught").format(), e);
      }
    }

    @Override
    public void onFailure(Throwable e) {
      LOG.error(LogFormatter.instance().message("Exception caught").format(), e);
    }

  }

  private AsyncRabbitTemplate tcpMessageAsyncAmqpTemplate;

  private Socket socket;
  private String partnerReferenceId;
  private int headerLength = 0;
  private boolean keepSocketAlive = false;

  public ClientSocketHandler(String partnerReferenceId, Socket socket) {
    this.partnerReferenceId = partnerReferenceId;
    this.socket = socket;
  }

  public void setKeepSocketAlive(boolean keepSocketAlive) {
    this.keepSocketAlive = keepSocketAlive;
  }

  public void setTcpMessageAsyncAmqpTemplate(AsyncRabbitTemplate tcpMessageAsyncAmqpTemplate) {
    this.tcpMessageAsyncAmqpTemplate = tcpMessageAsyncAmqpTemplate;
  }

  private void writeResponse(String traceId, byte[] response) throws IOException {
    synchronized (socket) {
      DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
      int responseLength = response.length;
      TCPConfig config = tcpConfigFactory.getConfig(String.format("config%s", partnerReferenceId));
      if (config.getHeaderLength() > 0) {
        // append response length as header
        ByteBuffer bb = ByteBuffer.allocate(headerLength);
        bb.putShort((short) responseLength);
        response = Bytes.concat(bb.array(), response);
      }

      LOG.info(LogFormatter.instance(traceId).data("headerLength", headerLength)
          .data("responseLength", responseLength).data("response.length", response.length)
          .format());

      dOut.write(response);
      dOut.flush();
    }
  }

  @Override
  public final void run() {
    String traceId = APIUtils.generateTraceId();

    LOG.debug(LogFormatter.instance(traceId)
        .message("Socket: " + socket.getPort() + " -> " + socket.getLocalPort()).format());
    if (socket == null || StringUtils.isEmpty(partnerReferenceId)) {
      LOG.error(
          LogFormatter.instance(traceId).message("Socket/PartnerReferenceId not set").format());
      return;
    }

    try {
      // if socket needs to be closed after each processing
      if (!keepSocketAlive) {
        socket.setSoTimeout(10000);
      }

      DataInputStream dIn = new DataInputStream(socket.getInputStream());

      while (socket.isConnected() && Thread.currentThread().isAlive()
          && !Thread.currentThread().isInterrupted()) {
        try {

          // if multiple requests on same socket, generate a new traceId
          if (keepSocketAlive)
            traceId = APIUtils.generateTraceId();

          int length = dIn.available();
          if (length > 0) {
            byte[] data = new byte[length];
            dIn.readFully(data, 0, length);

            LOG.debug(LogFormatter.instance(traceId)
                .data("message", new String(data, StandardCharsets.UTF_8)).data("length", length)
                .format());

            TCPMessageRequest message = new TCPMessageRequest();
            message.setData(data);
            message.setOriginIP(socket.getRemoteSocketAddress().toString());
            message.setPartnerReferenceId(partnerReferenceId);
            message.setTraceId(traceId);

            RabbitConverterFuture<TCPMessageResponse> future =
                tcpMessageAsyncAmqpTemplate.convertSendAndReceive(TCPMessageListener.EXCHANGE,
                    TCPMessageListener.ROUTINGKEY, message);
            future.addCallback(new MessageCallbackListener(this));

          }
        } catch (Exception e) {
          LOG.error(LogFormatter.instance(traceId).message("Exception caught").format(), e);
          byte[] response = new byte[0];
          writeResponse(traceId, response);

          // if socket needs to be closed after each processing
          if (!keepSocketAlive) {
            break;
          }
        }
      }

    } catch (Exception e) {
      // @NOTE - reaching this part of code will result in timeout on client side.
      LOG.error(LogFormatter.instance(traceId).message("Exception caught").format(), e);
    }
  }
}
