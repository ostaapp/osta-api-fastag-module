package com.dipcoin.api.fraudMgmt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.fraudMgmt.ProcessEvent.EventType;
import com.dipcoin.db.services.model.User;
import com.dipcoin.db.services.model.UserState;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
//import groovy.lang.Lazy;



public class EventUtils {

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;


  public static final String loginSuccess = "0";

  public static final String loginFailure = "1";

  public static final String dipcoinCreationSuccess = "2";

  public static final String dipcoinCreationFailure = "3";

  public static final String dipcoinUsageSuccess = "4";

  public static final String dipcoinUsageFailure = "5";

  public static final String dipcoinUsageInprocess = "6";

  static Map<String, String> userEvents = new HashMap<String, String>();

  static {
    userEvents.put("0", "loginSuccess");
    userEvents.put("1", "loginFailure");
    userEvents.put("2", "dipcoinCreationSuccess");
    userEvents.put("3", "dipcoinCreationFailure");
    userEvents.put("4", "dipcoinUsageSuccess");
    userEvents.put("5", "dipcoinUsageFailure");
    userEvents.put("6", "dipcoinUsageInprocess");
  }

  public static ProcessEvent executeRules(EventType type, UserState customer, UserState merchant,
      String dcoin, String requestTime, String originIp) throws JsonParseException, IOException {

    RuleEngine ruleEngine = new RuleEngine();
    ProcessEvent processEvent = new ProcessEvent();
    processEvent.setType(type);
    processEvent.setMerchant(merchant);
    processEvent.setCustomer(customer);
    processEvent.setDcoin(dcoin);
    processEvent.setRequestTime(requestTime);
    processEvent.setOriginIp(originIp);

    processEvent = ruleEngine.rule(processEvent);

    return processEvent;


  }

  public static boolean frequencyCheckWithRequestTime(EventStatus eventStatus, String requestTime,
      int timespan, int count) {

    int inticount = 0;
    boolean processRequest = false;

    if (eventStatus.getRequestTime() != null || eventStatus.getRequestTime().size() != 0) {
      for (int i = eventStatus.getRequestTime().size() - 1; i >= 0; i--) {
        // processRequest = false;

        String time = eventStatus.getRequestTime().get(i);

        int difference = (int) (((Long.parseLong(requestTime) - Long.parseLong(time)) / 1000) / 60);

        if (difference < timespan) {
          inticount++;
        }

        if (inticount >= count) {
          processRequest = true;
          break;
        }

        if (difference > timespan) {
          // processRequest = false;
          break;
        }
      }

    }

    return processRequest;

  }

  public static boolean checkIfDipcoinAlreadyExist(EventStatus eventStatus, String dcoin) {

    if (eventStatus.getOstaProcessed().containsKey(dcoin)) {
      return true;
    }
    return false;
  }

  public static Boolean checkIfTwoOstaAtDifferentMerchantsAtSameTime() {

    return null;

  }

}
