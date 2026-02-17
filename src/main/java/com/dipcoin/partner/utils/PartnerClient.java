package com.dipcoin.partner.utils;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.partner.recharge.comm.PartnerRequest;
import com.dipcoin.partner.recharge.comm.PartnerResponse;

public abstract class PartnerClient {

  private static final Logger LOG = LogManager.getLogger(PartnerClient.class);


  public static enum Protocol {
    HTTP("http", false), HTTPS("https", true), TCP("tcp", false), STCP("stcp", true);

    private static Map<String, Protocol> lookup = new HashMap<>();
    static {
      lookup.put(HTTP.type, HTTP);
      lookup.put(HTTPS.type, HTTPS);
      lookup.put(TCP.type, TCP);
      lookup.put(STCP.type, STCP);
    }

    private final String type;
    private boolean isSecure;

    private Protocol(String type, boolean isSecure) {
      this.type = type;
      this.isSecure = isSecure;
    }

    public String type() {
      return this.type;
    }

    public boolean isSecure() {
      return isSecure;
    }

    public static Protocol lookup(String protocol) {
      return lookup.get(protocol);
    }


  }

  public enum ChecksumFormat {
    MD5, CRC32, SHA256;
  }

  private ChecksumFormat checksumFormat = ChecksumFormat.SHA256;

  public ChecksumFormat getChecksumFormat() {
    return checksumFormat;
  }

  public void setChecksumFormat(ChecksumFormat checksumFormat) {
    this.checksumFormat = checksumFormat;
  }

  private String host;
  private int port;
  private int requestTimeout;
  private PartnerRequest partnerRequest;
  private PartnerResponse partnerResponse;

  private Protocol protocol;

  public String getHost() {
    return this.host;
  }

  public Integer getPort() {
    return this.port;
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public PartnerRequest getPartnerRequest() {
    return partnerRequest;
  }

  public void setPartnerRequest(PartnerRequest partnerRequest) {
    this.partnerRequest = partnerRequest;
  }

  public PartnerResponse getPartnerResponse() {
    return partnerResponse;
  }

  public void setPartnerResponse(PartnerResponse partnerResponse) {
    this.partnerResponse = partnerResponse;
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public PartnerClient(Protocol protocol, String host) {
    this.protocol = protocol;
    this.host = host;
  }
}
