package com.dipcoin.tcp;

import com.dipcoin.tcp.request.RequestProcessor;

public interface RequestProcessorFactory {
  public RequestProcessor<?> getProcessor(String name);
}
