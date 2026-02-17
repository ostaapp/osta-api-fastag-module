package com.dipcoin.api.fraudMgmt;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.commons.LogFormatter;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoginCheck implements RuleI<ProcessEvent, ProcessEvent> {

  private static final Logger LOG = LogManager.getLogger(LoginCheck.class);
  private static ObjectMapper mapper = new ObjectMapper();
  private static int timeSpan = 5;
  private static int count = 5;

  @Override
  public boolean matches(ProcessEvent input) {
    LOG.info("In LoginCheck matches method");
    return input.getType().equals(ProcessEvent.EventType.LoginCheck);
  }

  @Override
  public ProcessEvent process(ProcessEvent input)
      throws JsonParseException, JsonMappingException, IOException {



    if (input.getCustomer() != null) {

      LOG.info("In LoginCheck process method");
      LOG.debug(LogFormatter.instance().data("Customer", input.getCustomer().getId())
          .data("role", input.getCustomer().getRole()).format());
      // rules on user data

      UserEvent lue =
          mapper.readValue(input.getCustomer().getUserState(), new TypeReference<UserEvent>() {});

      // take out dipcoinUsageSuccess data userEventProperties
      EventStatus loginSuccess = lue.getState().get(EventUtils.userEvents.get("0"));

      // take out dipcounUsageInprocess data
      EventStatus loginFailuer = lue.getState().get(EventUtils.userEvents.get("1"));

      input.setProcess(false);

      if (loginSuccess != null) {
        if (EventUtils.frequencyCheckWithRequestTime(loginSuccess, input.getRequestTime(), timeSpan,
            count)) {
          input.setErrorMsg("User (" + input.getCustomer().getUserId()
              + ") has been detected with frequent login attempts");
          return input;
        }
      }

      if (loginFailuer != null) {
        if (EventUtils.frequencyCheckWithRequestTime(loginFailuer, input.getRequestTime(), timeSpan,
            count)) {
          input.setErrorMsg("User (" + input.getCustomer().getUserId()
              + ") has been detected with frequent login attempts with invalid credentials");
          return input;
        }
      }
    }

    input.setProcess(true);
    return input;
  }

}
