package com.dipcoin.api.filter.response;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIConstants.RequestType;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.UserDBService;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component("com.dipcoin.api.filter.response.CacheControlInterceptor")
public class CacheControlInterceptor implements ResponseInterceptor {

  private static int ttlInSec = 2;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private HttpServletContext httpServletContext;

  public void intercept(HttpServletRequest request, HttpServletResponse response) throws Exception {

    String path = request.getPathInfo();
    String method = request.getMethod();

    CacheControl cc = CacheControl.empty();
    if (RequestType.GET.name().equals(method) && !APIConstants.HEALTHCHECK_API.equals(path)) {
      log.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Request Method: " + method + ", Path: " + path + ", Cache TTL: " + ttlInSec)
          .format());
      cc.sMaxAge(ttlInSec, TimeUnit.SECONDS);
      cc.cachePrivate();
    } else {
      cc.cachePublic();
    }
    response.addHeader(HttpHeaders.CACHE_CONTROL, cc.toString());
  }
}
