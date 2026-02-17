package com.dipcoin.api.fraudMgmt;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface RuleI<I, O> {

  boolean matches(I input);

  O process(I input) throws JsonParseException, JsonMappingException, IOException;

}
