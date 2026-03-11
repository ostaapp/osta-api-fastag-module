package com.dipcoin.bank.services.utils;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource(value = { "classpath:dipcoin-ebank-services-B0725380.properties",
		"classpath:dipcoin-ebank-services-B0725380.properties" }, ignoreResourceNotFound = true)
@Configuration
@ComponentScan("com.dipcoin.bank.service")
public class VirtualBankProperties {

	@Value("${com.dipcoin.bank.services.utils.VirtualBankProperties.password}")
	private String password;

	@Value("${com.dipcoin.bank.services.utils.VirtualBankProperties.pin}")
	private String pin;

	public String getPassword() {
		return password;
	}

	public String getPin() {
		return pin;
	}

	@PreDestroy
	public void cleanUp() throws Exception {
		System.out.println("Cleaning up " + this.getClass().getSimpleName() + " ...");
	}

}
