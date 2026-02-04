package com.dipcoin.partner.toll.commons;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

public class TollConstant {

	public static int REQUEST_TIMEOUT = 15000;
	public static final String EMPTY = "";
	public static final String VERSION = "1.0";
	public static final String VER = "1.1";
	public static final String SUCCESS_RESPONSE = "00";
	public static final String SUCCESS = "SUCCESS";
	public static final String FAILURE = "FAILURE";
	public static final String TRACEID = "NPCI";
	public static final String RESULT_ACCEPTED = "ACCEPTED";
	public static final String RESULT_DECLINE = "DECLINE";
	public static final String PAYER_REF_TYPE = "PAYER";
	public static final String APPROVAL_NUMBER = "BRON";
	public static final String TS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String MSG_DATE_FORMAT = "ddMMMyy";
	public static final String ADD_OP = "ADD";
	public static final String REMOVE_OP = "REMOVE";
	public static final String ADD_TO_NPCI = "ADD TO NPCI";
	public static final String REMOVE_FROM_NPCI = "REMOVE FROM NPCI";
	public static final String UPDATE_OP = "UPDATE";
	public static final String REQPAY_TYPE_DEBIT = "DEBIT";
	public static final String REQPAY_TYPE_CREDIT = "CREDIT";
	public static final String REQPAY_TYPE_DEBIT_ADVICE = "DEBIT_ADV";
	public static final String REQPAY_TYPE_CREDIT_ADVICE = "CREDIT_ADV";
	public static final String REQPAY_TYPE_NON_FIN = "NON_FIN";
	public static final String SUCCESS_ERR_CODE = "000";

//Toll free no
	public static final int TOLL_FREE_NO = 1033;

	public static final Long EXCCODE_UPDATE_DIFFERENCE = 1200000L; // 20 MIN in millisecond
	public static final Long TXN_TS_DIFFERENCE = 900000L; // 15 MIN in millisecond
	public static final Long NEAR_TIME = 300000L; // 5 MIN in millisecond
	public static final Long QUERY_EXCEPTION_TIME_DIFFERENCE = 144000000L; // 4 Hr in millisecond
	public static final String SIGN_AUTH_UNKNOWN = "UNKNOWN";

	public static final String EXC_CODE_ACTIVE = "00";
	public static final String EXC_CODE_HOTLIST = "01";
	public static final String EXC_CODE_EXEMTED_LIST = "02";
	public static final String EXC_CODE_LOWBALANCE_LIST = "03";
	public static final String EXC_CODE_INVALID_CARRIAGE = "04";
	public static final String EXC_CODE_BLACKLIST = "05";
	public static final String EXC_CODE_CLOSED_OR_REPLACED = "06";

	public static final String GET_EXCEPTION_LIST_NOTE = "getExceptionLisT";
	public static final String GET_EXCEPTION_LIST_TXN_TYPE = "FETCHEXCEPTION";

	public static final String QUERY_EXCEPTION_LIST_NOTE = "queryExceptionList";
	public static final String QUERY_EXCEPTION_LIST_TXN_TYPE = "Query";

	public static final String TID = "TID";
	public static final String TAGID = "TAGID";
	public static final String ISSUEDATE = "ISSUEDATE";
	public static final String EXCCODE = "EXCCODE";
	public static final String VEHICLECLASS = "VEHICLECLASS";
	public static final String REGNUMBER = "REGNUMBER";
	public static final String COMVEHICLE = "COMVEHICLE";
	public static final String TAG_ISSUE_DATE_FORMAT = "dd-MM-yyyy";
	public static final String TAG_STATUS = "TAGSTATUS";

	public static final String TAG_VERIFICATION_DATE_FORMAT = "yyyy-MM-dd";

	public static final String REGISTERED_VEHICLE = "REGISTERED_VEHICLE";
	public static final String STATE = "STATE";
	// public static final List<String> TAGDESCRIPTOR =
	// Arrays.asList("NEW_TAG","REPLACEMEN_TAG");
	public static final String TAGDESCRIPTOR = "TAGDESCRIPTOR";
	public static final String VIN = "VIN";
	public static final String ENGINENO = "ENGINENO";
	// public static final List<String> VEHICLEDESCRIPTOR = Arrays.asList("PETROL",
	// "DIESEL","ELECTRIC","HYBRID","CNG");
	public static final String VEHICLEDESCRIPTOR = "VEHICLEDESCRIPTOR";
	public static final String NATIONALPERMIT = "NATIONALPERMIT";
	public static final String PERMITEXPIRYDATE = "PERMITEXPIRYDATE";

	public static final String MANAGE_ADD_TYPE = "A1";
	public static final String MANAGE_ADD_LIST_NOTE = "addTag";
	public static final String MANAGE_TXN_TYPE = "ManageTag";

	public static final String REQUEST_DETAILS_NOTE = "VehicleVerification";
	public static final String REQUEST_DETAILS_TYPE = "FETCH";

	public static final String MANAGE_EXCEPTION_NOTE = "updateException";
	public static final String MANAGE_EXCEPTION_TYPE = "ManageException";

	public static final String FORCE_CLOSE_TYPE = "ForcedClose";

	public static final List<String> states = Arrays.asList("Andaman and Nicobar Islands", "Andhra Pradesh",
			"Arunachal Pradesh", "Assam", "Bihar", "Chandigarh", "Chhattisgarh", "Dadra and Nagar Haveli",
			"Daman and Diu", "Delhi", "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jammu and Kashmir", "Jharkhand",
			"Karnataka", "Kerala", "Lakshadweep", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
			"Nagaland", "Odisha", "Puducherry", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
			"Uttar Pradesh", "Uttarakhand", "West Bengal");

	public static final List<String> tollRegistrationIdProofs = Arrays.asList("Aadhar Card", "Passport",
			"Voter ID Card", "Driving License", "Passbook", "Others");

	public static final List<String> vehicleTypes = Arrays.asList("Commercial", "Individual");

	public static final List<String> VEHICLE_AVC = Arrays.asList("VC1", "VC2", "VC3", "VC4", "VC5", "VC6", "VC7", "VC8",
			"VC9", "VC10", "VC11", "VC12", "VC13", "VC14", "VC15", "VC16", "VC17", "VC18", "VC19", "VC20");

	public static final String INR_CURRENCY = "INR";
	public static final String BANK_ID = "BANKID";
	public static final String NPCI_ADDRESS = ".iin.npci";

	public static final String VEHICLE_CLASS_6 = "VC6";
	public static final String DUMMY_REGISTRATION_NO = "XX00XX0000";
	public static final String UNREGISTERED_TAG_REGISTRATION_NO = "OS00TA0000";

	public static final String BULK_DELETE_FASTAG_STATUS_SHEET_HEADERS[] = { "DeletedTagIds", "NotDeletedTagIds" };
	public static final String BULK_DELETE_FASTAG_STATUS_SHEET = "BulkDeleteFastagStatus";

	public static final String BULK_HOTLIST_FASTAG_STATUS_SHEET_HEADERS[] = { "HotlistedTagIds", "NotHotlistedTagIds" };
	public static final String BULK_HOTLIST_FASTAG_STATUS_SHEET = "BulkHotlistFastagStatus";

	public static final String XLSXEXTENSION = ".xlsx";

	public static final String TOLLTAG_HAND_DELIVERY = "HAND-DELIVERY";

	public enum RegistrationType {
		// @formatter:off
        DEFAULT(0), // Osta type
        IHMCL(1), 
	  	WALLET(2);
        // @formatter:on

		private final int type;

		private RegistrationType(int type) {
			this.type = type;
		}

		public int value() {
			return this.type;
		}

		public boolean equals(int type) {
			return this.type == type;
		}
	}

	public enum TransactionInitiator {
	// @formatter:off
    OSTA("osta"), // Osta type
    NPCI("npci"); 
    // @formatter:on

		private final String type;

		private TransactionInitiator(String type) {
			this.type = type;
		}

		public String value() {
			return this.type;
		}

		public boolean equals(String type) {
			return this.type == type;
		}
	}

	public static String getRandomVehicleNumber() {
		return "XX" + RandomStringUtils.randomNumeric(2) + RandomStringUtils.randomAlphabetic(2).toUpperCase()
				+ RandomStringUtils.randomNumeric(4, 6);
	}

	public enum EpcOperation {
		EPC_TAG_SCRAPPED(1);

		private int operation;

		private EpcOperation(int operation) {
			this.operation = operation;
		}

		public int value() {
			return this.operation;
		}

		public boolean equals(int operation) {
			return this.operation == operation;
		}

	}

	public enum NETCResponseType {

		ADVICE(01, "ADVICE"), NOTIFICATION(02, "NOTIFICATION"), DECLINE(03, "DECLINE_TAG"),
		FORCE_CLOSE(04, "ForcedClose");

		private final int code;
		private String type;

		private NETCResponseType(int code, String type) {
			this.code = code;
			this.type = type;
		}

		public int code() {
			return this.code;
		}

		public String type() {
			return this.type;
		}

		public boolean equals(String type) {
			return this.type.equals(type);
		}

	}

}
