package com.dipcoin.partner.recharge.client;

public interface RechargeClientFactory {

  public RechargeClient getClient(String clientId);

}
