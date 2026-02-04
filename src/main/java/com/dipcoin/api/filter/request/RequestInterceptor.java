package com.dipcoin.api.filter.request;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface RequestInterceptor {

  Optional<ResponseEntity<String>> intercept(HttpServletRequest request) throws Exception;
}
