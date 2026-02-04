package com.dipcoin.api.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;

@Component
public class DipcoinDispatcherServlet extends DispatcherServlet {

  @Override
  protected final void doService(HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    if (NumberUtils.INTEGER_ZERO == request.getPathInfo().split("/").length
        || "/favicon.ico".equalsIgnoreCase(request.getPathInfo())) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    super.doService(request, response);

  }

}
