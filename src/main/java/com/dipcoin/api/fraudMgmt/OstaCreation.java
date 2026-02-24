package com.dipcoin.api.fraudMgmt;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dipcoin.commons.LogFormatter;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OstaCreation implements RuleI<ProcessEvent, ProcessEvent> {

    private static final Logger LOG = LogManager.getLogger(OstaCreation.class);
    private static ObjectMapper mapper = new ObjectMapper();
    private static int timeSpan = 5;
    private static int count= 5;

    @Override
    public boolean matches(ProcessEvent input) {
        LOG.info("In OstaCreation matches method");
        return input.getType().equals(ProcessEvent.EventType.OstaCreation);
    }

    @Override
    public ProcessEvent process(ProcessEvent input) throws JsonParseException, JsonMappingException, IOException {
        LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        LOG.info("In OstaCreation process method");
        LOG.debug(LogFormatter.instance().data("Customer", input.getCustomer().getId())
                .data("role", input.getCustomer().getRole()).format());
        LOG.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        if (input.getCustomer() != null) {
            // rules on user data

            UserEvent lue = mapper.readValue(input.getCustomer().getUserState(), new TypeReference<UserEvent>() {
            });

            // take out dipcoinUsageSuccess data userEventProperties
            EventStatus ostaCreationSuccess = lue.getState().get(EventUtils.userEvents.get("2"));
            // take out dipcounUsageInprocess data
            EventStatus ostaCreationFailure = lue.getState().get(EventUtils.userEvents.get("3"));

            input.setProcess(false);

            if(ostaCreationSuccess != null) {
                if (EventUtils.frequencyCheckWithRequestTime(ostaCreationSuccess, input.getRequestTime(),timeSpan,count)) {
                    input.setErrorMsg("User (" +input.getCustomer().getUserId()+ ") creating multiple ostas within short period of time");
                    return input;
                }
            }

            if(ostaCreationFailure != null) {
                if (EventUtils.frequencyCheckWithRequestTime(ostaCreationFailure, input.getRequestTime(),timeSpan,count)) {
                    input.setErrorMsg("User (" +input.getCustomer().getUserId()+ ") creating multiple ostas within short period of time");
                    return input;
                }
            }
        }

        input.setProcess(true);
        return input;
    }

}
