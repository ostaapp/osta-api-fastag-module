package com.dipcoin.metrics;

import com.dipcoin.api.config.ApplicationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component("com.dipcoin.metrics.DipcoinMetricRegistry")
@Accessors(fluent = true)
public class DipcoinMetricRegistry {

  private static final Logger LOG = LogManager.getLogger(DipcoinMetricRegistry.class);

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private MeterRegistry meterRegistry;

  @Getter
  private DistributionSummary dipcoinValueDistribution;


  private static Tag tag(String tag, String value) {
    return Tag.of(tag, value);
  }

  public Counter dipcoinUsage() {
    return Counter.builder("api_dipcoin_used")
        .description("(Number of dipcoin used)")
        .register(meterRegistry);
  }

  public Counter dipcoinCreated() {
    return Counter.builder("api_dipcoin_created_count")
        .description("(Number of dipcoin created)")
        .register(meterRegistry);
  }

  public Counter dipcoinAutoCreated() {
    return Counter.builder("api_dipcoin_auto_created_count")
        .description("(Number of auto gnerated dipcoins)")
        .register(meterRegistry);
  }

  public Counter dipcoinAutoCreatedFailed() {
    return Counter.builder("api_dipcoin_auto_created_count")
        .description("(Number of auto gnerated dipcoins failure)")
        .register(meterRegistry);
  }

  public Counter dipcoinDeleted() {
    return Counter.builder("api_dipcoin_deleted_count")
        .description("(Number of deleted dipcoins")
        .register(meterRegistry);
  }

  public DistributionSummary dipcoinValue() {
    return DistributionSummary.builder("api_dipcoin_value")
        .scale(1000)
        .sla(1, 10, 20)
        .register(meterRegistry);
  }
  
  public Counter dipcoinFailedToCreate() {
    return Counter.builder("api_dipcoin_created_failed_count")
        .description("(Number of dipcoin creation failed)")
        .register(meterRegistry);
  }
  
  public Counter dipcoinFailedToDelete() {
    return Counter.builder("api_dipcoin_deleted_failed_count")
        .description("(Number of dipcoins deletion failed")
        .register(meterRegistry);
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
