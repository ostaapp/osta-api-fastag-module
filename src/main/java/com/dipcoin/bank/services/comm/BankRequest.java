package com.dipcoin.bank.services.comm;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import com.dipcoin.bank.services.client.BankClient.Protocol;
import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.bank.services.utils.BankUtils;
import com.dipcoin.bank.services.utils.PartnerKeyStore;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.PartnerSecret;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Map dipcoin request to appropriate bank request
 */
public abstract class BankRequest {
  private static final Logger LOG = LogManager.getLogger(BankRequest.class);
  private static ObjectMapper objectMapper = new ObjectMapper();

  private static boolean containValue = false;
  // dipcoin to bank query fields mapping
  private final ConcurrentHashMap<String, String> QUERY_FIELDS_MAPPING = new ConcurrentHashMap<>();

  // dipcoin to bank payload fields mapping
  private final LinkedHashMap<String, String> PAYLOAD_FIELDS_MAPPING =
      new LinkedHashMap<>();

  // Attribute mapping 
  private final LinkedHashMap<String, String> ATTRIBUTE_MAPPING =
      new LinkedHashMap<>();
  
  // header fields
  private final ConcurrentHashMap<String, String> HEADER_FIELDS = new ConcurrentHashMap<>();

  // fields to encrypt
  private final ConcurrentHashMap<String, String> ENCRYPT_FIELDS = new ConcurrentHashMap<>();

  // dipcoin field to encoding mapping
  private final ConcurrentHashMap<String, String> CLIENT_FIELDS_DECODING =
      new ConcurrentHashMap<>();
  
  private final ConcurrentHashMap<String, Object> PAY_FIELDS_MAPPING = new ConcurrentHashMap<>();
  
  public final Map<String, Object> getPayFieldMapping() {
    return MapUtils.unmodifiableMap(PAY_FIELDS_MAPPING);
  }

  public void setPayFieldMapping(final Map<String, Object> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
       PAY_FIELDS_MAPPING.putAll(mappings);
    }
  }

  // bank field to encoding mapping
  private final ConcurrentHashMap<String, String> BANK_FIELDS_ENCODING = new ConcurrentHashMap<>();

  @Autowired
  private PartnerKeyStore partnerKeyStore;

  public void setHeaders(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (Entry<String, String> entry : mappings.entrySet()) {
        if (!StringUtils.isEmpty(entry.getKey()) && !StringUtils.isEmpty(entry.getValue())) {
          HEADER_FIELDS.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public void setHeader(final String key, final String value) {
    HEADER_FIELDS.put(key, value);
  }

  public final Map<String, String> getHeaders() {
    return MapUtils.unmodifiableMap(HEADER_FIELDS);
  }

  // dipcoin query key to bank key mapping
  public void setQueryFieldMapping(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (Entry<String, String> entry : mappings.entrySet()) {
        if (!StringUtils.isEmpty(entry.getKey()) && !StringUtils.isEmpty(entry.getValue())) {
          QUERY_FIELDS_MAPPING.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public final Map<String, String> getQueryFieldMapping() {
    return MapUtils.unmodifiableMap(QUERY_FIELDS_MAPPING);
  }

  // dipcoin payload key to bank key mapping
  public void setPayloadFieldMapping(final LinkedHashMap<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (Entry<String, String> entry : mappings.entrySet()) {
        if (!StringUtils.isEmpty(entry.getKey()) && !StringUtils.isEmpty(entry.getValue())) {
          PAYLOAD_FIELDS_MAPPING.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public final Map<String, String> getPayloadFieldMapping() {
    return MapUtils.unmodifiableMap(PAYLOAD_FIELDS_MAPPING);
  }
  
  public void setAttributeMapping(final LinkedHashMap<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (Entry<String, String> entry : mappings.entrySet()) {
        if (!StringUtils.isEmpty(entry.getKey()) && !StringUtils.isEmpty(entry.getValue())) {
          ATTRIBUTE_MAPPING.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public final Map<String, String> getAttributeMapping() {
    return MapUtils.unmodifiableMap(ATTRIBUTE_MAPPING);
  }

  public void setEncryptFields(final Map<String, String> fields) {
    if (!CollectionUtils.isEmpty(fields)) {
      for (Entry<String, String> entry : fields.entrySet()) {
        if (!StringUtils.isEmpty(entry.getKey()) && !StringUtils.isEmpty(entry.getValue())) {
          ENCRYPT_FIELDS.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public final Map<String, String> getEncryptFields() {
    return MapUtils.unmodifiableMap(ENCRYPT_FIELDS);
  }

  public void setClientFieldsDecoding(final Map<String, String> fields) {
    if (!CollectionUtils.isEmpty(fields)) {
      CLIENT_FIELDS_DECODING.putAll(fields);
    }
  }

  public final Map<String, String> getClientFieldsDecoding() {
    return MapUtils.unmodifiableMap(CLIENT_FIELDS_DECODING);
  }

  public void setBankFieldsEncoding(final Map<String, String> fields) {
    if (!CollectionUtils.isEmpty(fields)) {
      BANK_FIELDS_ENCODING.putAll(fields);
    }
  }

  public final Map<String, String> getBankFieldsEncoding() {
    return MapUtils.unmodifiableMap(BANK_FIELDS_ENCODING);
  }

  /*
   * Process fields as needed
   */
  private String processField(BankRequestContext requestContext, Protocol protocol,
      String bankReferenceId, String field, String value, PartnerSecret secret) throws Exception {
    if (StringUtils.isEmpty(value)) {
      return value;
    }

    //PartnerSecret secret = partnerKeyStore.getPartnerSecret(requestContext, bankReferenceId);
    
    if (secret == null) {
      throw new Exception("Partner Server not configured for partnerReferenceId " + bankReferenceId
          + " and protocol " + protocol.type());
    }

    // decode incoming data as configured
    final byte[] decoded = CLIENT_FIELDS_DECODING.containsKey(field)
        ? partnerKeyStore.clientDecode(requestContext, protocol, bankReferenceId, value)
        : value.getBytes(StandardCharsets.UTF_8);

    // apply encryption as configured
    final byte[] encrypted = ENCRYPT_FIELDS.containsKey(field)
        ? partnerKeyStore.encrypt(requestContext, protocol, bankReferenceId, decoded, secret)
        : decoded;

    // encode encrypted data as configured
    return BANK_FIELDS_ENCODING.containsKey(field)
        ? partnerKeyStore.partnerEncode(requestContext, protocol, bankReferenceId, encrypted, secret)
        : new String(encrypted, StandardCharsets.UTF_8);
  }

  protected String encryptPayload(BankRequestContext requestContext, Protocol protocol,
      String bankReferenceId, String payload, PartnerSecret partnerSecret) throws Exception {
    return partnerKeyStore.partnerEncode(requestContext, protocol, bankReferenceId,
        partnerKeyStore.encrypt(requestContext, protocol, bankReferenceId,
            payload.getBytes(StandardCharsets.UTF_8), partnerSecret), partnerSecret);
  }

  /*
   * Method to map dipcoin params to bank configured params
   */
  protected final DipcoinRequest mapAndResolveParams(BankRequestContext requestContext,
      Protocol protocol, final DipcoinRequest request, final Map<String, String> mapping, PartnerSecret partnerSecret) {
    if (request == null || CollectionUtils.isEmpty(mapping))
      return null;

    // make copy of request
    try {
      // make copy of mapping
      Map<String, String> mappingCopy = new LinkedHashMap<>(mapping);
      DipcoinRequest mappedRequest = (DipcoinRequest) request.clone();
      Set<String> keys = new HashSet<>(request.keySet());
      mappedRequest.clear();
      for (String key : mappingCopy.keySet()) {
        if (request.containsKey(key)) {
          mappedRequest.put(mapping.get(key), processField(requestContext, protocol,
              request.getBankReferenceId(), key, request.get(key), partnerSecret));
          keys.remove(key);
        } else {
          mappedRequest.put(key, processField(requestContext, protocol,
              request.getBankReferenceId(), key, mapping.get(key), partnerSecret));
          keys.remove(key);
        }
      }
      
      return mappedRequest;
    } catch (Exception e) {
      LOG.error("Exception", e);
      return null;
    }
  }
  
  protected final DipcoinRequest mapAndResolveNestedParams(BankRequestContext requestContext,
      Protocol protocol, final DipcoinRequest request, final Map<String, String> mapping, PartnerSecret partnerSecret) {
    if (request == null || CollectionUtils.isEmpty(mapping))
      return null;

    // make copy of request
    try {
      // make copy of mapping
      Map<String, String> mappingCopy = new LinkedHashMap<>(mapping);
      DipcoinRequest mappedRequest = (DipcoinRequest) request.clone();
      Set<String> keys = new HashSet<>(request.keySet());
      mappedRequest.clear();

      for (String key : mappingCopy.keySet()) {
        if (request.containsKey(key)) {
          mappedRequest.put(mapping.get(key), processField(requestContext, protocol,
              request.getBankReferenceId(), key, request.get(key), partnerSecret));
          keys.remove(key);
        } else {
          mappedRequest.put(key, processField(requestContext, protocol,
              request.getBankReferenceId(), key, mapping.get(key), partnerSecret));
          keys.remove(key);
        }
      }
      
      return mappedRequest;
    } catch (Exception e) {
      LOG.error("Exception", e);
      return null;
    }
  }


  public final DipcoinRequest mapAndResolveQueryParams(BankRequestContext requestContext,
      Protocol protocol, final DipcoinRequest request, PartnerSecret partnerSecret) {
    return mapAndResolveParams(requestContext, protocol, request, QUERY_FIELDS_MAPPING, partnerSecret);
  }

  public final DipcoinRequest mapAndResolveHeaders(BankRequestContext requestContext,
      Protocol protocol, final DipcoinRequest request, PartnerSecret partnerSecret) {
    return mapAndResolveParams(requestContext, protocol, request, HEADER_FIELDS, partnerSecret);
  }

  @Override
  public String toString() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
    }

    return null;
  }

  /*
   * 
   */
  public abstract byte[] getPayload(final BankRequestContext requestContext,
      final Protocol protocol, final DipcoinRequest request, int attempt, boolean encryptPayload,
      int encryptIterations, PartnerSecret partnerSecret) throws Exception;
  
  protected Map<String, Object> resolvedRequest(final DipcoinRequest mappedRequest, final List<DipcoinRequest> nestedMappedRequest) {
    
    Map<String, Object> resolvedRequest = new LinkedHashMap<>();
    
    Map<String, Object> nestedMap = getPayFieldMapping();
    
    Map<String, List<Map<String, Object>>> nestedReslovedMap = new LinkedHashMap<>();
    List<Map<String, Object>> nestedList = new LinkedList<Map<String,Object>>();
    for (String nestedKey : nestedMap.keySet()) {
      for(DipcoinRequest innerKey : nestedMappedRequest) {
        Map<String, Object> nestedResolvedRequest = new LinkedHashMap<>();
        resolveRequest(innerKey, nestedResolvedRequest);
        nestedList.add(nestedResolvedRequest);
      }
      nestedReslovedMap.put(nestedKey, nestedList);
      Map<String, Object> data = BankUtils.jsonNamespaceToMap(nestedKey, "\\.", nestedList);
      resolvedRequest.putAll(data);
    } 
    
    resolveRequest(mappedRequest, resolvedRequest);
    
    return resolvedRequest;
  }
  
  @SuppressWarnings("unchecked")
  protected Map<String, Object> resolveRequest(final DipcoinRequest mappedRequest,
      Map<String, Object> resolvedRequest) {
    LinkedHashMap<String, Object> resolvedRequestLevelOne = new LinkedHashMap<>();
    LinkedHashMap<String, Object> resolvedRequestLevelTwo = new LinkedHashMap<>();
    LinkedHashMap<String, Object> resolvedRequestLevelThree = new LinkedHashMap<>();
    LinkedHashMap<String, Object> resolvedRequestLevelFour = new LinkedHashMap<>();
    LinkedHashMap<String, Object> resolvedRequestLevelFive = new LinkedHashMap<>();
    LinkedHashMap<String, Object> resolvedRequestLevelSix = new LinkedHashMap<>();

    for (String key : mappedRequest.keySet()) {
      Map<String, Object> data = BankUtils.jsonNamespaceToMap(key, "\\.", mappedRequest.get(key));
      Entry<String, Object> entry = data.entrySet().iterator().next();
      if (resolvedRequest.containsKey(entry.getKey()) && entry.getValue() instanceof Map) {
        Map<String, Object> value = (Map<String, Object>) resolvedRequest.get(entry.getKey());
        resolvedRequestLevelOne.putAll(value);
        Entry<String, Object> entryOne =
            ((Map<String, Object>) entry.getValue()).entrySet().iterator().next();
        if (resolvedRequestLevelOne.containsKey(entryOne.getKey())
            && entryOne.getValue() instanceof Map) {
          Map<String, Object> valueOne =
              (Map<String, Object>) resolvedRequestLevelOne.get(entryOne.getKey());
          resolvedRequestLevelTwo.putAll(valueOne);
          Entry<String, Object> entryTwo =
              ((Map<String, Object>) entryOne.getValue()).entrySet().iterator().next();
          if (resolvedRequestLevelTwo.containsKey(entryTwo.getKey())
              && entryTwo.getValue() instanceof Map) {
            Map<String, Object> valueTwo =
                (Map<String, Object>) resolvedRequestLevelTwo.get(entryTwo.getKey());
            resolvedRequestLevelThree.putAll(valueTwo);
            Entry<String, Object> entryThree =
                ((Map<String, Object>) entryTwo.getValue()).entrySet().iterator().next();
            if (resolvedRequestLevelThree.containsKey(entryThree.getKey())
                && entryThree.getValue() instanceof Map) {
              Map<String, Object> valueThree =
                  (Map<String, Object>) resolvedRequestLevelThree.get(entryThree.getKey());
              resolvedRequestLevelFour.putAll(valueThree);
              Entry<String, Object> entryFour =
                  ((Map<String, Object>) entryThree.getValue()).entrySet().iterator().next();
              if (resolvedRequestLevelFour.containsKey(entryFour.getKey())
                  && entryFour.getValue() instanceof Map) {
                Map<String, Object> valueFour =
                    (Map<String, Object>) resolvedRequestLevelFour.get(entryFour.getKey());
                resolvedRequestLevelFive.putAll(valueFour);
                Entry<String, Object> entryFive =
                    ((Map<String, Object>) entryFour.getValue()).entrySet().iterator().next();
                if (resolvedRequestLevelFive.containsKey(entryFive.getKey())
                    && entryFive.getValue() instanceof Map) {
                  Map<String, Object> valueFive =
                      (Map<String, Object>) resolvedRequestLevelFive.get(entryFive.getKey());
                  resolvedRequestLevelSix.putAll(valueFive);
                  Entry<String, Object> entrySix =
                      ((Map<String, Object>) entryFive.getValue()).entrySet().iterator().next();
                  if (resolvedRequestLevelSix.containsKey(entrySix.getKey())
                      && entrySix.getValue() instanceof Map) {
                    Map<String, Object> valueSix =
                        (Map<String, Object>) resolvedRequestLevelSix.get(entrySix.getKey());
                    valueSix.putAll((Map<String, Object>) entrySix.getValue());
                  } else {
                    valueFive.putAll((LinkedHashMap<String, Object>) entryFive.getValue());
                  }
                } else {
                  valueFour.putAll((LinkedHashMap<String, Object>) entryFour.getValue());
                }
              } else {
                valueThree.putAll((LinkedHashMap<String, Object>) entryThree.getValue());
              }
            } else {
              valueTwo.putAll((LinkedHashMap<String, Object>) entryTwo.getValue());
            }
          } else {
            valueOne.putAll((LinkedHashMap<String, Object>) entryOne.getValue());
          }
        } else {
          value.putAll((LinkedHashMap<String, Object>) entry.getValue());
        }
      } else {
        resolvedRequest.putAll(data);

      }
    }
    return resolvedRequest;
  }
 
}
