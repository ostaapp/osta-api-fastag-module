package com.dipcoin.metrics;

import com.dipcoin.api.config.ApplicationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component("com.dipcoin.metrics.UserMetricRegistry")
public class UserMetricRegistry {

  private static final Logger LOG = LogManager.getLogger(UserMetricRegistry.class);

  public enum UserType {
    MERCHANT("M"), BANK("B"), CUSTOMER("C");

    private String type;

    UserType(String type) {
      this.type = type;
    }

    public String type() {
      return type;
    }
  }

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private MeterRegistry meterRegistry;

  private static String TYPE = "type";

  private static Tag tag(String tag, String value) {
    return Tag.of(tag, value);
  }

  /*
   * metrics
   */

  public Counter userCreatedCounter(UserType userType) {
    return Counter.builder("api_user_created").description("(Number of new users)")
        .tags(Arrays.asList(tag(TYPE, userType.type()))).register(meterRegistry);
  }

  public Counter userLoggedInCounter(UserType userType) {
    return Counter.builder("api_user_logged_in").description("(Number of logged in users)")
        .tags(Arrays.asList(tag(TYPE, userType.type()))).register(meterRegistry);
  }

  public Counter userLoggedOutCounter(UserType userType) {
    return Counter.builder("api_user_logged_out").description("(Number of logged out users)")
        .tags(Arrays.asList(tag(TYPE, userType.type()))).register(meterRegistry);
  }
  
  public Counter userForgotPasswordCounter() {
    return Counter.builder("api_user_forgot_password")
        .description("(Number of times user attempts to do forgot password)")
        .register(meterRegistry);
  }

  public Counter resetPinCounter() {
    return Counter.builder("api_user_reset_pin")
        .description("(Number of times a particular user changes its pin)").register(meterRegistry);
  }

  @PostConstruct
  public void construct() throws Exception {
    LOG.info("Constructing " + this.getClass().getSimpleName() + " ...");

  }

  @PreDestroy
  public void cleanUp() throws Exception {
    LOG.info("Cleaning up " + this.getClass().getSimpleName() + " ...");
  }

}
