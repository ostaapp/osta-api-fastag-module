package com.dipcoin.api.fraudMgmt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.amqp.RabbitMqConfiguration;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.fraudMgmt.ProcessEvent.EventType;
//import com.dipcoin.api.model.AsyncUserEventRequest;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.UserStateService;
import com.dipcoin.db.services.model.User;
import com.dipcoin.db.services.model.UserState;
//import com.dipcoin.db.services.model.UserState;
//import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import groovy.lang.Lazy;

@Component("UserEventResource")
@Transactional(rollbackFor = {Exception.class}, propagation = Propagation.REQUIRES_NEW)
public class UserEventResource {

  private static final Logger LOG = LogManager.getLogger(UserEventResource.class);
  private static ObjectMapper mapper = new ObjectMapper();

  @Autowired
  UserStateService userStateService;

  @Autowired
  RabbitTemplate asyncRequestAmqpTemplate;

  @Autowired
  private EmailUtils emailUtils;

  @Autowired
  private UserDBService userDBService;
  
  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;
  
  public void setHttpServletContext(HttpServletContext httpServletContext) {   
    this.httpServletContext = httpServletContext;
  }

  /*
   * Here the User Events will get added to User State table
   * 
   */
  public void processInQueue(User user, String requestTime, String event, String dcoin)
      throws JsonProcessingException {

    AsyncUserEventRequest<UserEvent> auer =
        getAsyncUserEventRequest(user, requestTime, event, dcoin);
    asyncRequestAmqpTemplate.convertAndSend(RabbitMqConfiguration.AsyncUserEventQueue, auer);

  }

  public boolean applyRule(User customer, User merchant, String dcoin, String requestTime,
      EventType eventType, String eventFailure)
      throws IOException, InterruptedException, ExecutionException {
    
    if(!applicationProperties.isFraudMgmt()) {
      return true;
    }
    
    LOG.debug(
        LogFormatter.instance(httpServletContext.getTraceId()).data("processEvent type", eventType)
            .data("User", customer != null ? customer.getId() : null)
            .data("Merchant", merchant != null ? merchant.getId() : null));

    ProcessEvent processEvent = EventUtils.executeRules(eventType,
        customer != null ? fetchUserState(customer.getId()) : null,
        merchant != null ? fetchUserState(merchant.getId()) : null, dcoin, requestTime,
        httpServletContext.getOriginIp());

    if (!processEvent.isProcess()) {

      if (customer != null)
        processInQueue(customer, requestTime, eventFailure, dcoin);

      if (merchant != null)
        processInQueue(merchant, requestTime, eventFailure, dcoin);

      notifyAdmin(eventType, customer, merchant, processEvent.getErrorMsg());

      /*
       * notifyCustomer(eventType,customer,merchant,
       * "loggIn limit exceeded,plz loggout and login again",requestContext);
       */
      return false;
    }

    return true;
  }
//
//  public void updateUserState(AsyncUserEventRequest<?> request, User user, EventStatus blob,
//      String event) throws JsonParseException, JsonMappingException, IOException {
//
//    UserState userStateExist;
//
//    LOG.info("In UpdateUserState");
//
//    String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
//
//    LOG.debug(LogFormatter.instance(request.getTraceId()).data("RawData", blob)
//        .data("RequestTime", requestTime).format());
//
//    // validate request
//    if (blob == null) {
//      LOG.error(LogFormatter.instance(request.getTraceId()).message("Invalid Request").format());
//      return;
//    }
//    if (user != null) {
//      userStateExist = userStateService.getUserStateById(user.getId());
//    } else {
//      userStateExist = userStateService.getUserStateById(-1);
//    }
//
//    if (userStateExist != null) {
//
//      UserEvent lue =
//          mapper.readValue(userStateExist.getUserState(), new TypeReference<UserEvent>() {});
//
//      lue.getMetadata().setUpdatedAt(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
//
//      if (lue.getState().containsKey(event.toString())) {
//
//        List<String> ipAddress = new ArrayList<String>();
//        List<String> rt = new ArrayList<String>();
//        ipAddress.addAll(lue.getState().get(event).getIpaddress());
//
//        if (!ipAddress.contains(blob.getIpaddress().get(0))) {
//          ipAddress.add(blob.getIpaddress().get(0));
//        }
//
//        rt.addAll(lue.getState().get(event).getRequestTime());
//        rt.add(blob.getRequestTime().get(0));
//
//        EventStatus eventStatus = lue.getState().get(event);
//        eventStatus.setCount(eventStatus.getCount() + 1);
//        eventStatus.setIpaddress(ipAddress);
//        eventStatus.setRequestTime(rt);
//
//        if (event.equalsIgnoreCase(EventUtils.userEvents.get("2"))) {
//          HashMap<String, String> hm_ostacreated = new HashMap<String, String>();
//
//          if (lue.getState().get(event).getOstaProcessed() != null)
//            hm_ostacreated.putAll(lue.getState().get(event).getOstaProcessed());
//
//          hm_ostacreated.put(request.getOstaId(), blob.getIpaddress().get(0));
//          eventStatus.setOstaProcessed(hm_ostacreated);
//        }
//
//        if (event.equalsIgnoreCase(EventUtils.userEvents.get("3"))) {
//          HashMap<String, String> hm_ostaCreationFailure = new HashMap<String, String>();
//
//          if (lue.getState().get(event).getOstaProcessed() != null)
//            hm_ostaCreationFailure.putAll(lue.getState().get(event).getOstaProcessed());
//
//          hm_ostaCreationFailure.put(request.getOstaId(), blob.getIpaddress().get(0));
//          eventStatus.setOstaProcessed(hm_ostaCreationFailure);
//        }
//
//        if (event.equalsIgnoreCase(EventUtils.userEvents.get("4"))) {
//          HashMap<String, String> hm_ostaProcessed = new HashMap<String, String>();
//
//          if (lue.getState().get(event).getOstaProcessed() != null)
//            hm_ostaProcessed.putAll(lue.getState().get(event).getOstaProcessed());
//
//          hm_ostaProcessed.put(request.getOstaId(), blob.getIpaddress().get(0));
//          eventStatus.setOstaProcessed(hm_ostaProcessed);
//        }
//
//        if (event.equalsIgnoreCase(EventUtils.userEvents.get("5"))) {
//          HashMap<String, String> hm_ostaInProcess = new HashMap<String, String>();
//          if (lue.getState().get(event).getOstaProcessed() != null)
//            hm_ostaInProcess.putAll(lue.getState().get(event).getOstaProcessed());
//
//          hm_ostaInProcess.put(request.getOstaId(), blob.getIpaddress().get(0));
//          eventStatus.setOstaProcessed(hm_ostaInProcess);
//        }
//
//        if (event.equalsIgnoreCase(EventUtils.userEvents.get("6"))) {
//
//          HashMap<String, String> hm_ostaInProcess = new HashMap<String, String>();
//          if (lue.getState().get(event).getOstaProcessed() != null)
//            hm_ostaInProcess.putAll(lue.getState().get(event).getOstaProcessed());
//
//          hm_ostaInProcess.put(request.getOstaId(), blob.getIpaddress().get(0));
//          eventStatus.setOstaProcessed(hm_ostaInProcess);
//        }
//
//        lue.getState().replace(event, eventStatus);
//        userStateExist.setUserState(mapper.writeValueAsString(lue));
//
//      } else {
//
//        lue.getState().put(event, blob);
//        userStateExist.setUserState(mapper.writeValueAsString(lue));
//
//      }
//
//      userStateService.updateUserState(userStateExist);
//
//    } else {
//      HashMap<String, EventStatus> hm = new HashMap<String, EventStatus>();
//      hm.put(event, blob);
//      UserEvent userEvent = new UserEvent();
//      userEvent.setState(hm);
//      Metadata metadata = new Metadata();
//
//
//      if (user != null) {
//        metadata.setRole(user.getRole());
//        metadata.setUpdatedAt(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
//        userEvent.setMetadata(metadata);
//        // userEvent.setRole(user.getRole());
//      } else {
//        metadata.setRole("-1");
//        metadata.setUpdatedAt(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
//        userEvent.setMetadata(metadata);
//        // userEvent.setRole("-1");
//      }
//
//      UserState userState = new UserState();
//      if (user != null) {
//        userState.setUserId(user.getId());
//        userState.setRole(user.getRole());
//      } else {
//        userState.setUserId(-1);
//        userState.setRole("-1");
//      }
//
//      userState.setUserState(mapper.writeValueAsString(userEvent));
//      userStateService.addUserState(userState);
//    }
//    LOG.info("In UpdateUserState");
//
//  }
//
//
  public UserState fetchUserState(int id) {


    UserState userStateExist = userStateService.getUserStateById(id);

    return userStateExist;

  }
//
//  /*
//   * public void notifyCustomer(EventType eventType,User customer , User merchant ,String
//   * msg,RequestContext requestContext) {
//   * 
//   * // send SMS if (!smsClient.sendSms(customer.getPhone(), Templates.VelocityCheck.
//   * format("loggIn limit exceeded,plz loggout and login again"),
//   * requestContext.getClientFeatureFlags().smsEnabled())) {
//   * LOG.debug(LogFormatter.instance(requestContext.getTraceId()). message("Failed to send SMS")
//   * .data("phone", customer.getPhone()) .data("template", Templates.VelocityCheck.
//   * format("loggIn limit exceeded,plz loggout and login again")) .format()); }
//   * 
//   * }
//   */
//
  public void notifyAdmin(EventType eventType, User customer, User merchant, String msg)
      throws InterruptedException, ExecutionException {

    List<String> roles = new ArrayList<String>();
    List<User> users = new ArrayList<>();
    roles.add("DS");
    users = userDBService.asyncGetUsersByRoles(roles).get();

    if (emailUtils.alertToAdmin(customer, merchant, eventType, "ankita.neosoft@gmail.com", msg)) {
      LOG.error("Failed to send email to admin " + "ankita.neosoft@gmail.com");
    }

  }
//  
  public AsyncUserEventRequest<UserEvent> getAsyncUserEventRequest(User user, String requestTime,
      String event, String ostaId) throws JsonProcessingException {

    List<String> ipAddress = new ArrayList<String>();
    List<String> rt = new ArrayList<String>();

    ipAddress.add(httpServletContext.getOriginIp());
    rt.add(requestTime);
    EventStatus eventStatus = new EventStatus();
    eventStatus.setCount(1);
    eventStatus.setIpaddress(ipAddress);
    eventStatus.setRequestTime(rt);

    if (EventUtils.userEvents.get(event).equalsIgnoreCase(EventUtils.userEvents.get("2"))) {
      HashMap<String, String> hm_ostaProcessed = new HashMap<String, String>();
      hm_ostaProcessed.put(ostaId, httpServletContext.getOriginIp());
      eventStatus.setOstaProcessed(hm_ostaProcessed);
    }

    if (EventUtils.userEvents.get(event).equalsIgnoreCase(EventUtils.userEvents.get("3"))) {
      HashMap<String, String> hm_ostaProcessed = new HashMap<String, String>();
      hm_ostaProcessed.put(ostaId, httpServletContext.getOriginIp());
      eventStatus.setOstaProcessed(hm_ostaProcessed);
    }


    if (EventUtils.userEvents.get(event).equalsIgnoreCase(EventUtils.userEvents.get("4"))) {
      HashMap<String, String> hm_ostaProcessed = new HashMap<String, String>();
      hm_ostaProcessed.put(ostaId, httpServletContext.getOriginIp());
      eventStatus.setOstaProcessed(hm_ostaProcessed);
    }

    if (EventUtils.userEvents.get(event).equalsIgnoreCase(EventUtils.userEvents.get("5"))) {
      HashMap<String, String> hm_ostaInProcess = new HashMap<String, String>();
      hm_ostaInProcess.put(ostaId, httpServletContext.getOriginIp());
      eventStatus.setOstaProcessed(hm_ostaInProcess);
    }

    if (EventUtils.userEvents.get(event).equalsIgnoreCase(EventUtils.userEvents.get("6"))) {

      HashMap<String, String> hm_ostaInProcess = new HashMap<String, String>();
      hm_ostaInProcess.put(ostaId, httpServletContext.getOriginIp());
      eventStatus.setOstaProcessed(hm_ostaInProcess);
    }


    AsyncUserEventRequest<UserEvent> auer = new AsyncUserEventRequest<>();
    auer.setIpAddress(httpServletContext.getOriginIp());
    auer.setTraceId(httpServletContext.getTraceId());
    if (user != null) {
      auer.setUserId(user.getId());
    } else {
      auer.setUserId(-1);
    }
    auer.setStatus("0");
    auer.setUserEvent(EventUtils.userEvents.get(event));
    auer.setRequestTime(requestTime);
    auer.setRawRequest(eventStatus);
    auer.setOstaId(ostaId);
    return auer;
  }


}
