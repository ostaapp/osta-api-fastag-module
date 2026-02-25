package com.dipcoin.partner.recharge.comm;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import com.dipcoin.commons.PartnerSecret;
import com.dipcoin.partner.utils.PartnerClient.Protocol;
import com.dipcoin.partner.utils.PartnerKeyStoreManager;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class PartnerResponse {

  private static final Logger LOG = LogManager.getLogger(RechargeResponse.class);
  private static ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private PartnerKeyStoreManager partnerKeyStoreR;

  public PartnerResponse() {

  }

  private final HashMap<String, String> FIELDS_MAPPING = new HashMap<>();
  private final HashMap<String, Object> FIELDS_MAPPING_BILL = new HashMap<>();

  // fields to decrypt
  private final ConcurrentHashMap<String, String> DECRYPT_FIELDS = new ConcurrentHashMap<>();

  // dipcoin field to decoding mapping
  private final ConcurrentHashMap<String, String> CLIENT_FIELDS_ENCODING =
      new ConcurrentHashMap<>();

  // bank payload fields decoding
  private final ConcurrentHashMap<String, String> BANK_FIELDS_DECODING = new ConcurrentHashMap<>();

  // bank field to dipcoin field mapping.
  public void setFieldsMapping(final Map<String, String> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (String key : mappings.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(mappings.get(key))) {
          FIELDS_MAPPING.put(key, mappings.get(key));
        }
      }
    }
  }

  public final Map<String, String> getFieldsMapping() {
    return MapUtils.unmodifiableMap(FIELDS_MAPPING);
  }

  // bank field to dipcoin field mapping.
  public void setFieldsMappingBill(final Map<String, Object> mappings) {
    if (!CollectionUtils.isEmpty(mappings)) {
      for (String key : mappings.keySet()) {
        FIELDS_MAPPING_BILL.put(key, mappings.get(key));
      }
    }
  }

  public final Map<String, Object> getFieldsMappingBill() {
    return MapUtils.unmodifiableMap(FIELDS_MAPPING_BILL);
  }

  public void setDecryptFields(final Map<String, String> fields) {
    if (!CollectionUtils.isEmpty(fields)) {
      for (String key : fields.keySet()) {
        if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(fields.get(key))) {
          DECRYPT_FIELDS.put(key, fields.get(key));
        }
      }
    }
  }

  public final Map<String, String> getDecryptFields() {
    return MapUtils.unmodifiableMap(DECRYPT_FIELDS);
  }

  public void setClientFieldsEncoding(final Map<String, String> fields) {
    if (!CollectionUtils.isEmpty(fields)) {
      CLIENT_FIELDS_ENCODING.putAll(fields);
    }
  }

  public final Map<String, String> getClientFieldsEncoding() {
    return MapUtils.unmodifiableMap(CLIENT_FIELDS_ENCODING);
  }

  public void setBankFieldsDecoding(final Map<String, String> fields) {
    if (!CollectionUtils.isEmpty(fields)) {
      BANK_FIELDS_DECODING.putAll(fields);
    }
  }

  public final Map<String, String> getBankFieldsDecoding() {
    return MapUtils.unmodifiableMap(BANK_FIELDS_DECODING);
  }

  protected String processField(PartnerRequestContext requestContext, Protocol protocol,
      String bankReferenceId, String field, String value) throws Exception {
    if (StringUtils.isEmpty(value)) {
      return value;
    }

    PartnerSecret secret = partnerKeyStoreR.getPartnerSecret(requestContext, bankReferenceId);
    if (secret == null) {
      throw new Exception("Partner Server not configured for partnerReferenceId " + bankReferenceId
          + " and protocol " + protocol.type());
    }

    // decode incoming data as configured
    final byte[] decoded = BANK_FIELDS_DECODING.containsKey(field)
        ? partnerKeyStoreR.partnerDecode(requestContext, protocol, bankReferenceId, value)
        : value.getBytes(StandardCharsets.UTF_8);

    // apply decryption as configured
    final byte[] decrypted = (DECRYPT_FIELDS.containsKey(field))
        ? partnerKeyStoreR.decrypt(requestContext, protocol, bankReferenceId, decoded)
        : decoded;

    // encode encrypted data as configured
    return CLIENT_FIELDS_ENCODING.containsKey(field)
        ? partnerKeyStoreR.clientEncode(requestContext, protocol, bankReferenceId, decrypted)
        : new String(decrypted, StandardCharsets.UTF_8);
  }

  protected String decryptPayload(PartnerRequestContext requestContext, Protocol protocol,
      String bankReferenceId, String payload) throws Exception {
    return new String(
        partnerKeyStoreR.decrypt(requestContext, protocol, bankReferenceId,
            partnerKeyStoreR.partnerDecode(requestContext, protocol, bankReferenceId, payload)),
        StandardCharsets.UTF_8);
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
  public abstract boolean processResults(final PartnerRequestContext requestContext,
      final Protocol protocol, final RechargeRequest request, final byte[] rawData,
      RechargeResponse response) throws Exception;

  public abstract boolean processBillResults(final PartnerRequestContext requestContext,
      final Protocol protocol, final BillRequest dcRequest, final byte[] rawData,
      final BillResponse dcResponse) throws Exception;

}
