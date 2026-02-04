package com.dipcoin.api.commons;

public class APIDoc {

  private APIDoc() {}

  public static final String tokenNotes =
      "A unique token per login session per browser. The <b>token</b> will be obtained as part of the user <b>login</b> API.";

  public static final String authorizationTokenDefaultValue =
      APIConstants.AUTHORIZATION_TOKEN + " &lt;token&gt;";

  public static final String dcCookieNotes = "Osta Session Cookie";

  public static final String clientTransactionId =
      "A uniqie transaction identifier send by client. If same 'clientTransactionId' is obtained with same request parameters the request will be treated as duplicate and existing record will be returned.";
}
