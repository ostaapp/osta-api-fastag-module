package com.dipcoin.api.filter.response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ResponseInterceptor {

  void intercept(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
