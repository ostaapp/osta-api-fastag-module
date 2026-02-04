package com.dipcoin.api.commons;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.dipcoin.db.services.model.User;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.apache.commons.codec.binary.Base64;


import com.dipcoin.api.model.UserDeviceInfoResponse;
//import com.dipcoin.api.model.CustomerRechargeRequest;
//import com.dipcoin.api.model.UserDeviceInfoResponse;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.core.CryptoUtil.AlgoScheme;
import com.dipcoin.db.services.commons.DBConstants.DipcoinUsageType;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;


//import com.dipcoin.db.services.model.DipcoinTransaction;
//import com.dipcoin.db.services.model.Merchant;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component("aPIUtils")
public class APIUtils {
  private static final Logger LOG = LogManager.getLogger(APIUtils.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final BigDecimal hundred = new BigDecimal(100.00);
  private static final String NINE = "9";
  private static final String EIGHT = "8";
  private static final String SEVEN = "7";
  
  
//  @Autowired
//  private DipcoinDBService dipcoinDBService;
  

  public APIUtils() {}

  // save uploaded file to a defined location on the server
  public static boolean saveFile(InputStream uploadedInputStream, String serverLocation) {

    OutputStream outpuStream = null;
    try {
      outpuStream = new FileOutputStream(new File(serverLocation));

      int read = 0;
      byte[] bytes = new byte[1024];
      outpuStream = new FileOutputStream(new File(serverLocation));

      while ((read = uploadedInputStream.read(bytes)) != -1) {
        outpuStream.write(bytes, 0, read);
      }
      outpuStream.flush();

      return true;
    } catch (IOException e) {
      LOG.error("Exception", e);
    } finally {
      if (outpuStream != null)
        try {
          outpuStream.close();
          uploadedInputStream.close();
        } catch (IOException e) {
        }
    }

    return false;
  }

  public static String approvedFilePath(String filePath) {
    return filePath + ".approved";
  }

  public static String approvedFilePath(String filePath, String docType) {
    return filePath + "APPROVED_BACKUP/" + docType;
  }

  public static Map<Integer, User> getUserLookup(final List<User> users) {
    final Map<Integer, User> lookup = new HashMap<>();
    if (!CollectionUtils.isEmpty(users)) {
      for (User user : users) {
        lookup.put(user.getId(), user);
      }
    }
    return lookup;
  }

  private static final String BOOLEAN_TRUE = "true";
  private static final String BOOLEAN_1 = "1";

  public static boolean isBoolean(String flag) {
    return StringUtils.isEmpty(flag) ? false
        : (BOOLEAN_TRUE.equalsIgnoreCase(flag.trim()) || BOOLEAN_1.equals(flag.trim()));
  }

  public static String encryptUserDeviceInfo(CryptoUtil cryptoUtil, UserDeviceInfoResponse info) {
    try {
      return Base64.encodeBase64String(cryptoUtil.encrypt(objectMapper.writeValueAsBytes(info),
          AlgoScheme.AES_ECB_PKCS5PADDING));
    } catch (Exception e) {
    }
    return null;
  }

  public static UserDeviceInfoResponse decryptUserDeviceInfo(CryptoUtil cryptoUtil,
      String base64Data) {
    try {
      return objectMapper.readValue(
          cryptoUtil.decrypt(Base64.decodeBase64(base64Data), AlgoScheme.AES_ECB_PKCS5PADDING),
          UserDeviceInfoResponse.class);
    } catch (Exception e) {
    }
    return null;
  }

//  public static User populatePartnerInternalUser(final HttpServletContext httpServletContext,
//      final User adminUser, final Merchant merchant, final Bank bank) {
//    try {
//      User user = new User();
//      user.setCreatedBy(adminUser.getId());
//      user.setStatus(UserStatus.ACTIVE.value());
//      user.setTnC(BooleanStatus.YES.value());
//
//      // @TODO - Fix these
//      user.setFirstName("MI");
//      user.setLastName("MI");
//      user.setExpertise(0);
//
//      if (merchant != null) {
//        user.setPhone(StringUtils.leftPad(merchant.getReferenceId().substring(1), 10, "0"));
//        user.setRole(UserRoles.MERCHANT_INTERNAL.value());
//        user.setBankMerchantId(merchant.getId());
//        user.setEmail(merchant.getReferenceId() + APIConstants.INTERNAL_PARTNER_EMAIL_SUFFIX);
//
//        return user;
//      } else if (bank != null) {
//        user.setPhone(StringUtils.leftPad(bank.getReferenceId().substring(1), 10, "0"));
//        user.setRole(UserRoles.BANK_INTERNAL.value());
//        user.setBankMerchantId(bank.getId());
//        user.setEmail(bank.getReferenceId() + APIConstants.INTERNAL_PARTNER_EMAIL_SUFFIX);
//
//        return user;
//      }
//
//    } catch (Exception e) {
//      LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught")
//          .format(), e);
//    }
//    return null;
//  }

//  public static User populateVirtualBankUser(final HttpServletContext httpServletContext,
//      final User adminUser, final Bank bank) {
//    try {
//      User user = new User();
//      user.setCreatedBy(adminUser.getId());
//      user.setStatus(UserStatus.ACTIVE.value());
//      user.setTnC(BooleanStatus.YES.value());
//
//      // @TODO - Fix these
//      user.setFirstName("BV");
//      user.setLastName("VB");
//      user.setExpertise(0);
//      user.setPerDayLimit(BigDecimal.ZERO);
//      user.setPerMonthLimit(BigDecimal.ZERO);
//      user.setPerYearLimit(BigDecimal.ZERO);
//
//      if (bank != null) {
//
//        user.setPhone(bank.getReferenceId().substring(1).length() < 10
//            ? StringUtils.leftPad(bank.getReferenceId().substring(1), 10, "1")
//            : StringUtils.leftPad(bank.getReferenceId().substring(5), 10, "1"));
//        user.setRole(UserRoles.VIRTUAL_BANK.value());
//        user.setBankMerchantId(bank.getId());
//        user.setEmail(bank.getReferenceId() + APIConstants.INTERNAL_VIRTUAL_BANK_EMAIL_SUFFIX);
//
//        return user;
//      }
//
//    } catch (Exception e) {
//      LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught")
//          .format(), e);
//    }
//    return null;
//  }

  public static List<String> getUserRoles(final boolean merchantRequest, final boolean bankRequest,
      final boolean brontooRequest, final boolean virtual) {
    if (merchantRequest)
      return UserRoles.merchantRoles();
    else if (bankRequest)
      return UserRoles.bankRoles();
    else if (brontooRequest)
      return UserRoles.brontooRoles();
    else if (virtual)
      return Arrays.asList(UserRoles.VIRTUAL_CUSTOMER.value(), UserRoles.VIRTUAL_MERCHANT.value());
    else
      return Arrays.asList(UserRoles.CUSTOMER.value());
  }

  // wallet user roles
  public static List<String> getVirtualUserRoles() {
    return Arrays.asList(UserRoles.VIRTUAL_CUSTOMER.value(), UserRoles.VIRTUAL_MERCHANT.value());
  }

  /*
   * 
   */
  public static List<String> getUserRoles(String partner) {

    boolean merchantRequest = APIConstants.MERCHANT.equals(partner.toUpperCase());
    boolean brontooRequest = APIConstants.BRONTOO.equals(partner.toUpperCase());
    boolean bankRequest = APIConstants.BANK.equals(partner.toUpperCase());
    boolean virtualRequest = APIConstants.VIRTUAL_CUSTOMER.equals(partner.toUpperCase())
        || APIConstants.VIRTUAL_MERCHANT.equals(partner.toUpperCase());

    return getUserRoles(merchantRequest, bankRequest, brontooRequest, virtualRequest);
  }

  public static String generateTraceId() {
    return CoreUtils.randomAlphaString(6);
  }

  // Find start time of day.
  public static DateTime findStartTimeOfDay(Long timeInMillis, Integer timeInHrs) {
    if (timeInHrs != null && timeInMillis != null)
      return new DateTime(Long.valueOf(timeInMillis), DateTimeZone.UTC).plusHours(timeInHrs)
          .withTimeAtStartOfDay();
    return null;
  }

  // Validate epoch time
  public static boolean isValidEpoch(Long timeInMillis) {
    return timeInMillis >= APIConstants.MIN_TIMESTAMP
        && timeInMillis <= System.currentTimeMillis();
  }

  // Find start time of day.
  public static DateTime findStartTimeOfDay(Long timeInMillis) {
    if (timeInMillis != null)
      return new DateTime(Long.valueOf(timeInMillis), DateTimeZone.UTC).withTimeAtStartOfDay();
    return null;
  }

  // for euronet
  public static String getCamelCase(String input) {

    String result = "";
    char firstChar = input.charAt(0);
    result = result + Character.toLowerCase(firstChar);
    for (int i = 1; i < input.length(); i++) {
      char currentChar = input.charAt(i);
      char previousChar = input.charAt(i - 1);
      if (previousChar == ' ' /*
                               * || Character.isUpperCase(previousChar) ||
                               * Character.isUpperCase(currentChar)
                               */) {
        result = result + Character.toLowerCase(currentChar);
      } else {
        result = result + currentChar;
      }
    }
    return result;
  }

  // Converting amount in rupees
  public static String getAmountPaisaToRupee(String amount) {

    if (StringUtils.isEmpty(amount)) {
      LOG.debug(LogFormatter.instance().data("Amount is null or empty ", amount).format());

      return null;
    }

    Double rupee = new BigDecimal(amount).divide(hundred).doubleValue();
    BigDecimal rupeeDoubleDecimal = truncateDecimal(rupee, 2);

    LOG.debug(
        LogFormatter.instance().data("Amount", amount).data("Converted to Rupee", rupee).format());
    return rupeeDoubleDecimal != null ? rupeeDoubleDecimal.toString() : null;

  }

  // Converting rupees in paise
  public static String getAmountRupeeToPaisa(String amount) {

    if (StringUtils.isEmpty(amount)) {

      LOG.debug(LogFormatter.instance().data("Amount is null or empty ", amount).format());

      return null;
    }

    Long paisa = new BigDecimal(amount).multiply(hundred).longValue();
    LOG.debug(
        LogFormatter.instance().data("Amount", amount).data("Converted to Paise", paisa).format());
    return paisa != null ? paisa.toString() : null;
  }


//  public static String getSSpCode(CustomerRechargeRequest request) {
//    if (StringUtils.isEmpty(request.getServiceProviderCode())
//        || StringUtils.isEmpty(request.getCircleCode()))
//      return null;
//    return request.getServiceProviderCode().toUpperCase() + request.getCircleCode().toUpperCase();
//  }


  public static ResponseEntity<byte[]> generateMultiPartResponse(byte[] data, String fname) {
    return generateMultiPartResponse(data, fname, Optional.empty());
  }

  public static ResponseEntity<byte[]> generateMultiPartResponse(byte[] data, String fname,
      Optional<Map<String, String>> extraHeaders) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fname + "\"");

    if (extraHeaders.isPresent()) {
      extraHeaders.get().entrySet().forEach(e -> headers.add(e.getKey(), e.getValue()));
    }
    return new ResponseEntity<>(data, headers, HttpStatus.OK);
  }

  public static ResponseEntity generateMultiPartResponse(InputStream is, String fname)
      throws IOException {
    return generateMultiPartResponse(IOUtils.toByteArray(is), fname);
  }

  public static BigDecimal truncateDecimal(double number, int numberofDecimals) {
    if (number > 0) {
      return new BigDecimal(String.valueOf(number)).setScale(numberofDecimals, RoundingMode.FLOOR);
    } else {
      return new BigDecimal(String.valueOf(number)).setScale(numberofDecimals,
          RoundingMode.CEILING);
    }
  }

  public static Boolean findDuplicateElements(List<String> alpha) {
    ArrayList<String> tempList = new ArrayList<String>();
    if (!CollectionUtils.isEmpty(alpha)) {
      for (String beta : alpha) {
        if (!tempList.contains(beta)) {
          tempList.add(beta);
        } else {
          return true;
        }
      }
    }

    return false;
  }
  
  public static Boolean checkPhone(String phone) {
    if (phone.startsWith(NINE) || phone.startsWith(EIGHT) || phone.startsWith(SEVEN)) {
      return true;
    }
    return false;
  }
  
  public static Boolean smsEmailAreNotAllowed(Integer dcoinUsageType) {
    ArrayList<Integer> dcoinUsagTypes  = new ArrayList<Integer>();
    dcoinUsagTypes.add(DipcoinUsageType.TOLL.value());
    dcoinUsagTypes.add(DipcoinUsageType.FEE.value());
    dcoinUsagTypes.add(DipcoinUsageType.DEPOSIT.value());

    if (dcoinUsagTypes.contains(dcoinUsageType)) {
      return true;
    }
    return false;

  }
  
//  public Dipcoin getDipcoin(String traceId, int custAcctId) {
//    try {
//      List<Dipcoin> dipcoins = this.dipcoinDBService.findActiveDipcoins(custAcctId,
//          DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());
//      LOG.debug(LogFormatter.instance(traceId)
//          .data("Dipcoin", dipcoins)
//          .data("Dipcoin size", dipcoins.size()).format());
//      if (CollectionUtils.isEmpty(dipcoins)) {
//        return null;
//      }
//      if (dipcoins.size() == NumberUtils.INTEGER_ONE) {
//        LOG.debug(LogFormatter.instance(traceId)
//            .data("Dipcoin 1 Dipcoin", dipcoins.size())
//            .data("Dipcoin size", dipcoins.get(NumberUtils.INTEGER_ZERO).getId()).format());
//        return dipcoins.get(NumberUtils.INTEGER_ZERO);
//      }
//      for (Dipcoin dipcoin : dipcoins) {
//        LOG.debug(LogFormatter.instance(traceId)
//            .data("Active dipcoin with Status 1", dipcoin.getId()).format());
//        if (dipcoin.getParentDipcoinId() != NumberUtils.INTEGER_ZERO) {
//          LOG.debug(LogFormatter.instance(traceId)
//              .data("Multiple parent dipcoin", dipcoin
//              ).format());;
//          List<Dipcoin> dCoins = dipcoinDBService.getParentDipcoins(dipcoin.getId());
//          dCoins.forEach(x->{
//            LOG.debug(LogFormatter.instance(traceId)
//                .data("Coins", x.getId()).format());
//          });
//          for (Dipcoin dCoin : dCoins) {
//            //Fetch the parent dipcoin Dtxn 
//            List<DipcoinTransaction> dtxs =
//                dipcoinDBService.getTransactions(dCoin, Arrays.asList(DipcoinTransactionType.PARTIALLY_USED.value(),
//                        DipcoinTransactionType.COMPLETELY_USED.value()), null, null);
//            dtxs.forEach(x->{
//              LOG.debug(LogFormatter.instance(traceId)
//                  .data("DipcoinTransaction", x.getId()).format());
//            });
//            //If No Dtxn for parent is found then Child dipcoin are no use
//            //So setting Expiry for this Osta.
//            if (CollectionUtils.isEmpty(dtxs)) {
//              List<Dipcoin> dcoins = dipcoinDBService.getChildrenDipcoins(dCoin.getId());
//              dcoins.forEach(x->{
//                LOG.debug(LogFormatter.instance(traceId)
//                    .data("Childen Dipcoin", x.getId()).format());
//              });
//              for (Dipcoin childDipcoin : dcoins) {
//                if (childDipcoin.getStatus() == DBConstants.DipcoinStatus.ACTIVE.value()) {
//                  LOG.debug(LogFormatter.instance(traceId)
//                      .data("Child dipcoin active", childDipcoin.getId()).format());
//                  childDipcoin.setStatus(DBConstants.DipcoinStatus.EXPIRED.value());
//                  childDipcoin.setParentDipcoinId(NumberUtils.INTEGER_ZERO);
//                  dipcoinDBService.updateCoin(childDipcoin);
//                }
//              }
//              LOG.debug(LogFormatter.instance(traceId)
//                  .data("Child dipcoin active", dCoin.getId()).format());
//              return dCoin;
//            }
//            if (dCoin.getStatus() == DBConstants.DipcoinStatus.ACTIVE.value()) {
//              LOG.debug(LogFormatter.instance(traceId)
//                  .data("Updating record not found in DTxn", dCoin.getId()).format());
//              dCoin.setStatus(DBConstants.DipcoinStatus.PROCESSED.value());
//              dipcoinDBService.updateCoin(dCoin);
//            }
//            LOG.debug(LogFormatter.instance(traceId)
//                .data("looping coin ends", dCoins).format());
//          }
//        } else {
//          LOG.debug(LogFormatter.instance(traceId)
//              .data("This Dipcoin is Parent Dipcoin", dipcoin.getId())
//              .data("Dipcoin size", dipcoins.size()).format());
//          return dipcoin;
//        }
//      }
//    } catch (Exception e) {
//      LOG.debug(LogFormatter.instance(traceId)
//          .message("Exception caught while finding dipcoin from multiple active dipcoin").format(),
//          e);
//    }
//    LOG.debug(LogFormatter.instance(traceId)
//        .data("Retruing null","NULL").format());
//    return null;
//  }
  
}
