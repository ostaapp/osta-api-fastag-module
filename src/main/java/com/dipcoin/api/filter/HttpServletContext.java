package com.dipcoin.api.filter;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@JsonFilter("httpServletContextFilter")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpServletContext {

  private static final Logger LOG = LogManager.getLogger(HttpServletContext.class);
  private static final ObjectMapper objectMappper = new ObjectMapper();

  public static final String HEADER_FF = "X-Feature-Flags";
  public static final String HEADER_TRACEID = "X-TraceId";

  public static class ClientFeatureFlags {

    private static String DELIMITER_COMMA = ",";
    private static String DELIMITER_EQUALS = "=";

    private enum Flags {
      notificationEnabled, smsEnabled;
    }

    private Map<String, String> lookup = new HashMap<>();

    private ClientFeatureFlags() {
    }

    public static ClientFeatureFlags instance() {
      return new ClientFeatureFlags();
    }

    public ClientFeatureFlags(String ff) {

      if (!StringUtils.isEmpty(ff)) {
        LOG.debug(HEADER_FF + ": " + ff);

        String[] flags = ff.split(DELIMITER_COMMA);
        for (String flag : flags) {
          String[] kv = flag.split(DELIMITER_EQUALS);
          if (kv.length == 2) {
            lookup.put(kv[0].toLowerCase(), kv[1]);
          } else {
            lookup.put(kv[0].toLowerCase(), null);
          }
        }
      }
    }

    public boolean smsEnabled() {

      HttpServletContext httpServletContext = new HttpServletContext();
      if (httpServletContext.isProdEnvironment()) {
        return true;
      }
      String key = Flags.smsEnabled.name().toLowerCase();
      if (lookup.get(key) != null) {
        return APIUtils.isBoolean(lookup.get(key));
      } else {
        return true;
      }

    }

    public boolean notificationEnabled() {
      String key = Flags.notificationEnabled.name().toLowerCase();
      return APIUtils.isBoolean(lookup.get(key));
    }

    public void setNotificationEnabled(boolean flag) {
      String key = Flags.notificationEnabled.name().toLowerCase();
      lookup.put(key, String.valueOf(flag));
    }

    @Override
    public String toString() {
      try {
        return objectMappper.writeValueAsString(lookup);
      } catch (JsonProcessingException e) {
        LOG.error(LogFormatter.instance().message("Exception caught").format(), e);
      }

      return null;
    }
  }

  private ClientFeatureFlags clientFeatureFlags;
  @JsonIgnore
  private Device device;
  private String traceId;
  private String originIp;
  private String clientTransactionId;
  private MultiValuedMap<String, String> headers = new ArrayListValuedHashMap<>();
  private Map<String, Cookie> cookies = new HashMap<>();

  private User user;
  private Merchant merchant;
  private Bank bank;

  @Autowired
  private HttpServletRequest servletRequest;

  @Autowired
  private HttpServletResponse servletResponse;

  @Autowired
  private Environment environment;

  private boolean devEnvironment;
  private boolean stageEnvironment;
  private boolean uatEnvironment;
  private boolean prodEnvironment;
  private UriInfo uriInfo;
  private URI uri;

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  @PostConstruct
  public void setup() {

    clientFeatureFlags = new ClientFeatureFlags(servletRequest.getHeader(HEADER_FF));

    device = DeviceUtils.getCurrentDevice(servletRequest);

    traceId = Optional.ofNullable(servletRequest.getHeader(HEADER_TRACEID))
        .filter(StringUtils::isNotEmpty).orElse(APIUtils.generateTraceId());

    if (environment != null) {
      Set<String> profiles = new HashSet<>(Arrays.asList(
          ArrayUtils.isNotEmpty(environment.getActiveProfiles()) ? environment.getActiveProfiles()
              : environment.getDefaultProfiles()));

      if (CollectionUtils.isEmpty(profiles)) {
        devEnvironment = true;
      } else {
        if (profiles.contains("prod")) {
          prodEnvironment = true;
        } else if (profiles.contains("uat")) {
          uatEnvironment = true;
        } else if (profiles.contains("stage")) {
          stageEnvironment = true;
        } else {
          devEnvironment = true;
        }
      }
    }
    try {
      this.uri = new URI(servletRequest.getRequestURI());
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // headers
    /*while (servletRequest.getHeaderNames().hasMoreElements()) {
      String name = servletRequest.getHeaderNames().nextElement();
      while (servletRequest.getHeaders(name).hasMoreElements()) {
        String value = servletRequest.getHeaders(name).nextElement();
        headers.put(name, value);
      }
    }*/

    // cookies
    if (ArrayUtils.isNotEmpty(servletRequest.getCookies())) {
      for (Cookie c : servletRequest.getCookies()) {
        cookies.put(c.getName(), c);
      }
    }
  }

  public ClientFeatureFlags getClientFeatureFlags() {
    return clientFeatureFlags;
  }

  public void setClientFeatureFlags(ClientFeatureFlags clientFeatureFlags) {
    this.clientFeatureFlags = clientFeatureFlags;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getClientTransactionId() {
    
    if (StringUtils.isNotBlank(clientTransactionId)) {
      return clientTransactionId;
    }
    return servletRequest.getParameter(APIConstants.CLIENT_TRANSACTION_ID);
    
  }

  public void setClientTransactionId(String clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
  }

  public String getOriginIp() {

    if (StringUtils.isNotBlank(originIp)) {
      return originIp;
    } else if (StringUtils.isNotBlank(getxForwardedFor())) {

      String[] ips = getxForwardedFor().split(",");
      originIp = ips[ips.length - 1].trim();
    } else {
      originIp = servletRequest.getRemoteAddr();
    }

    return originIp;
  }

  public void setOriginIp(String originIp) {
    this.originIp = originIp;
  }

  public String getOriginProto() {
    return Optional.ofNullable(getxForwardedProto()).orElse(servletRequest.getScheme());
  }

  public int getOriginPort() {
    return Optional.ofNullable(getxForwardedPort()).map(Integer::valueOf)
        .orElse(servletRequest.getRemotePort());
  }

  public UriInfo getUriInfo() {
    return uriInfo;
  }

  public void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  public HttpServletRequest getServletRequest() {
    return servletRequest;
  }

  public HttpServletResponse getServletResponse() {
    return servletResponse;
  }

  public Device getDevice() {
    return device;
  }

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  public String getHost() {
    return servletRequest.getHeader(HttpHeaders.HOST);
  }


  public String getxForwardedProto() {
    return servletRequest.getHeader(HttpHeaders.X_FORWARDED_PROTO);
  }

  public String getxForwardedPort() {
    return servletRequest.getHeader(APIConstants.X_FWD_PORT);
  }

  public String getxForwardedFor() {
    return servletRequest.getHeader(HttpHeaders.X_FORWARDED_FOR);
  }


  public String getxDeviceId() {
    return servletRequest.getHeader(APIConstants.X_DEVICE_ID);
  }

  public MultiValuedMap<String, String> getRequestHeaders() {
    return headers;
  }

  public Map<String, Cookie> getRequestCookies() {
    return cookies;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public Bank getBank() {
    return bank;
  }

  public void setBank(Bank bank) {
    this.bank = bank;
  }

  public boolean isDisableHeader() {
    return Optional.ofNullable(servletRequest.getParameter("disableHeader")).map(Boolean::valueOf)
        .orElse(false);
  }

  public String getUserAgent() {
    return servletRequest.getHeader(APIConstants.USER_AGENT);
  }

  public boolean isDevEnvironment() {
    return devEnvironment;
  }

  public boolean isStageEnvironment() {
    return stageEnvironment;
  }

  public boolean isUatEnvironment() {
    return uatEnvironment;
  }

  public boolean isProdEnvironment() {
    return prodEnvironment;
  }

  public static HttpServletContext instance() {
    return new HttpServletContext();
  }

  public void setServletRequest(HttpServletRequest servletRequest) {
    this.servletRequest = servletRequest;
  }

  @PostConstruct
  public void construct() throws Exception {
    LOG.trace("Constructing " + this.getClass().getSimpleName() + " ...");
  }

  @PreDestroy
  public void cleanUp() throws Exception {
    LOG.trace("Cleaning up " + this.getClass().getSimpleName() + " ...");
  }
}