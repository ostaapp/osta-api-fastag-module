package com.dipcoin.api.utils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.Getter;
import lombok.experimental.Accessors;

@Lazy
@Component("com.dipcoin.metrics.TollMetricRegistry")
@Accessors(fluent = true)
public class TollMetricRegistry {

	private static final Logger LOG = LogManager.getLogger(TollMetricRegistry.class);

	@Autowired
	private MeterRegistry meterRegistry;

	public Counter tagApplicationSuccess() {
		return Counter.builder("api_tag_application_success_count").description("Number of success tag applications")
				.register(meterRegistry);
	}

	public Counter tagApplicationFailed() {
		return Counter.builder("api_tag_applications_failed_count").description("Number of failed tag applications")
				.register(meterRegistry);
	}

	  public Counter numberOfTagsApplied() {
		    return Counter.builder("number_of_tags_applied")
		        .description("Number of Tags applied.")
		        .register(meterRegistry);
		  }

	public Counter numberOfTagsAppliedByBank() {
		return Counter.builder("number_of_tags_applied_by_Bank").description("Number of Tags applied by bank.")
				.register(meterRegistry);
	}

	public Counter numberOfTagsApprovedByBank() {
		return Counter.builder("number_of_tags_approved_by_Bank").description("Number of Tags approved by bank.")
				.register(meterRegistry);
	}

	public Counter numberOfTagsActivatedByCustomer() {
		return Counter.builder("number_of_tags_activated_by_Customer")
				.description("Number of Tags activated by Customer.").register(meterRegistry);
	}

	public Counter numberOfTollTransaction() {
		return Counter.builder("number_of_toll_transaction").description("Number Of Toll Transaction.")
				.register(meterRegistry);
	}

	public Counter numberOfFailedTollTransaction() {
		return Counter.builder("number_of_failed_toll_transaction").description("Number Of Failed Toll Transaction.")
				.register(meterRegistry);
	}

	public Counter tollAmountUsed() {
		return Counter.builder("toll_amount_used").description("Toll Amount Used.").register(meterRegistry);
	}

	public Counter tollAmountRecharged() {
		return Counter.builder("toll_amount_recharged").description("Toll Amount Recharged.").register(meterRegistry);
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
