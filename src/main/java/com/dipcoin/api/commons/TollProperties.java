package com.dipcoin.api.commons;

import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import com.dipcoin.commons.EncryptedPropertiesUtils;
import lombok.Getter;
import lombok.Setter;

@PropertySources({ @PropertySource(value = {
		"classpath:dipcoin-partner-services-${spring.profiles.active}.properties" }, ignoreResourceNotFound = true),
		@PropertySource(value = { "classpath:dipcoin-partner-services.properties" }, ignoreResourceNotFound = true) })
@Configuration
@ComponentScan("com.dipcoin.partner.toll.commons")
@Getter
@Setter
public class TollProperties {

	private static final Logger LOG = LogManager.getLogger(TollProperties.class);

	@Profile("dev")
	public class DevProfile {
		@Bean("tollEncryptedPropertiesUtils")
		public EncryptedPropertiesUtils amqpEncryptedPropertiesUtils() {
			return new EncryptedPropertiesUtils();
		}
	}

	@Profile("!dev")
	public class ProdProfile {
		@Bean("tollEncryptedPropertiesUtils")
		public EncryptedPropertiesUtils amqpEncryptedPropertiesUtils() {
			return new EncryptedPropertiesUtils(System.getProperty("ASP"));
		}
	}

	@Value("${com.dipcoin.partner.toll.connect.npci}")
	private boolean connectNpci;

	@Value("${com.dipcoin.partner.toll.bankKeystore.path}")
	public String keystorePath;

	@Value("#{tollEncryptedPropertiesUtils.decrypt('${com.dipcoin.partner.toll.bankKeystore.password}')}")
	public String keystorePassword;

	@Value("${com.dipcoin.partner.toll.api.url.syncTimeRequest}")
	public String syncTimeRequestUrl;

	@Value("${com.dipcoin.partner.toll.api.url.getExceptionList}")
	public String getExceptionListUrl;

	@Value("${com.dipcoin.partner.toll.api.url.queryException}")
	public String queryExceptionUrl;

	@Value("${com.dipcoin.partner.toll.api.url.manageTags}")
	public String manageTagsUrl;

	@Value("${com.dipcoin.partner.toll.api.url.asyncManageTags}")
	public String asyncManageTagsUrl;

	@Value("${com.dipcoin.partner.toll.api.url.requestDetail}")
	public String requestDetailUrl;

	@Value("${com.dipcoin.partner.toll.api.url.reqVehicleDetails}")
	public String reqVehicleDetailsUrl;

	@Value("${com.dipcoin.partner.toll.api.url.requestDetailIHMCL}")
	public String requestDetailIHMCL;

	@Value("${com.dipcoin.partner.toll.api.url.manageException}")
	public String manageExceptionUrl;

	@Value("${com.dipcoin.partner.toll.api.url.responsePayService}")
	public String responsePayServiceUrl;

	@Value("${com.dipcoin.partner.toll.compliance.test.check400}")
	public String complianceTestCheck400;

	@Value("${com.dipcoin.partner.toll.compliance.test.check408}")
	public String complianceTestCheck408;

	@Value("${com.dipcoin.partner.toll.bank.info}")
	public String bankInfo;

	@Value("${com.dipcoin.partner.toll.mappingOfVehicleVC6}")
	public String mappingOfVehicleVC6;

	@Value("${com.dipcoin.partner.toll.bank.info.nameAndIin}")
	public String bankNameAndIin;

	@Value("${com.dipcoin.partner.toll.api.netcHealthCheck.ips}")
	private String[] netcHealthCheckIps;

	@Value("${com.dipcoin.partner.toll.api.netcHealthCheck.port}")
	private int netcHealthCheckPort;

	@Value("${com.dipcoin.partner.toll.api.netcHealthCheck.endPoint}")
	private String netcHealthCheckEndPoint;

	@Value("${com.dipcoin.partner.toll.api.url.ackUrl}")
	public String ackUrl;

	@PreDestroy
	public void cleanUp() throws Exception {
		LOG.info("Cleaning up " + this.getClass().getSimpleName() + " ...");
	}
}
