package com.dipcoin.api.fraudMgmt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonParseException;

@Component
@PropertySource(value = {
"classpath:dipcoin-api-system-fraud-mgmt.properties" }, ignoreResourceNotFound = true)
@Configuration
@ConfigurationProperties("com.dipcoin.api")
@ComponentScan("com.dipcoin")
public class RuleEngine {
  
  private static final Logger LOG = LogManager.getLogger(OstaUsage.class);
  
  private static List<RuleI<ProcessEvent, ProcessEvent>> rules = new ArrayList<>();
  
  public Map<String, String> fraudMgmt = new HashMap<>();

  public void init() {
    
    RuleEngine ruleEngine = new RuleEngine();
    ruleEngine.registerRule(new OstaUsage(), fraudMgmt)
        .registerRule(new LoginCheck(), fraudMgmt)
        .registerRule(new OstaCreation(), fraudMgmt);
    
    LOG.info("RuleEngine"+fraudMgmt);
  }

  public Map<String, String> getFraudMgmt() {
    return fraudMgmt;
  }

  public void setFraudMgmt(Map<String, String> fraudMgmt) {
    this.fraudMgmt = fraudMgmt;
  }

  public RuleEngine() {

  }

  public ProcessEvent rule(ProcessEvent event) throws JsonParseException, IOException {
    return rules.stream().filter(rule -> rule.matches(event)).map(rule -> {
      try {
        return rule.process(event);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return event;
    }).findFirst().orElse(event);
  }


  public RuleEngine registerRule(RuleI<ProcessEvent, ProcessEvent> rule, Map<String, String> rb) {

    /*
     * if(propertyname.containsKey(rule.getClass().getSimpleName())) {
     * if(propertyname.get(rule.getClass().getSimpleName()).equalsIgnoreCase("yes")) {
     * rules.add(rule); } }
     */

    LOG.info("rule:-" + rule.getClass().getName());
    Set<String> keys = rb.keySet();
    
    for (String string : keys) {
      String key = string;
      String value = rb.get(string);
      if (rule.getClass().getSimpleName().equalsIgnoreCase(key)) {
        System.out.println(key + ": " + value);
        if (value.equalsIgnoreCase("yes")) {
          rules.add(rule);
        }
        break;
      }
    }

    return this;
  }

}
