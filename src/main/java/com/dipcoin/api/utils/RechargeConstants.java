package com.dipcoin.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;


public class RechargeConstants {
	public static String HEADER_TRACEID = "X-TraceId";
	public static String CURRENCY = "INR";
	public static String MOBILE = "MOBILE";
	public static String JIO = "JIO";
	public static String BILLDATEFORMAT = "yyyy-MM-dd";
	public static String DUEDATEFORMAT = "yyyy-MM-dd";

	public static String TWO_ZERO = "00"; // for making dynamic Biller Id for Epay MR
	public static String THREE_ZERO = "000"; // for making dynamic Biller Id for Epay MR
	public static String ZERO_ONE = "01"; // for making dynamic Biller Id for Epay MR
	public static String MR = "MR"; // for making dynamic Biller Id for Epay MR
	public static String CONSUMER_NUMBER = "Consumer Number";
	public static String APP_NAME = "Osta|";

	public static enum SupportedHttpMethod {
		POST, GET;
	}

	public enum RechargeResponseStatus {
		//@formatter:off
	    SUCCESS("00", "Transaction Successful"), 
	    BAD_REQUEST("01", "Bad Request"), 
	    TIME_OUT("02", "Time Out"),
	    INTERNAL_ERROR("03", "Internal Error"),
	    INVALID_AMOUNT("AI","Sorry, Invalid amount, Please try doing a recharge"),
	    AMOUNT_BARRED("AB","Amount temporarily barred for Recharge."),
	    PENDING("UP","Transaction Under Process."),
	    TRANSACTION_STATUS_UNKNOWN("US", "Transaction Status Unknown"),
	    OPERATOR_SYSTEM_GENERAL_ERROR("EG", "Operator System General Error"),
	    OPERATOR_SYSTEM_INTERNAL_ERROR("EI", "Operator System Internal Error"),
	    MERCHANT_EXCEEDED_AVAILABLE_BALANCE("EBO", "Merchant Exceeded Available Balance"),
	    DUPLICATE_TRANSACTION_ID("EDT", "Duplicate Transaction ID received from Merchant");
	    //@formatter:on

		private static Map<String, RechargeResponseStatus> lookup = new HashMap<>();
		static {
			for (RechargeResponseStatus status : values()) {
				lookup.put(status.code(), status);
			}
		}
		private String code;
		private String desc;

		private RechargeResponseStatus(String code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public String code() {
			return this.code;
		}

		public String description() {
			return this.desc;
		}

		public static boolean validCode(String code) {
			return lookup.keySet().contains(code);
		}

		public static RechargeResponseStatus resolve(String code) {
			return lookup.get(code);
		}
	}

	public enum RechargeReconFileStatus {
		//@formatter:off
	    SUCCESS("00"), 
	    FAILED("EG");
	    //@formatter:on

		private String value;

		private RechargeReconFileStatus(String value) {
			this.value = value;
		}

		public String value() {
			return value;
		}
	}

	public enum RechargeStatus {
		//@formatter:off
	    DEFAULT(-1),
	    SUCCESS(0), 
	    FAILED(1),
	    PENDING(2);
	    //@formatter:on

		private Integer value;

		private RechargeStatus(Integer value) {
			this.value = value;
		}

		public Integer value() {
			return value;
		}

		public static String nameOf(Integer value) {
			for (RechargeStatus rechargeStatus : values()) {
				if (value != null && rechargeStatus.value() == value) {
					return rechargeStatus.name();
				}
			}
			return null;
		}
	}

	public enum ChannelCode {

		INTERNET("INT"), INTERNET_BANKING("INTB"), MOBILE("MOB"), MOBILE_BANKING("MOBB"), POINT_OF_SALE("POS"),
		MOBILE_POINT_OF_SALE("MOPS"), AUTOMATED_TELLER_MACHINE("ATM"), KIOSK("KIOSK"), BANK_BRANCH("BNKBRNCH"),
		AGENT("AGT"), BUSINESS_CORRESPONDENT("BSC"), INTERNET_RETAIL("INR");

		private String channelCode;

		private ChannelCode(String channelCode) {
			this.channelCode = channelCode;
		}

		public String value() {
			return this.channelCode;
		}

		public boolean equals(String channelCode) {
			return this.channelCode.equalsIgnoreCase(channelCode);
		}

		public static String nameOf(String value) {
			for (ChannelCode channelCode : values()) {
				if (StringUtils.isNotEmpty(value) && channelCode.value().equalsIgnoreCase(value.replaceAll(" ", ""))) {
					return channelCode.name();
				}
			}
			return null;
		}
	}

	public enum PaymentMode {

		CASH("Cash"), INTERNET_BANKING("InternetBanking"), DEBIT_CARD("DebitCard"), CREDIT_CARD("CreditCard"),
		PREPAID_CARD("PrepaidCard"), IMPS("IMPS"), UPI("UPI"), WALLET("Wallet"), NEFT("NEFT"),
		AADHAAR_ENABLED_PAYMENT_SYSTEM("AEPS"), ACCOUNT_TRANSFER("AccountTransfer"), BHARAT_QR("BharatQR"),
		USSD("USSD");

		private String paymentMode;

		private PaymentMode(String paymentMode) {
			this.paymentMode = paymentMode;
		}

		public String value() {
			return this.paymentMode;
		}

		public boolean equals(String paymentMode) {
			if (StringUtils.isNotEmpty(paymentMode)) {
				return this.paymentMode.equalsIgnoreCase(paymentMode.replaceAll(" ", ""));
			}
			return false;
		}

		public static String nameOf(String value) {
			for (PaymentMode paymentMode : values()) {
				if (StringUtils.isNotEmpty(value) && paymentMode.value().equalsIgnoreCase(value.replaceAll(" ", ""))) {
					return paymentMode.name();
				}
			}
			return null;
		}

		public static String stringValue(String paymentMode) {
			if (StringUtils.isNotEmpty(paymentMode)) {
				String rawValue = Arrays.toString(paymentMode.split("(?=[A-Z])"));
				String finalValue = rawValue.replace("[", "").replaceAll(",", "").replace("]", "");
				return finalValue;
			}
			return null;
		}
	}

	public enum Mode {

		NOT_SUPPORTED("0"), MANDATORY("1"), OPTIONAL("2");

		private String mode;

		private Mode(String mode) {
			this.mode = mode;
		}

		public String value() {
			return this.mode;
		}

		public boolean equals(String mode) {
			return this.mode.equals(mode.toUpperCase());
		}

		public static String getMode(String mode) {
			if (NOT_SUPPORTED.toString().equals(mode))
				return NOT_SUPPORTED.value();
			else if (MANDATORY.toString().equals(mode))
				return MANDATORY.value();
			else
				return OPTIONAL.value();
		}
	}

	public enum Source {

		EURONET("EN"), BHARAT_BILL_PAYMENT_SYSTEM("BBPS");

		private String source;

		private Source(String source) {
			this.source = source;
		}

		public String value() {
			return this.source;
		}

		public boolean equals(String source) {
			return this.source.equals(source.toUpperCase());
		}
	}

	public enum ComplaintType {

		TRANSACTION("Transaction"), SERVICE("Service");

		private String complaintType;

		private ComplaintType(String complaintType) {
			this.complaintType = complaintType;
		}

		public String value() {
			return this.complaintType;
		}

		public boolean equals(String complaintType) {
			return this.complaintType.equals(complaintType.toUpperCase());
		}
	}

	public enum ParticipationType {

		AGENT("AGENT"), BILLER("BILLER");

		private String participationType;

		private ParticipationType(String participationType) {
			this.participationType = participationType;
		}

		public String value() {
			return this.participationType;
		}

		public boolean equals(String participationType) {
			return this.participationType.equals(participationType.toUpperCase());
		}
	}

	public enum RequestType {

		VALIDATE(0), SERVICE(1), BILL_VALIDATION(2), STATUS(3), COMPLAINT(4), COMPLAINT_STATUS(5);

		private Integer requestType;

		private RequestType(Integer requestType) {
			this.requestType = requestType;
		}

		public int value() {
			return this.requestType;
		}

		public boolean equals(Integer requestType) {
			return this.requestType == requestType;
		}
	}

	public enum BillPaymentStatus {

		SUCCESS("SUCCESS"), SUCCESSFUL("SUCCESSFUL"), FAILURE("FAILURE"), FAILED("FAILED"), PENDING("PENDING");

		private String billPaymentStatus;

		private BillPaymentStatus(String billPaymentStatus) {
			this.billPaymentStatus = billPaymentStatus;
		}

		public String value() {
			return this.billPaymentStatus;
		}

		public boolean equals(String billPaymentStatus) {
			return this.billPaymentStatus.equalsIgnoreCase(billPaymentStatus);
		}
	}

	public enum BillerStatus {

		ACTIVE("ACTIVE"), PENDING_DELETE("PENDING DELETE"), PENDING("PENDING"), INACTIVE("INACTIVE"),
		DELETED("DELETED");

		private String billStatus;

		private BillerStatus(String billStatus) {
			this.billStatus = billStatus;
		}

		public String value() {
			return this.billStatus;
		}

		public boolean equals(String billStatus) {
			return this.billStatus.equalsIgnoreCase(billStatus);
		}
	}

	// added epay 3.0
	public enum RechargeSuccessResponseCode {
		//@formatter:off
		    SUCCESS("00", "Transaction Successful"), 
		    DEEMED_SUCCESS("01", "Deemed Success");
		    //@formatter:on

		private static Map<String, RechargeSuccessResponseCode> lookup = new HashMap<>();
		static {
			for (RechargeSuccessResponseCode status : values()) {
				lookup.put(status.code(), status);
			}
		}
		private String code;
		private String desc;

		private RechargeSuccessResponseCode(String code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public String code() {
			return this.code;
		}

		public String description() {
			return this.desc;
		}

		public static boolean validCode(String code) {
			return lookup.keySet().contains(code);
		}

		public static RechargeSuccessResponseCode resolve(String code) {
			return lookup.get(code);
		}

		public static List<String> getAllResponseCode() {
			return new ArrayList<>(lookup.keySet());
		}

	}

	public enum RechargePendingResponseCode {
		//@formatter:off
		    TRANSACTION_UNDER_PROCESS("UP", "Transaction Under Process"), 
		    TRANSACTION_STATUS_UNKNOWN("US", "Transaction Status is Unknown"),
		    ACKNOWLEDGEMENT_SUCCESSFUL("AS", "Acknowledgement Successful"),
		    TIMEOUT_ERROR_FROM_OPERATOR("TEO", "Timeout error from Operator"),
		    OPERATOR_SYSTEM_GENERAL_ERROR("EG", "Operator System General Error"),
		    OPERTOR_SYSTEM_INTERNAL_ERROR("EI", "Operator System Internal Error"),
		    OPERATOR_SYSTEM_NOT_AVAILABLE("ES", "Operator System Not Available, please try after some time");

		    //@formatter:on

		private static Map<String, RechargePendingResponseCode> lookup = new HashMap<>();
		static {
			for (RechargePendingResponseCode status : values()) {
				lookup.put(status.code(), status);
			}
		}
		private String code;
		private String desc;

		private RechargePendingResponseCode(String code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public String code() {
			return this.code;
		}

		public String description() {
			return this.desc;
		}

		public static boolean validCode(String code) {
			return lookup.keySet().contains(code);
		}

		public static RechargePendingResponseCode resolve(String code) {
			return lookup.get(code);
		}

		public static List<String> getAllResponseCode() {
			return new ArrayList<>(lookup.keySet());
		}

	}

	public enum RechargeFailureResponseCode {
		//@formatter:off
		    MOBILE_NO_BARRED("CB", "Mobile No temporarily barred from Refill, please contact Operator"),
		    INVALID_MOBILE_NUMBER("CI", "Invalid Mobile Number"),
		    INVALID_MOBILE_NUM("ECI", "Invalid Mobile Number"),
		    CHANGE_PASSWORD("ECP", "Please change the password"),
		    DUPLICATE_TRANSACTION_ID("EDT","Duplicate Transaction ID received from Merchant"),
		    INVALID_CHANNEL("EIC","Invalid Channel sent by Merchant"),
		    EURONET_INTERNAL_ERROR("EIE","Euronet Internal Error"),
		    INVALID_MERCHANT_AMOUNT("EIM", "Invalid Merchant Account sent by Merchant"),
		    INVALID_PAYMENT_MODE("EIP", "Invalid Payment Mode sent by Merchant"),
		    INVALID_MERCHANT_USER("EIU", "Invalid User Name sent by Merchant"),
		    INVALID_MESSAGE_SENT("EMI", "Invalid Message sent by Merchant"),
		    SERVICE_NOT_AVAILABLE_FOR_MERCHANT("ESM", "Service provider or Service not available for Merchant"),
		    SERVICE_PROVIDER_IS_NOT_ACTIVE("ESP", "Invalid Service Provider or Service Provider is not active"),
		    SUB_SERVICE_IS_NOT_ACTIVE("ESS", "Invalid Sub Service Provider or Sub Service Provider is not active"),
		    INVALID_MERCHANT("E18", "Invalid Merchant"),
		    INVALID_SERVICE_PROVIDER("E50", "Invalid Service Provider, please select a valid Telecom Operator"),
		    NO_BILL_DATA_AVAILABLE("NB", "No bill data available"),
		    DUE_DATA_EXCEEDED("DE", "Due date exceeded"),
		    CANNOT_PROCEED_FOR_PAYMENT("BP", "Cannot Proceed for Payment. Successful Online Payment available"),
		    BILL_ALREADY_PAID_FOR_THIS_ACCOUNT("BIP", "Bill already paid for this account"),
		    INCORRECT_CUSTOMER_ACCOUNT("ICA", "Incorrect / invalid customer account"),
		    UNSUPPORTED_TAG_PRESENT("UTP", "Unsupported Tag present"),
		    CUSTOMER_TAG_MOBILE_NUMBER_SHOULD_BE_NUMBERIC("CMN", "Customer tag mobile number should be numeric"),
		    YOU_CANT_DO_TRANSACTION_WITH_SAME_NUMBER_WITH_SAME_AMOUNT_WITHIN_15MINUTES_TRY_AFTER_15MINS("TST", "You cannot do transaction with same number with same amount within 15 minutes. Try after 15 mins"),
		    INVALID_IP("INP", "Invalid IP"),
		    INVALID_INPUT_PARAMETER("IVP", "Invalid Input parameter"),
		    AUTHENTICATION_ERROR("AUE", "Authentication Error"),
		    HASH_KEY_MISMATCH("HKM", "Hash Key Mismatch"),
		    INVALID_INPUT_INPUT_PARAMETERS_MISSING("IPM", "Invalid Input. Input Parameters Missing"),
		    CUSTOMER_SUBSCRIPTION_LIMIT_REACHED("998", "Customer Subscription Limit Reached "),
		    MERCHANT_EXCEEDED_AVAILABLE_BALANCE("EBO", "Merchant Exceeded Available Balance");

		    //@formatter:on

		private static Map<String, RechargeFailureResponseCode> lookup = new HashMap<>();
		static {
			for (RechargeFailureResponseCode status : values()) {
				lookup.put(status.code(), status);
			}
		}
		private String code;
		private String desc;

		private RechargeFailureResponseCode(String code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public String code() {
			return this.code;
		}

		public String description() {
			return this.desc;
		}

		public static boolean validCode(String code) {
			return lookup.keySet().contains(code);
		}

		public static RechargeFailureResponseCode resolve(String code) {
			return lookup.get(code);
		}

		public static List<String> getAllResponseCode() {
			return new ArrayList<>(lookup.keySet());
		}

	}

	public enum Operators {
		//@formatter:off
		    VOD("VOD", "Vi","VIL000000NAT01"),
		    ART("ART","Airtel","ART00MR000MR01"),
		    BSP("BSP","BSNL",""),   // will set dynamically based on Circle
		    MTL("MTL","MTNL",""),  // will set dynamically based on Circle 
		    JIO("JIO","Jio","JIO00MR00NAT01");

		    //@formatter:on

		private static Map<String, Operators> lookup = new HashMap<>();
		static {
			for (Operators operator : values()) {
				lookup.put(operator.serviceProviderCode(), operator);
			}
		}
		private String operator;
		private String serviceProviderCode;
		private String billerId;

		private Operators(String serviceProviderCode, String operator, String billerId) {
			this.operator = operator;
			this.serviceProviderCode = serviceProviderCode;
			this.billerId = billerId;
		}

		public String operator() {
			return this.operator;
		}

		public String serviceProviderCode() {
			return this.serviceProviderCode;
		}

		public String billerId() {
			return this.billerId;
		}

		private Operators opr;

		public static boolean validOperator(String serviceProviderCode) {
			return lookup.keySet().contains(serviceProviderCode);
		}

		public static Operators resolve(String serviceProviderCode) {
			return lookup.get(serviceProviderCode);
		}
	}

	public enum Circle {
		//@formatter:off
		    AP("Andhra Pradesh","AP"),
		    AS("Assam","AS"),
		    BH("Bihar","BH"),
		    CH("Chennai","CH"),
		    DL("Delhi","DL"),
		    GJ("Gujarat","GJ"),
		    HP("Himachal Pradesh","HP"),
		    HR("Haryana","HR"),
		    JK("Jammu and Kashmir","JK"),
		    KK("Karnataka","KK"),
		    KO("Kolkata","KO"),
		    KR("Kerala","KR"),
		    MH("Maharashtra","MH"),
		    MP("Madhya Pradesh","MP"),
		    MU("Mumbai","MU"),
		    NE("North East","NE"),
		    OR("Orissa","OR"),
		    PB("Punjab","PB"),
		    RJ("Rajasthan","RJ"),
		    TN("Tamil Nadu","TN"),
		    UE("UP East","UE"),
		    UW("UP West","UW"),
		    WB("West Bengal","WB");

		    //@formatter:on

		private static Map<String, Circle> lookup = new HashMap<>();
		static {
			for (Circle circle : values()) {
				lookup.put(circle.circle(), circle);
			}
		}
		private String circleCode;
		private String circle;

		private Circle(String circle, String circleCode) {
			this.circle = circle;
			this.circleCode = circleCode;
		}

		public String circleCode() {
			return this.circleCode;
		}

		public String circle() {
			return this.circle;
		}

		public static boolean validCircle(String circle) {
			return lookup.keySet().contains(circle);
		}

		public static Circle resolve(String circle) {
			return lookup.get(circle);
		}

	}

	public enum RechargeReconFileResponseCode {
		//@formatter:off
		    SUCCESS("00", "Transaction Successful"),
		    ALSO_SUCCESS("0", "Transaction Successful");

			private static Map<String, RechargeReconFileResponseCode> lookup = new HashMap<>();
			static {
				for (RechargeReconFileResponseCode status : values()) {
					lookup.put(status.code(), status);
				}
			}
			private String code;
			private String desc;

			private RechargeReconFileResponseCode(String code, String desc) {
				this.code = code;
				this.desc = desc;
			}

			public String code() {
				return this.code;
			}

			public String description() {
				return this.desc;
			}

			public static boolean validCode(String code) {
				return lookup.keySet().contains(code);
			}

			public static RechargeReconFileResponseCode resolve(String code) {
				return lookup.get(code);
			}

			public static List<String> getAllResponseCode() {
				return new ArrayList<>(lookup.keySet());
			}

		}
		
		
		public enum ReconFileStatuses {
		    //@formatter:off
		    SUCCESS(0), 
		    FAILED(1);
		    //@formatter:on

		private Integer value;

		private ReconFileStatuses(Integer value) {
			this.value = value;
		}

		public Integer value() {
			return value;
		}
	}

	public enum MerchantTxnType {

		PRIMEFLIX(1);

		private Integer merchantTxnType;

		private MerchantTxnType(Integer merchantTxnType) {
			this.merchantTxnType = merchantTxnType;
		}

		public int value() {
			return this.merchantTxnType;
		}

		public boolean equals(Integer merchantTxnType) {
			return this.merchantTxnType == merchantTxnType;
		}
	}

	public enum RechargeErrorResponse {
		NO("No"), YES("Yes");

		private String errorResponse;

		private RechargeErrorResponse(String errorResponse) {
			this.errorResponse = errorResponse;
		}

		public String value() {
			return this.errorResponse;
		}

		public boolean equals(String errorResponse) {
			return this.errorResponse.equalsIgnoreCase(errorResponse);
		}
	}

}
