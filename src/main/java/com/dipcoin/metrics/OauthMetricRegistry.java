package com.dipcoin.metrics;

import com.dipcoin.api.config.ApplicationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component("com.dipcoin.metrics.OauthMetricRegistry")
@Accessors(fluent = true)
public class OauthMetricRegistry {

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private MeterRegistry meterRegistry;

  @Getter
  private DistributionSummary dipcoinValueDistribution;

  @PostConstruct
  public void init() {
  }

  @PreDestroy
  public void cleanup() {
  }

  public Counter totalOauthUsedApi() {
    return Counter.builder("api_used_for_oauth_transactions")
        .description("(Number of api's used for oauth related transactions)")
        .register(meterRegistry);
  }

  public Counter dipcoinUsageUsingOauth() {
    return Counter.builder("api_dipcoin_used_using_oauth")
        .description("(Number of dipcoin used via oauth transactions)")
        .register(meterRegistry);
  }
}
