package com.dipcoin.partner.toll.commons;

import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.Getter;

@Getter
public enum TollAquirerFunctionCodes {
  
  //@formatter:off
  SETTELED_TXN(200, "Settled Transaction","", "", "F", 1),
  DEBIT_ADJUSTMENT(763,"Debit Adjustment", "200","5 days from 200","F", 2),
  CREDIT_ADJUSTMENT(762,"Credit Adjustment", "200","30 days from 200","F", 3),
  CREDIT_CHARGEBACK_ACCEPTANCE(452, "Credit chargeback Acceptance", "451", "10 days from 451", "F", 2),
  CREDIT_CHARGEBACK_DEEMED_ACCEPTANCE(502, "Credit chargeback deemed Acceptance", "451", "10 days from 451", "F", 2),
  DEBIT_CHARGEBACK_ACCEPTANCE(470, "Debit Chargeback Acceptance", "450", "10 days from 450", "F", 3),
  DEBIT_CHARGEBACK_DEEMED_ACCEPTANCE(500, "Debit chargeback deemed Acceptance", "450", "10 days from 450", "F", 3),
  RE_PRESENTMENT_RAISE(205, "Re-presentment Raise", "450", "10 days from 450", "N", 8),
  RE_PRESENTMENT_DEEMED_ACCEPTANCE(501, "Re-presentment deemed Acceptance", "205", "10 days from 205", "N",9),
  PRE_ARBITRATION_ACCEPTANCE(474, "Pre-Arbitration Acceptance", "471", "10 days from 471", "F", 3),
  PRE_ARBITRATION_DECLINE(473, "Pre-Arbitration Declined", "471", "10 days from 471", "N", 10),
  PRE_ARBITRATION_DEEMED_DECLINED(503, "Pre-Arbitration deemed Declined", "471", "10 days from 471", "N", 11),
  ARBITRATION_ACCEPTANCE(480, "Arbitration Acceptance", "479", "10 days from 479", "F", 3),
  ARBITRATION_CONTINUATION(481, "Arbitration Continuation", "479", "10 days from 479", "N", 12),
  ARBITRATION_DEEMED_CONTINUATION(504, "Arbitration deemed Continuation", "479", "10 days from 479", "N", 13),
  ARBITRATION_VERDICT(483, "Arbitration Verdict", "481,504", "10 days from 481,504", "NA", 14),
  MEMBER_FUND_COLLECTION_ACQUIRER(700, "Member Fund Collection", "483", "5 days from 483", "F", 2),
  // Aquirer compliance decision is based on issuer compliance decision but preRequisite code is same for both issuer and aquirer
  PRE_COMPLIANCE_RAISE(672, "Pre-Compliance Raise", "200", "160 days from 200", "N", 15),
  PRE_COMPLIANCE_ACCEPTANCE(673, "Pre-Compliance Acceptance", "672", "10 days from 672", "F", 3),
  PRE_COMPLIANCE_DECLINED(674, "Pre-Compliance Declined", "672", "10 days from 672", "N", 16),
  PRE_COMPLIANCE_DEEMED_DECLINED(506, "Pre-Compliance deemed Declined", "672", "10 days from 672", "N", 17),
  COMPLIANCE_RAISE(675, "Compliance Raise", "674,506", "10 days from 674,506", "N", 18),
  COMPLIANCE_ACCEPTANCE(676, "Compliance Acceptance", "675", "10 days from 675", "F", 3),
  COMPLIANCE_WITHDRAWN(678, "Compliance Withdrawn", "675", "10 days from 675", "N", 19),
  COMPLIANCE_CONTINUATION(677, "Compliance Continuation", "675", "10 days from 675", "N", 20),
  COMPLIANCE_DEEMED_CONTINUATION(507, "Compliance deemed Continuation", "675", "10 days from 475", "N", 21),
  COMPLIANCE_VERDICT(679, "Compliance Verdict", "677,507", "15 days from 677,507", "N", 22);
  
  //@formatter:on
  private static final Logger LOG = LogManager.getLogger(TollAquirerFunctionCodes.class);


  TollAquirerFunctionCodes(int functionCode, String disputeName, String preRequisite, String tat, String financialNonFinancial, int chargeBackTransactionStatus) {
    this.functionCode = functionCode;
    this.disputeName = disputeName;
    this.preRequisite = preRequisite;
    this.tat = tat;
    this.financialNonFinancial = financialNonFinancial;
    this.chargeBackTransactionStatus = chargeBackTransactionStatus;    
  }
 
 private int functionCode;
 private String disputeName;
 private String preRequisite;
 private String tat;//turn around time
 private String financialNonFinancial; 
 private int chargeBackTransactionStatus;
 
 public final static  Set<Integer> lookup = new HashSet<>();
 
 static {

   for (TollAquirerFunctionCodes tollErrorCode : TollAquirerFunctionCodes.values()) {
     if (lookup.contains(tollErrorCode.getFunctionCode())) {
       LOG.error(String.format("Duplicate code: %s configured. Last instance will be used",
           tollErrorCode.getFunctionCode()));
     } else {
       lookup.add(tollErrorCode.getFunctionCode());
     }
   }
 }
  
 
 public static boolean contains(Integer reason) {
   return lookup.contains(reason);
 }
  
}
