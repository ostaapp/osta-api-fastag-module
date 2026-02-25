package com.dipcoin.partner.toll.commons;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public enum TollErrorCodes {

  //@formatter:off
  Error_code_SUCCESS("000", "Success"), 
  Error_code_UNREGISTERED_TAGS("001", "Unregistered tags"), 
  Error_code_VEHICLE_CLASS_AVC_IS_NOT_EQUAL_TO_MAPPER_VEHICLE_CLASS_("002", "Vehicle Class.AVC is not equal to mapper vehicle class."), 
  Error_code_CONDITION_APPLIED_READERREADTIME_TIME_OF_UPDATING_TAG_ID_IN_LOW_BAL_EX_LIST_20_MINUTES_TAG_IN_LOW_BALANCE_EXCEPTION_LIST("051", "Condition Applied [ReaderReadTime - Time of updating tag Id in low bal ex list > 20 minutes].Tag in Low balance exception list."),
  Error_code_INVALID_TAG_ID("052", "Invalid tag id"), 
  Error_code_TRANSACTION_WITH_ACQ_LIABILITY_BUT_CUSTOMER_IS_HAVING_SUFFICIENT_BALANCE_IN_TAG_LINKED_ACCOUNT("003", "Transaction with acq liability but customer is having sufficient balance in tag linked account"), 
  Error_code_TRANSACTION_WITH_ACQ_LIABILITY_BUT_CUSTOMER_IS_NOT_HAVING_SUFFICIENT_BALANCE_IN_TAG_LINKED_ACCOUNT("053", "Transaction with acq liability but customer is not having sufficient balance in tag linked account"), 
  Error_code_EXPIRED_TRANSACTION_NETC_SWITCH_TRANSACTION_TIME_READERREADTIME_3_DAYS("054", "Expired transaction [NETC Switch Transaction time - ReaderReadTime > 3 days]"), 
  Error_code_INVALID_VEHICLE_REGISTRATION_NUMBER("055", "invalid vehicle registration number (Unregistered Tag/Vehicle)"), 
  Error_code_INVALID_ISSUE_DATE("056", "invalid issue date"),
  Error_code_INVALID_COMMERCIAL_VEHICLE_TYPE("057", "Invalid commercial vehicle type"),
  Error_code_INVALID_VEHICLE_CLASS("058", "Invalid vehicle class"), 
  Error_code_CONDITION_TAG_IN_EXEMPTED_VEHICLE_CLASS("059", "Condition [transaction amount > 0]Tag in exempted vehicle class"), 
  Error_code_INVALID_TID("060", "Invalid TID"), 
  Error_code_INVALID_TAG_SIGNATURE("061", "Invalid tag signature"), 
  Error_code_INVALID_ACQUIRER_ID("062", "Invalid acquirer id"), 
  Error_code_INVALID_PLAZA_ID("004", "Invalid plaza id"), 
  Error_code_INVALID_TOLL_FARE("005", "Invalid toll fare"), 
  Error_code_INCORRECT_TRANSACTION_STATUS("063", "incorrect transaction status"), 
  Error_code_INVALID_MERCHANT_TYPE("006", "Invalid merchant type"), 
  Error_code_INVALID_MERCHANT_SUB_TYPE("007", "Invalid merchant sub-type"), 
  Error_code_INVALID_TAG_VERIFIED_STATUS_AND_TAG_IS_REGISTERED_IN_MAPPER("008", "Invalid tag verified status and tag is registered in mapper"), 
  Error_code_INVALID_TAG_VERIFIED_STATUS_AND_TAG_IS_NOT_REGISTERED_IN_MAPPER("064", "Invalid tag verified status and tag is not registered in mapper"), 
  errorCode_INVALID_FIELD_LENGTH_FOR_PROCESSING_RESTRICTION_RESULT("009", "Invalid field length, for processing restriction result"), 
  errorCode_INCORRECT_SIGNATURE_AUTHENTICATION_VALUE("065","Incorrect signature authentication value"), 
  //errorCode_FOR_XML_MESSAGES_WITH_VERSION_SIGNAUTH("000", "FOR XML MESSAGES WITH VERSION 1.0	signAuth"), 
  errorCode_INVALID_READER_READ_TIME_I_E_FUTURE_DATE_TRANSACTIONS("066", "invalid reader read time i.e. future date transactions"), 
  Error_code_INVALID_MESSAGE_FORMAT("999", "Invalid message format"), 
  //errorCode_FOR_XML_MESSAGES_WITH_VERSION_INCORRECT_PUBLIC_KEY_CHECK_SUM("000", "FOR XML MESSAGES WITH VERSION 1.0	Incorrect public key check sum"), 
  errorCode_INVALID_TRANSACTION_ID("067", "Invalid transaction ID"), 
  errorCode_INVALID_PAYER_ADDRESS("068", "invalid payer address"), 
  errorCode_INVALID_PAYER_TYPE("069", "invalid payer type"), 
  //errorCode_INVALID_PAYER_NAME("000", "invalid payer name"), 
  errorCode_INVALID__PAYEE_ADDRESS("070", "invalid  payee address"), 
  errorCode_INVALID__PAYEE_TYPE("071", "invalid  payee type"), 
  //errorCode_INVALID__PAYEE_NAME("000", "invalid  payee name"), 
  errorCode_TRANSACTION_WITH_HIGH_RISK_SCORE("010", "Transaction with high risk score"), 
  //errorCode_FOR_XML_MESSAGES_WITH_VERSION_WIM_VALUE_PROVIDED_IN_THE_XML_ATTRIBUTE("000", "FOR XML MESSAGES WITH VERSION 1.0	WIM value provided in the xml attribute"), 
  //errorCode_FOR_XML_MESSAGES_WITH_VERSION_TOLL_FARE_PENALTY_CALCULATED_ON_WIM_AND_THE_TRANSACTION_AMOUNT_IS_LESS_THAN_2000("000", "FOR XML MESSAGES WITH VERSION 1.0	Toll fare penalty calculated on WIM and the transaction amount is less than 2000"), 
  errorCode_TRANSACTION_AMOUNT("072", "Transaction amount > 2000"), 
  errorCode_CLONED_TRANSACTION_AS_PER_THE_DEFINITION_OF_NEAR_TIME("073", "cloned transaction as per the definition of near time"), 
  errorCode_INACTIVE_TAGS("074", "Inactive tags"), 
  errorCode_AVC_IS_INVALID_BUT_MAPPER_VC_IS_VALID("075", "AVC is invalid but mapper VC is valid"), 
  errorCode_INVALID_PLAZA_LOCATION("011", "Invalid plaza location"), 
  errorCode_INVALID_LANE_DIRECTION("012", "Invalid lane direction"), 
  errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO("076", "Transaction status is set to failed and amount is greater than zero"), 
  errorCode_ICICI_TAGS_NOT_REGISTERED_ON_MAPPER("077", "ICICI tags not registered on Mapper"), 
  error_code_CREDIT_TO_CUSTOMER_ACCOUNT_MORE_THAN_2000("013", "Credit to customer account more than 2000"), 
  errorCode_INVALID_CURRENCY("078", "Invalid currency"), 
  errorcode_NEGATIVE_CREDIT_REQUEST("079", "Negative credit request"), 
  errorcode_NEGATIVE_DEBIT_REQUEST("080", "Negative debit request"), 
  errorcode_BLACKLISTED_TAG("081", "Blacklisted tag"),
  errorcode_HOTLISTED_TAG("082", "Hotlisted tag"),
  errorcode_CLOSED_OR_REPLACED_TAG("083", "Closed Or Replaced tag"),
  errorcode_STATE_IS_NOT_IN_DB("806","State is not In DB"),
  errorcode_INVALID_VIN("604","Invalid Vin"),
  errorcode_INVALID_ENGINE_NO("605","Invalid Engine No"),
  errorcode_VIN_ENGINENUMBER_CANNOT_BE_EMPTY("606","Vin/EngineNumber Cannot be Empty"),
  errorcode_REGISTERED_VEHICLE_IS_INCORRECT("702","Registered Vehicle Is Incorrect"),
  errorcode_STATE_IS_EMPTY_OR_INCORRECT("703","State is Empty or Incorrect"),
  errorcode_SAME_VIN_ENGINE_REQNO_EXIST("715", "Same VIN or EngineNumber or ReqNo already exist"),
  errorcode_VIN_IS_EMPTY("811", "VIN Is empty");


	

  
  
  //@formatter:on
  private static final Logger LOG = LogManager.getLogger(TollErrorCodes.class);

  private String apicode;
  private String message;

  private TollErrorCodes(String apicode, String message) {
    this.apicode = apicode;
    this.message = message;
  }

  public String code() {
    return this.apicode;
  }

  public String message() {
    return this.message;
  }

  static {
    Set<String> lookup = new HashSet<>();
    for (TollErrorCodes tollErrorCode : TollErrorCodes.values()) {
      if (lookup.contains(tollErrorCode.code())) {
        LOG.error(String.format("Duplicate code: %s configured. Last instance will be used",
            tollErrorCode.code()));
      } else {
        lookup.add(tollErrorCode.code());
      }
    }
  }
}
