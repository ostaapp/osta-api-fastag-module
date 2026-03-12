package com.dipcoin.api.filter;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.HeaderCode;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dipcoin.api.filter.request.AuthorizationInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.commons.LogFormatter;

import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component("com.dipcoin.api.filter.RequestHandlerInterceptor")
public class RequestHandlerInterceptor implements HandlerInterceptor {

	private static final Logger log = LogManager.getLogger(RequestHandlerInterceptor.class);
  @Autowired
  private HttpServletContext httpServletContext;

//  @Autowired
//  @Qualifier("com.dipcoin.api.filter.request.BaseInterceptor")
//  private BaseInterceptor baseInterceptor;
//
//  @Autowired
//  @Qualifier("com.dipcoin.api.filter.request.APIControlInterceptor")
//  private APIControlInterceptor apiControlInterceptor;

//  @Autowired
//  @Qualifier("com.dipcoin.api.filter.request.OAuth2Interceptor")
//  private OAuth2Interceptor oAuth2Interceptor;

//  @Autowired
//  @Qualifier("com.dipcoin.api.filter.request.PartnerAuthorizationInterceptor")
//  private PartnerAuthorizationInterceptor partnerAuthorizationInterceptor;

  @Autowired
  @Qualifier("com.dipcoin.api.filter.request.AuthorizationInterceptor")
  private AuthorizationInterceptor authorizationInterceptor;

//  @Autowired
//  @Qualifier("com.dipcoin.api.filter.request.SwaggerInterceptor")
//  private SwaggerInterceptor swaggerInterceptor;

  private static void errorResponse(ResponseEntity<String> error, HttpServletResponse response)
      throws IOException {
    response.setStatus(error.getStatusCodeValue());
    if (error.getBody() != null) {
      response.getWriter().write(new ObjectMapper().writeValueAsString(error.getBody()));
    }
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    try {
      String path = request.getRequestURI();
      String method = request.getMethod();

      if (HttpMethod.OPTIONS.equalsIgnoreCase(method)) {
        return true;
      }

      if (APIConstants.HEALTHCHECK_API.equals(path) || APIConstants.HEAPCHECK_API.equals(path)) {
        return true;
      }

      log.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("=============== Request START ===============").format());
      log.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Path", path)
          .data("Request", request.getRequestURI())
          .data("Headers", httpServletContext.getRequestHeaders())
          .data("Cookies", request.getCookies()).format());

      log.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Environment Profiles")
          .data("Active", httpServletContext.getEnvironment().getActiveProfiles())
          .data("Default", httpServletContext.getEnvironment().getDefaultProfiles()).format());
      String[] profiles =
          ArrayUtils.isNotEmpty(httpServletContext.getEnvironment().getActiveProfiles())
              ? httpServletContext.getEnvironment().getActiveProfiles()
              : httpServletContext.getEnvironment().getDefaultProfiles();

      if (ArrayUtils.isNotEmpty(profiles)
          && new HashSet<String>(Arrays.asList(profiles)).contains("prod") && path
          .startsWith("/test")) {
        response.setStatus(HttpStatus.SC_FORBIDDEN);
        response.getWriter().write(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString());
        return false;
      }
      
      log.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("AFter the Line Commented profiles").format());

      // if in deployment mode skip any further processing
      if (APIConstants.DEPLOYMENT_API.equalsIgnoreCase(path)) {
        boolean isDeployment = isDeploymentInProgress();
        response.setStatus(isDeployment ? HttpStatus.SC_TEMPORARY_REDIRECT : HttpStatus.SC_OK);
        if (!isDeployment) {
          response.getWriter().write(APIResponse.error(HeaderCode.REQUEST_OK).toString());
        }
        return false;
      }

      // run base interceptor
//      {
//        Optional<ResponseEntity<String>> errorT = baseInterceptor.intercept(request);
//        if (errorT.isPresent()) {
//        	  log.debug(LogFormatter.instance(httpServletContext.getTraceId())
//                      .message("Inside 1st interceptor").format());
//          errorResponse(errorT.get(), response);
//          return false;
//        }
//      }
//
//      {
//        Optional<ResponseEntity<String>> errorT = apiControlInterceptor.intercept(request);
//        if (errorT.isPresent()) {
//        	log.debug(LogFormatter.instance(httpServletContext.getTraceId())
//                    .message("Inside 2st interceptor").format());
//          errorResponse(errorT.get(), response);
//          return false;
//        }
//      }
//
//      if (path.toLowerCase().contains("swagger")) {
//        Optional<ResponseEntity<String>> errorT = swaggerInterceptor.intercept(request);
//        if (errorT.isPresent()) {
//        	log.debug(LogFormatter.instance(httpServletContext.getTraceId())
//                    .message("Inside 3rd interceptor").format());
//          errorResponse(errorT.get(), response);
//          return false;
//        }
//      } 
//      else if (path.startsWith(APIConstants.PARTNER)) {
//        Optional<ResponseEntity<String>> errorT = partnerAuthorizationInterceptor
//            .intercept(request);
//        if (errorT.isPresent()) {
//          errorResponse(errorT.get(), response);
//          return false;
//        }
//      } 
      else {
        Optional<ResponseEntity<String>> errorT = authorizationInterceptor.intercept(request);
        if (errorT.isPresent()) {
        	log.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Inside else wala interceptor").format());
          errorResponse(errorT.get(), response);
          return false;
        }
        }
//      }

    } catch (Exception e) {
      log.error(
          LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught")
              .format(),
          e);

      response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      return false;
    }

    return true;
  }

  private boolean isDeploymentInProgress() {
    return new File("/tmp/deployment").exists();
  }
}
