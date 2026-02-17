package com.dipcoin.partner.recharge.comm;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;
import com.dipcoin.partner.utils.PartnerClient.Protocol;
import com.dipcoin.partner.utils.PartnerRequestContext;

public abstract class PartnerRequest {
  private static final Logger LOG = LogManager.getLogger(RechargeRequest.class);

  // dipcoin to bank query fields mapping
  private final Map<String, String> QUERY_FIELDS_MAPPING = new LinkedHashMap<>();

  // dipcoin to bank payload fields mapping
  private final ConcurrentHashMap<String, String> PAYLOAD_FIELDS_MAPPING =
      new ConcurrentHashMap<>();

  private final Map<String, Object> XYZ_FIELDS_MAPPING = new HashMap<>();

  // header fields
  public final ConcurrentHashMap<String, String> HEADER_FIELDS = new ConcurrentHashMap<>();

  // fields to encrypt
  private final ConcurrentHashMap<String, String> ENCRYPT_FIELDS = new ConcurrentHashMap<>();

  // dipcoin field to encoding mapping
  private final ConcurrentHashMap<String, String> CLIENT_FIELDS_DECODING =
      new ConcurrentHashMap<>();

  // bank field to encoding mapping
  private final ConcurrentHashMap<String, String> BANK_FIELDS_ENCODING = new ConcurrentHashMap<>();

  public void setHeaders(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (String key : mappings.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(mappings.get(key))) {
          HEADER_FIELDS.put(key, mappings.get(key));
        }
      }
    }
  }

  // dipcoin query key to bank key mapping
  public void setQueryFieldMapping(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (String key : mappings.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(mappings.get(key))) {
          QUERY_FIELDS_MAPPING.put(key, mappings.get(key));
        }
      }
    }
  }

  public final Map<String, String> getQueryFieldMapping() {
    return MapUtils.unmodifiableMap(QUERY_FIELDS_MAPPING);
  }

  // dipcoin payload key to bank key mapping

  public void setXyzFieldMapping(final Map<String, Object> mappings) {
    // if (!CollectionUtils.isEmpty(mappings)) {
    // for (String key : mappings.keySet()) {
    // if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(mappings.get(key))) {
    XYZ_FIELDS_MAPPING.putAll(mappings);
    // }
    // }
    // }
  }

  public final Map<String, Object> getXyzFieldMapping() {
    return MapUtils.unmodifiableMap(XYZ_FIELDS_MAPPING);
  }

  public void setPayloadFieldMapping(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (String key : mappings.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(mappings.get(key))) {
          PAYLOAD_FIELDS_MAPPING.put(key, mappings.get(key));
        }
      }
    }
  }

  public void setBillPayloadFieldMapping(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (String key : mappings.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(mappings.get(key))) {
          PAYLOAD_FIELDS_MAPPING.put(key, mappings.get(key));
        }
      }
    }
  }

  public final Map<String, String> getPayloadFieldMapping() {
    return MapUtils.unmodifiableMap(PAYLOAD_FIELDS_MAPPING);
  }

  public final Map<String, String> getBillPayloadFieldMapping() {
    return MapUtils.unmodifiableMap(PAYLOAD_FIELDS_MAPPING);
  }

  public void setEncryptFields(final Map<String, String> fields) {
    if (!CollectionUtils.isEmpty(fields)) {
      for (String key : fields.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(fields.get(key))) {
          ENCRYPT_FIELDS.put(key, fields.get(key));
        }
      }
    }
  }

  public void setBillEncryptFields(final Map<String, Object> fields) {
    if (!CollectionUtils.isEmpty(fields)) {
      for (String key : fields.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty((CharSequence) fields.get(key))) {
          ENCRYPT_FIELDS.put(key, (String) fields.get(key));
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
  private String processField(PartnerRequestContext requestContext, Protocol protocol,
      String bankReferenceId, String field, String value) throws Exception {
    if (StringUtils.isEmpty(value)) {
      return value;
    }

    // PartnerSecret secret = partnerKeyStore.getPartnerSecret(requestContext, bankReferenceId);
    // if (secret == null) {
    // throw new Exception("Partner Server not configured for partnerReferenceId " + bankReferenceId
    // + " and protocol " + protocol.type());
    // }

    // decode incoming data as configured
    final byte[] decoded = value.getBytes(StandardCharsets.UTF_8);

    // apply encryption as configured
    final byte[] encrypted = decoded;

    // encode encrypted data as configured
    return new String(encrypted, StandardCharsets.UTF_8);
  }

  /*
   * Method to map dipcoin params to bank configured params
   */
  protected final RechargeRequest mapAndResolveParams(PartnerRequestContext requestContext,
      Protocol protocol, final RechargeRequest request, final Map<String, String> mapping) {
    if (request == null || CollectionUtils.isEmpty(mapping))
      return request;

    // make copy of request
    try {
      RechargeRequest mappedRequest = (RechargeRequest) request.clone();
      Set<String> keys = new HashSet<>(request.keySet());
      mappedRequest.clear();

      for (String key : mapping.keySet()) {
        if (request.containsKey(key)) {
          mappedRequest.put(mapping.get(key), processField(requestContext, protocol,
              request.getMerchantReferenceId(), key, request.get(key)));
          keys.remove(key);
        }
      }

      // add remaining fields
      for (String key : keys) {
        mappedRequest.put(key, processField(requestContext, protocol,
            request.getMerchantReferenceId(), key, request.get(key)));
      }

      return mappedRequest;
    } catch (Exception e) {
      LOG.error("Exception", e);
      return null;
    }
  }

  protected final BillRequest mapAndResolveBillParams(PartnerRequestContext requestContext,
      Protocol protocol, final BillRequest request, final Map<String, String> mapping,
      final Map<String, Object> transformationMapping) {
    if (request == null || CollectionUtils.isEmpty(mapping))
      return request;

    // make copy of request
    try {
      BillRequest mappedRequest = (BillRequest) request.clone();
      Set<String> keys = new HashSet<>(request.keySet());
      mappedRequest.clear();

      for (String key : mapping.keySet()) {
        if (request.containsKey(key)) {
          if (transformationMapping.containsKey(mapping.get(key))) {

            if (transformationMapping.get(mapping.get(key)) instanceof Map) {
              Map<String, Object> formationMapping =
                  (Map<String, Object>) transformationMapping.get(mapping.get(key));
              Map<String, Object> formationMappingOne = (Map<String, Object>) request.get(key);
              Map<String, Object> formationMappingTwo = new HashMap<String, Object>();
              for (String keyNew : formationMapping.keySet()) {
                if (formationMappingOne.containsKey(keyNew)
                    && formationMapping.get(keyNew) != null) {

                  if (formationMappingOne.get(keyNew) instanceof List) {
                    formationMappingTwo.put(formationMapping.get(keyNew).toString(),
                        formationMappingOne.get(keyNew));
                  } else if (formationMappingOne.get(keyNew) instanceof Map) {
                    Map<String, Object> internalMapping =
                        (Map<String, Object>) formationMappingOne.get(keyNew);
                    Map<String, Object> mappedRequestInternal = new HashMap<String, Object>();

                    for (String keyInternal : internalMapping.keySet()) {
                      mappedRequestInternal.put(keyInternal, internalMapping.get(keyInternal));
                    }

                    if (formationMapping.get(keyNew) instanceof Map
                        || formationMapping.get(keyNew) instanceof List)
                      formationMappingTwo.put(keyNew, mappedRequestInternal);
                    else
                      formationMappingTwo.put(formationMapping.get(keyNew).toString(),
                          mappedRequestInternal);
                  } else {
                    if (formationMapping != null)
                      formationMappingTwo.put(formationMapping.get(keyNew).toString(),
                          processField(requestContext, protocol, request.getMerchantReferenceId(),
                              keyNew, formationMappingOne.get(keyNew).toString()));
                  }

                }
              }

              if (formationMappingTwo != null) {
                mappedRequest.put(mapping.get(key), formationMappingTwo);
                keys.remove(key);
              }
              continue;
            }

            mappedRequest.put(mapping.get(key), request.get(key));
            // mappedValidateRequest.put(mapping.get(key), processField(requestContext, protocol,
            // validateRequest.getRechargeRequest().getMerchantReferenceId(), key,
            // validateRequest.get(key).toString()));
            keys.remove(key);
            continue;
          }
          mappedRequest.put(mapping.get(key), request.get(key));
          keys.remove(key);
        }
      }

      // for (String key : mapping.keySet()) {
      // if (request.containsKey(key)) {
      // mappedRequest.put((String) mapping.get(key), processBillField(requestContext, protocol,
      // request.getMerchantReferenceId(), key, request.get(key)));
      // keys.remove(key);
      // }
      // }

      // add remaining fields
      for (String key : keys) {
        mappedRequest.put(key, processBillField(requestContext, protocol,
            request.getMerchantReferenceId(), key, request.get(key)));
      }


      return mappedRequest;
    } catch (Exception e) {
      LOG.error("Exception", e);
      return null;
    }
  }

  private String processBillField(PartnerRequestContext requestContext, Protocol protocol,
      String bankReferenceId, String field, Object object) throws Exception {
    /*
     * if (StringUtils.isAllEmpty(object)) { return object; }
     */

    // PartnerSecret secret = partnerKeyStore.getPartnerSecret(requestContext, bankReferenceId);
    // if (secret == null) {
    // throw new Exception("Partner Server not configured for partnerReferenceId " + bankReferenceId
    // + " and protocol " + protocol.type());
    // }

    // decode incoming data as configured
    final byte[] decoded = ((String) object).getBytes(StandardCharsets.UTF_8);

    // apply encryption as configured
    final byte[] encrypted = decoded;

    // encode encrypted data as configured
    return new String(encrypted, StandardCharsets.UTF_8);
  }
  
  public final BillRequest mapAndResolveQueryParams(PartnerRequestContext requestContext,
      Protocol protocol, final BillRequest request) {
    
    Map<String, String> mapping = this.getQueryFieldMapping();
    
    if (request == null || CollectionUtils.isEmpty(mapping))
      return null;

    // make copy of request
    try {
      // make copy of mapping
      Map<String, String> mappingCopy = new LinkedHashMap<>(mapping);
      BillRequest mappedRequest = (BillRequest) request.clone();
      Set<String> keys = new HashSet<>(request.keySet());
      mappedRequest.clear();

      for (String key : mappingCopy.keySet()) {
        if (request.containsKey(key)) {
          mappedRequest.put(mapping.get(key), processBillField(requestContext, protocol,
              request.getMerchantReferenceId(), key, request.get(key)));
          keys.remove(key);
        }
      }

      // add remaining fields from mapping
      for (String key : mapping.keySet()) {
        if (!mappedRequest.containsKey(mapping.get(key))) {
          mappedRequest.put(mapping.get(key), processField(requestContext, protocol,
              request.getMerchantReferenceId(), mapping.get(key), key));
        }
      }
      return mappedRequest;
    } catch (Exception e) {
      LOG.error("Exception", e);
      return null;
    }
  }
  
  public abstract byte[] getPayload(final PartnerRequestContext requestContext,
      final Protocol protocol, final RechargeRequest request, int attempt) throws Exception;

  public abstract byte[] getBillPayload(final PartnerRequestContext requestContext,
      final Protocol protocol, final BillRequest request, final int retries) throws Exception;


}
