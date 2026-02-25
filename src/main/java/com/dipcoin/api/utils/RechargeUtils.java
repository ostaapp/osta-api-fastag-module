package com.dipcoin.api.utils;

import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.recharge.comm.BillRequest;
import com.dipcoin.partner.recharge.comm.PartnerRequest;
import com.dipcoin.partner.utils.PartnerClient.Protocol;
import com.dipcoin.partner.utils.PartnerRequestContext;

public class RechargeUtils {
  
  private static final Logger LOG = LogManager.getLogger(RechargeUtils.class);
  
  public static Map<String, Object> jsonNamespaceToMap(String namespace, String delimiter,
      final Object value) {
    String[] split = namespace.split(delimiter);
    ArrayUtils.reverse(split);

    Deque<String> stack = new ArrayDeque<>();
    stack.addAll(Arrays.asList(split));
    Map<String, Object> data = new HashMap<>();

    Object tempValue = value;
    while (!stack.isEmpty()) {
      data = new HashMap<>();
      data.put(stack.pop().trim(), tempValue);

      tempValue = data;
    }

    return data;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> flattenMap(final Map<String, Object> data, String delimiter) {
    Map<String, Object> flatMap = new HashMap<>();
    if (!CollectionUtils.isEmpty(data)) {
      for (String key : data.keySet()) {
        Object value = data.get(key);
        if (value instanceof Map) { // json object
          Map<String, Object> children = flattenMap((Map<String, Object>) value, delimiter);
          flatMap.put(key, children);
        } else if (value instanceof List) { // json array
          List temp = (List) value;

          if (!CollectionUtils.isEmpty(temp)) {
            List tempNew = new ArrayList<Object>();
            for (Object tem : temp) {
              if (tem instanceof Map) {
                Map<String, Object> childrenNew = flattenMap((Map<String, Object>) tem, delimiter);
                tempNew.add(childrenNew);
              }
            }
            temp = tempNew;
          }
          flatMap.put(key, temp);
          // not supported for now. so skip
        } else if (value instanceof Integer) {
          flatMap.put(key, value);
        } else {
          flatMap.put(key, value);
        }
      }
    }

    return flatMap;
  }

  // public static String getRequestURI(final RechargeRequestContext requestContext,
  // final Protocol protocol, String baseURI, final PartnerRequest request) {
  //
  // return baseURI;
  // }

  public static String getRequestURI(PartnerRequestContext requestContext, Protocol protocol,
      String baseURI, PartnerRequest partnerRequest) {
    return baseURI;
  }
  
  public static String getRequestedURI(PartnerRequestContext requestContext, Protocol protocol,
      String baseURI, BillRequest request, PartnerRequest partnerRequest) {
   
    if (StringUtils.isBlank(baseURI))
      return baseURI;

    BillRequest mappedBillRequest =
        partnerRequest.mapAndResolveQueryParams(requestContext, protocol, request);

    try {
      if (mappedBillRequest == null)
        return new URIBuilder(baseURI).build().toString();

      List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
      for (Entry<String, Object> entry : mappedBillRequest.entrySet()) {
        queryParams.add(new BasicNameValuePair(entry.getKey(), (String) entry.getValue()));
      }
      String finalURI = new URIBuilder(baseURI).addParameters(queryParams).build().toString();
      if (finalURI.startsWith("/")) {
        finalURI = finalURI.substring(1, finalURI.length());
      }
      return finalURI;
    } catch (URISyntaxException e) {
      LOG.error(LogFormatter.instance().message("Exception Caught").format(), e);
    }
    return baseURI;
  }
}
