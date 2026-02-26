package com.dipcoin.bank.services.utils;

import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.zip.CRC32;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.dipcoin.bank.services.client.BankClient;
import com.dipcoin.bank.services.client.BankClient.ChecksumFormat;
import com.dipcoin.bank.services.client.BankClient.Operation;
import com.dipcoin.bank.services.client.BankClient.Protocol;
import com.dipcoin.bank.services.client.BankClientFactory;
import com.dipcoin.bank.services.comm.BankRequest;
import com.dipcoin.bank.services.comm.DipcoinRequest;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.PartnerSecret;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;


@Component
public class BankUtils {
  private static final Logger LOG = LogManager.getLogger(BankUtils.class);

  private BankUtils() throws Exception {}

  @Autowired
  private BankClientFactory bankClientFactory;

  private static String delimitor = "-";
  private static String referenceNumberFormat = "{0}-{1}";

  public String generateDipcoinToBankReferenceNumber(String bankRefId, Operation operation,
      Integer transactionType) {
    StringBuilder bankClientName = new StringBuilder();
    bankClientName.append(bankRefId).append(operation.value());
    if (transactionType == BankTransactionType.AUTHENTICATION_CARD.value())
      bankClientName.append("Type").append(transactionType);

    BankClient client = this.bankClientFactory.getClient(bankClientName.toString());
    // @NOTE - Format : TimeInMilliSec-UUID
    String referenceNumber = MessageFormat.format(referenceNumberFormat,
        String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()), UUID.randomUUID().toString())
        .substring(0, client.getOstaReferenceIdLength());
    if (referenceNumber.endsWith(delimitor))
      return referenceNumber.substring(0, referenceNumber.length() - 1);

    return referenceNumber;
  }
  
  public String generateDipcoinToBankReferenceNumber(String bankRefId, Operation operation) {
	    StringBuilder bankClientName = new StringBuilder();
	    bankClientName.append(bankRefId).append(operation.value());

	    BankClient client = this.bankClientFactory.getClient(bankClientName.toString());
	    // @NOTE - Format : TimeInMilliSec-UUID
	    String referenceNumber = MessageFormat.format(referenceNumberFormat,
	        String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()), UUID.randomUUID().toString())
	        .substring(0, client.getOstaReferenceIdLength());
	    if (referenceNumber.endsWith(delimitor))
	      return referenceNumber.substring(0, referenceNumber.length() - 1);

	    return referenceNumber;
	  }

  public static void printRequestMap(String offset, Map<String, String> mappings) {
    Iterator<Map.Entry<String, String>> it = mappings.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
      LOG.info(offset + ": " + pair.getKey() + " => " + pair.getValue());
    }
  }

  public static String getRequestURI(final BankRequestContext requestContext,
      final Protocol protocol, String baseURI, final DipcoinRequest request,
      final BankRequest bankRequest, PartnerSecret partnerSecret) {

    if (StringUtils.isBlank(baseURI))
      return baseURI;

    DipcoinRequest mappedRequest =
        bankRequest.mapAndResolveQueryParams(requestContext, protocol, request, partnerSecret);

    try {
      if (mappedRequest == null)
        return new URIBuilder(baseURI).build().toString();

      List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
      for (Entry<String, String> entry : mappedRequest.entrySet()) {
        queryParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
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

  public static Map<String, Object> jsonNamespaceToMap(String namespace, String delimiter,
      final Object value) {
    String[] split = namespace.split(delimiter);
    ArrayUtils.reverse(split);

    Deque<String> stack = new ArrayDeque<>();
    stack.addAll(Arrays.asList(split));
    LinkedHashMap<String, Object> data = new LinkedHashMap<>();

    Object tempValue = value;
    while (!stack.isEmpty()) {
      data = new LinkedHashMap<>();
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
          for (String ckey : children.keySet()) {
            flatMap.put(key + delimiter + ckey, children.get(ckey));
          }
        } else if (value instanceof List) { // json array
          // not supported for now. so skip
        } else {
          flatMap.put(key, value);
        }
      }
    }

    return flatMap;
  }

  public String computeChecksum(final byte[] rawPayload, ChecksumFormat format) {
    try {
      if (ChecksumFormat.CRC32.equals(format)) {
        CRC32 crc32 = new CRC32();
        crc32.update(rawPayload);
        return String.valueOf(crc32.getValue());
      }
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update(rawPayload);
      return Hex.encodeHexString(md.digest());
    } catch (NoSuchAlgorithmException e) {
      LOG.error(LogFormatter.instance().message("Exception Caught").format(), e);
    }
    return null;
  }
}
