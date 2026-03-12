package com.dipcoin.api.filter;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.response.CacheControlInterceptor;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.commons.LogFormatter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Slf4j
@Component
public class ResponseHeadersInterceptor implements HandlerInterceptor {

	private static final Logger log = LogManager.getLogger(ResponseHeadersInterceptor.class);
  @Autowired
  private HttpServletContext httpServletContext;

  @Autowired
  @Qualifier("com.dipcoin.api.filter.response.CacheControlInterceptor")
  private CacheControlInterceptor cacheControlInterceptor;




  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler,
      Exception ex) throws Exception {

    try {
      String path = request.getRequestURI();
      String method = request.getMethod();

      if (HttpMethod.OPTIONS.equalsIgnoreCase(method)) {
        return;
      }

      cacheControlInterceptor.intercept(request, response);

//      if (path.toLowerCase().contains("swagger")) {
//        swaggerInterceptor.intercept(request, response);
//
//        return;
//      }

//      jsonToXMLInterceptor.intercept(request, response);

      if (!APIConstants.HEALTHCHECK_API.equals(path) || !APIConstants.HEAPCHECK_API.equals(path) ) {
        // log outgoing response
        log.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Method", method)
            .data("Path", path).data("Status", response.getStatus()).format());

        log.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("=============== Request END ===============").format());
      }

    } catch (Exception e) {
      log.error(
          LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught")
              .format(),
          e);

      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write(APIResponse.error(HeaderCode.INTERNAL_ERROR).toString());
    }
  }
}
