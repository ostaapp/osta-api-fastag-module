package com.dipcoin.bank.services.client;

/*
 * A placeholder Factory interface. The factory is initiated as a ServiceLocatorFactoryBean in
 * application-context.xml
 */
public interface BankClientFactory {

  /*
   * Method stub. Spring ServiceLocatorFactoryBean will do the resource allocation
   */
  public BankClient getClient(String clientId);

}
