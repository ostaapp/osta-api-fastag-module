package com.dipcoin.partner.toll.commons;

import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.Getter;


@Getter
public enum TollReasonCodes { // charge back or compliance reason codes
  
  
  //@formatter:off 
  DEBIT_CHARGEBACK_REASON_1(3001, "NETC Toll services not availed/ Tag holder does not recognise the transaction", "450"),
  DEBIT_CHARGEBACK_REASON_2(3002, "Duplicate transaction done at Toll Plaza", "450"),
  DEBIT_CHARGEBACK_REASON_3(3003, "Vehicle was in exempted list", "450"),
  DEBIT_CHARGEBACK_REASON_4(3004, "Vehicle was in black list", "450"),
  DEBIT_CHARGEBACK_REASON_5(3005, "Vehicle was in low balance list", "450"),
  DEBIT_CHARGEBACK_REASON_6(3006, "Toll fare calculation error", "450"),
  DEBIT_CHARGEBACK_REASON_7(3007, "Vehicle class mismatch", "450"),
  DEBIT_CHARGEBACK_REASON_8(3008, "Signature not validated", "450"),
  DEBIT_CHARGEBACK_REASON_9(3009, "Wrong Debit Adjustment raised","450"),
  DEBIT_CHARGEBACK_REASON_10(3010, "Credit posted as Debit", "450"),
  DEBIT_CHARGEBACK_REASON_11(3011, "Paid by other means", "450"),
  DEBIT_CHARGEBACK_REASON_12(3012, "Fraudulent tagholder not present transaction", "450"),
  DEBIT_CHARGEBACK_REASON_13(3013, "Fraudulent multiple transaction", "450"),
  DEBIT_CHARGEBACK_REASON_14(3014, "Other Specify", "450"),
  CREDIT_CHARGEBACK_REASON_1(3015, "NETC Toll services not availed/ Tag holder does not recognise the transaction", "451"),
  CREDIT_CHARGEBACK_REASON_2(3016, "Toll fare calculation error", "451"),
  CREDIT_CHARGEBACK_REASON_3(3017, "Vehicle class mismatch", "451"),
  CREDIT_CHARGEBACK_REASON_4(3018, "Wrong Credit Adjustment raised", "451"),
  CREDIT_CHARGEBACK_REASON_5(3019, "Debit posted as Credit", "451"),
  CREDIT_CHARGEBACK_REASON_6(3020, "Paid by other means", "451"),
  CREDIT_CHARGEBACK_REASON_7(3021, "Fraudulent multiple transaction", "451"),
  CREDIT_CHARGEBACK_REASON_8(3022, "Other Specify", "451"),
  RE_PRESENTMENT_REASON_1(4001, "Supporting Documents for services availed/valid transactions","205,261,501"),
  RE_PRESENTMENT_REASON_2(4002, "Supporting Documents for multiple passing", "205,261,501"),
  RE_PRESENTMENT_REASON_3(4003, "Proof of Vehicle is not in exempted List", "205,261,501"),
  RE_PRESENTMENT_REASON_4(4004, "Proof of Vehicle is not in black list", "205,261,501"),
  RE_PRESENTMENT_REASON_5(4005, "Proof of Vehicle is not in low balance list", "205,261,501"),
  RE_PRESENTMENT_REASON_6(4006, "Proof of valid Toll Fare calculation", "205,261,501"),
  RE_PRESENTMENT_REASON_7(4007, "Proof of valid Vehicle class", "205,261,501"),
  RE_PRESENTMENT_REASON_8(4008, "Proof of successful response", "205,261,501"),
  RE_PRESENTMENT_REASON_9(4009, "Proof of successful signature validation", "205,261,501"),
  RE_PRESENTMENT_REASON_10(4010, "Other Specify", "205,261,501"),
  DEBIT_ADJUSTMENT_REASON_1(1001, "Toll fare calculation error","763"),
  DEBIT_ADJUSTMENT_REASON_2(1002, "Vehicle class mismatch","763"),
  DEBIT_ADJUSTMENT_REASON_3(1003, "Unregistered Tag in the mapper", "763"),
  DEBIT_ADJUSTMENT_REASON_4(1004, "Vehicle is not in exempted list", "763"),
  DEBIT_ADJUSTMENT_REASON_5(1005, "Tag Class is Greater than MVC", "763"),
  DEBIT_ADJUSTMENT_REASON_6(1006, "Vehicle is not in low balance list", "763"),
  DEBIT_ADJUSTMENT_REASON_7(1007, "Other Specify", "763"),
  CREDIT_ADJUSTMENT_REASON_1(2001, "Toll fare calculation error", "762"),
  CREDIT_ADJUSTMENT_REASON_2(2002, "Duplicate transaction done at Toll Plaza", "762"),
  CREDIT_ADJUSTMENT_REASON_3(2003, "Tag holder was charged for unsuccessful transaction", "762"),
  CREDIT_ADJUSTMENT_REASON_5(2005, "Paid by other means", "762"),
  CREDIT_ADJUSTMENT_REASON_6(2006, "Vehicle is in exempted list", "762"),
  CREDIT_ADJUSTMENT_REASON_7(2007, "Other Specify", "762"),
  PRE_COMPLIANCE_AND_COMPLIANCE_1(5001, "Tag is not NETC Tag", "672,673,674,675,676,677,678,506,507"),
  PRE_COMPLIANCE_AND_COMPLIANCE_2(5002, "Tag is not as per the EPC Guidelines", "672,673,674,675,676,677,678,506,507"),
  PRE_COMPLIANCE_AND_COMPLIANCE_3(5003, "Tag Vendor not certified by NPCI", "672,673,674,675,676,677,678,506,507"),
  PRE_COMPLIANCE_AND_COMPLIANCE_4(5004, "Other Specify", "672,673,674,675,676,677,678,506,507");
  
  //@formatter:on
  private static final Logger LOG = LogManager.getLogger(TollIssuerFunctionCodes.class);


  TollReasonCodes(int reasonCode, String description, String functionCodes) {
    this.reasonCode = reasonCode;
    this.description = description;
    this.functionCodes = functionCodes;
   
  }
 
 private int reasonCode;
 private String description;
 private String functionCodes;
 
 public final static  Set<Integer> lookup = new HashSet<>();
 
 static {
  
   for (TollReasonCodes tollErrorCode : TollReasonCodes.values()) {
     if (lookup.contains(tollErrorCode.getReasonCode())) {
       LOG.error(String.format("Duplicate code: %s configured. Last instance will be used",
           tollErrorCode.getReasonCode()));
     } else {
       lookup.add(tollErrorCode.getReasonCode());
     }
   }
 }
 
 
 public static boolean contains(Integer reason) {
   return lookup.contains(reason);
 }

}
