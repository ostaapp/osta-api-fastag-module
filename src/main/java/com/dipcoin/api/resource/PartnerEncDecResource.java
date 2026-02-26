package com.dipcoin.api.resource;

import javax.ws.rs.core.Response.Status;

import org.springframework.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jasypt.commons.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.MerchantDipcoinRequest;
import com.dipcoin.api.model.PartnerEncryptedRequest;
import com.dipcoin.api.model.PartnerRequest;
import com.dipcoin.api.model.PartnerResponse;
import com.dipcoin.api.model.PartnerRegisterCustomerRequest;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.PartnerSecret;
import com.dipcoin.commons.PartnerSecret.Key;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import com.dipcoin.partner.utils.PartnerClient;
import com.dipcoin.partner.utils.PartnerKeyStoreManager;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Bytes;

@Component("partnerEncDecResource")
public class PartnerEncDecResource extends PartnerClient {

  private static final Logger LOG = LogManager.getLogger(PartnerEncDecResource.class);

  private static ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private PartnerKeyStoreManager partnerKeyStoreManager;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private MerchantResource merchantResource;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;

  public void setHttpServletContext(HttpServletContext httpServletContext) {
    this.httpServletContext = httpServletContext;
  }

  public PartnerEncDecResource() {
    super(Protocol.HTTP, "");
  }

  // decrypt Partner Payment Details.
  public ResponseEntity decryptPaymentDetails(PartnerRequest partnerRequest, User loggedInUser,
      Merchant loggedInUserMerchant, String checksum) throws Exception {

    PartnerResponse partnerResponse = new PartnerResponse();
    PartnerRequestContext partnerRequestContext = new PartnerRequestContext();

    partnerRequestContext.setTraceId(httpServletContext.getTraceId());

    if (CommonUtils.isEmpty(checksum)) {
      partnerResponse.addHeaderCode(HeaderCode.CHECKSUM_MISSING);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
    }

    String checksumKey =
        getChecksumKey(partnerRequestContext, loggedInUserMerchant.getReferenceId());

    // validate checksum
    if (!checksum.equalsIgnoreCase(CoreUtils.computeChecksum(
        Bytes.concat(partnerRequest.getEncryptedPayload().getBytes(), checksumKey.getBytes()),
        CoreUtils.ChecksumFormat.SHA256))) {
      LOG.debug(LogFormatter.instance().data("Checksum validation failed", checksum).format());
      partnerResponse.addHeaderCode(HeaderCode.INVALID_CHECKSUM);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
    }

    LOG.debug(LogFormatter.instance().data("Checksum validated", checksum).format());

    // decrypt payload
    byte[] payload = partnerKeyStoreManager.decrypt(partnerRequestContext, Protocol.HTTP,
        loggedInUserMerchant.getReferenceId(),
        partnerKeyStoreManager.partnerDecode(partnerRequestContext, Protocol.HTTP,
            loggedInUserMerchant.getReferenceId(), partnerRequest.getEncryptedPayload()));

    // retrive data from decrypted payload
    PartnerRequest mappedRequest = objectMapper.readValue(payload, PartnerRequest.class);
    mappedRequest.setMerchantName(loggedInUserMerchant.getName());

    LOG.debug(LogFormatter.instance().data("Partner Amount", mappedRequest.getAmount())
        .data("Partner orderId", mappedRequest.getOrderId())
        .data("Partner ParttransactionId", mappedRequest.getPartnerTransactionReferenceId())
        .format());


    return ResponseEntity.ok(mappedRequest);
  }

  public PartnerResponse encryptPartnerResponse(String partnerReferenceId, String rawdata)
      throws JsonProcessingException, Exception {

    PartnerRequestContext partnerRequestContext = new PartnerRequestContext();
    partnerRequestContext.setTraceId(httpServletContext.getTraceId());

    PartnerResponse partnerResponse = new PartnerResponse();
    // encrypt the Object
    String encryptedResponse = partnerKeyStoreManager.clientEncode(partnerRequestContext,
        Protocol.HTTP, partnerReferenceId, partnerKeyStoreManager.encrypt(partnerRequestContext,
            Protocol.HTTP, partnerReferenceId, rawdata.getBytes()));

    String checkSumKey = getChecksumKey(partnerRequestContext, partnerReferenceId);
    // checksum of encryptedResponse
    String checksumOfEncryptedResponse = CoreUtils.computeChecksum(
        Bytes.concat(encryptedResponse.getBytes(), checkSumKey.getBytes()),
        CoreUtils.ChecksumFormat.SHA256);

    partnerResponse.setEncryptedPayload(encryptedResponse);
    partnerResponse.setChecksum(checksumOfEncryptedResponse);

    LOG.debug(LogFormatter.instance().data("Encrypted Response", encryptedResponse)
        .data("encryptedchecsum", checksumOfEncryptedResponse).format());

    return partnerResponse;
  }

  private String getChecksumKey(PartnerRequestContext partnerRequestContext,
      String partnerReferenceId) throws Exception {
    PartnerSecret secret = this.partnerKeyStoreManager.getPartnerSecret(partnerRequestContext,
        partnerReferenceId.concat(APIConstants.CHECKSUM_KEY)); // use for checksum key _checksum
                                                               // format
    Key partnerKey = secret != null ? secret.getKeyByProtocol(getProtocol().name()) : null;
    String secretKey = partnerKey != null ? partnerKey.getSecretKey() : "";
    return secretKey;
  }

  // for test purpose to be removed
  public ResponseEntity encryptPartnerDetails(PartnerEncryptedRequest partnerEncryptedRequest,
      Merchant loggedInUserMerchant, Bank loggedInUserbank ) throws JsonProcessingException, Exception {
	 if(loggedInUserbank != null) {
		   PartnerResponse partnerResponse = encryptPartnerResponse(loggedInUserbank.getReferenceId(),
			        objectMapper.writeValueAsString(partnerEncryptedRequest)); 
		   return ResponseEntity.ok(partnerResponse);
	 }
		 
    PartnerResponse partnerResponse = encryptPartnerResponse(loggedInUserMerchant.getReferenceId(),
        objectMapper.writeValueAsString(partnerEncryptedRequest));

    return ResponseEntity.ok(partnerResponse);

  }

  public ResponseEntity generatePartnerQrcode(User user, Merchant merchant,
      PartnerRequest partnerRequest, Integer width, Integer height, String image) throws Exception {

    if (partnerRequest == null) {
      LOG.error(LogFormatter.instance().data("BAD_Request", Status.BAD_REQUEST));
      // return Response.noContent().status(Status.BAD_REQUEST).build();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(APIResponse.error(HeaderCode.REQUEST_OK));
    }


    if (!this.userDBService.merchantRepresentative(user) || !this.userDBService.isActive(user)) {
      LOG.error(
          LogFormatter.instance().data("Unauthorized User", HeaderCode.USER_UNAUTHORIZED).format());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
    }

    MerchantDipcoinRequest merchantDipcoinRequest = new MerchantDipcoinRequest();
    merchantDipcoinRequest.setAmount(partnerRequest.getAmount());
    merchantDipcoinRequest.setCurrency(partnerRequest.getCurrency());
    merchantDipcoinRequest
        .setMerchantTransactionReferenceId(partnerRequest.getPartnerTransactionReferenceId());
    merchantDipcoinRequest.setOrderId(partnerRequest.getOrderId());


    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching User Device")
        .data("user", user.getId()).format());

    return merchantResource.getMerchantDipcoinRequestQRCode(user, merchant, merchantDipcoinRequest,
        width, height, image);

  }
  // decrypt User Registration Details.
  public ResponseEntity decryptDetails(PartnerRegisterCustomerRequest registerBankCustomerRequest , User user,
      Bank bank, String checksum) throws Exception {

    PartnerResponse partnerResponse = new PartnerResponse();
    PartnerRequestContext partnerRequestContext = new PartnerRequestContext();

    partnerRequestContext.setTraceId(httpServletContext.getTraceId());

    if (CommonUtils.isEmpty(checksum)) {
      partnerResponse.addHeaderCode(HeaderCode.CHECKSUM_MISSING);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
    }

    String checksumKey =
        getChecksumKey(partnerRequestContext, bank.getReferenceId());

    // validate checksum
    if (!checksum.equalsIgnoreCase(CoreUtils.computeChecksum(
        Bytes.concat(registerBankCustomerRequest.getEncryptedPayload().getBytes(), checksumKey.getBytes()),
        CoreUtils.ChecksumFormat.SHA256))) {
      LOG.debug(LogFormatter.instance().data("Checksum validation failed", checksum).format());
      partnerResponse.addHeaderCode(HeaderCode.INVALID_CHECKSUM);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
    }

    LOG.debug(LogFormatter.instance().data("Checksum validated", checksum).format());

    // decrypt payload
    byte[] payload = partnerKeyStoreManager.decrypt(partnerRequestContext, Protocol.HTTP,
    		bank.getReferenceId(),
        partnerKeyStoreManager.partnerDecode(partnerRequestContext, Protocol.HTTP,
        		bank.getReferenceId(), registerBankCustomerRequest.getEncryptedPayload()));

    // retrive data from decrypted payloady
    System.out.println(payload.toString());
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .data("raw callback data", new String(payload)).format());
    PartnerRegisterCustomerRequest mappedRequest = objectMapper.readValue(payload, PartnerRegisterCustomerRequest.class);
 

		/*
		 * LOG.debug(LogFormatter.instance().data("Partner Amount",
		 * mappedRequest.getBankName()) .data("Partner ParttransactionId",
		 * mappedRequest.getPartnerTransactionReferenceId()) .format());
		 */


    return ResponseEntity.ok(mappedRequest);
  }

}
