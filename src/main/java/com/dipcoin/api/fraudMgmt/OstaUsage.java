package com.dipcoin.api.fraudMgmt;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.dipcoin.commons.LogFormatter;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OstaUsage implements RuleI<ProcessEvent, ProcessEvent> {

    private static final Logger LOG = LogManager.getLogger(OstaUsage.class);
    private static ObjectMapper mapper = new ObjectMapper();
    private static int timeSpan = 2;
    private static int count= 1;

    @Override
    public boolean matches(ProcessEvent input) {

        LOG.info("In OstaUsage matches method");
        return input.getType().equals(ProcessEvent.EventType.OstaUsage);
    }

    @Override
    public ProcessEvent process(ProcessEvent input) throws JsonParseException, JsonMappingException, IOException {

        if (input.getCustomer() != null) {

            LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            LOG.info("In OstaUsage process method");
            LOG.debug(LogFormatter.instance().data("Customer", input.getCustomer().getId())
                    .data("role", input.getCustomer().getRole()).format());
            LOG.debug(LogFormatter.instance().data("dcoin", input.getDcoin()).format());
            LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            // rules on user data

            UserEvent lue = mapper.readValue(input.getCustomer().getUserState(), new TypeReference<UserEvent>() {
            });

            // take out dipcoinUsageSuccess data userEventProperties
            EventStatus dipcoinUsageSuccess = lue.getState().get(EventUtils.userEvents.get("4")); // userEventProperties.getEvents().get("dipcoinUsageSuccess")

            // take out dipcounUsageInprocess data
            EventStatus dipcoinUsageInprocess = lue.getState().get(EventUtils.userEvents.get("6"));

            // take out dipcoinUsageFailure data
            EventStatus dipcoinUsageFailure = lue.getState().get(EventUtils.userEvents.get("5"));

            input.setProcess(false);


            /*
             * Rule1 check in dipcoinUsageSuccess first and then check in
             * dipcoinUsageInprocess check if the comming osta id is in already processed or
             * is in inprocess map if its their in processed map dont check in inprocess map
             * if its not their in processed map check in inprocess map if its their in
             * ,processed map return false if its their in inprocess map return false
             */
                if(dipcoinUsageSuccess != null) {
                if (EventUtils.checkIfDipcoinAlreadyExist(dipcoinUsageSuccess, input.getDcoin())) {
                    input.setErrorMsg("User (" +input.getCustomer().getUserId()+ ") used already processed osta"+input.getDcoin() );
                    return input;
                }
                }
                if(dipcoinUsageInprocess != null) {

                 if (EventUtils.checkIfDipcoinAlreadyExist(dipcoinUsageInprocess,input.getDcoin()))
                 {
                     input.setErrorMsg("User (" +input.getCustomer().getUserId()+ ") is processing same osta which is already in process "+input.getDcoin() );
                     return input; 

                 }
                }




            /*
             * Rule2 dipcoinUsageSuccess,dipcoinUsageFailure check the diff between the
             * current request time and existing request times diffrence between the
             * existing request time also can be checked but in offine ,in online dont think
             * to do this if its less than 5 min increase the count if the count increases
             * ten then return false
             * 
             */
            if(dipcoinUsageSuccess != null) {

                if (EventUtils.frequencyCheckWithRequestTime(dipcoinUsageSuccess, input.getRequestTime(),timeSpan,count)) {
                     input.setErrorMsg("User (" +input.getCustomer().getUserId()+ ") is processing multiple osta within very short period of time");
                    return input;
                }   
            }


            if(dipcoinUsageFailure != null) {

                if (EventUtils.frequencyCheckWithRequestTime(dipcoinUsageFailure, input.getRequestTime(),timeSpan,count)) {
                     input.setErrorMsg("User (" +input.getCustomer().getUserId()+ ") is processing multiple osta within very short period of time");
                    return input;
                }

            }

        }

        if (input.getMerchant() != null) {

            LOG.debug(LogFormatter.instance().data("Merchant", input.getMerchant().getId())
                    .data("role", input.getMerchant().getRole()).format());
            LOG.debug(LogFormatter.instance().data("dcoin", input.getDcoin()).format());

            // rules on user data

            UserEvent lue = mapper.readValue(input.getMerchant().getUserState(), new TypeReference<UserEvent>() {
            });

            // take out dipcoinUsageSuccess data
            EventStatus dipcoinUsageSuccess = lue.getState().get(EventUtils.userEvents.get("4"));

            // take out dipcoinUsageFailure data
            EventStatus dipcoinUsageFailure = lue.getState().get(EventUtils.userEvents.get("5"));

            input.setProcess(false);

            /*
             * Rule1 dipcoinUsageSuccess check if osta already there in processed ostas
             * 
             */
            if(dipcoinUsageSuccess != null) {
                if (EventUtils.checkIfDipcoinAlreadyExist(dipcoinUsageSuccess, input.getDcoin())) {
                    input.setErrorMsg("Merchant (" +input.getMerchant().getUserId()+ ") is been detected with same osta getting processed again");
                    return input;
                }
            }

            /*
             * Rule1 dipcoinUsageSuccess,dipcoinUsageFailure check the diff between the
             * current request time and existing request times diffrence between the
             * existing request time also can be checked but in offine ,in online dont think
             * to do this if its less than 5 min increase the count if the count increases
             * ten then return false
             * 
             */
            if(dipcoinUsageSuccess != null) {
                if (EventUtils.frequencyCheckWithRequestTime(dipcoinUsageSuccess, input.getRequestTime(),timeSpan,count)) {
                    input.setErrorMsg("Merchant (" +input.getMerchant().getUserId()+ ") processing multiple ostas within a short period of time");
                    return input;
                }
            }

            if(dipcoinUsageFailure != null) {
                if (EventUtils.frequencyCheckWithRequestTime(dipcoinUsageFailure, input.getRequestTime(),timeSpan,count)) {
                    input.setErrorMsg("Merchant (" +input.getMerchant().getUserId()+ ") processing multiple ostas within a short period of time");
                    return input;
                }
            }

        }
        input.setProcess(true);
        return input;
    }

}