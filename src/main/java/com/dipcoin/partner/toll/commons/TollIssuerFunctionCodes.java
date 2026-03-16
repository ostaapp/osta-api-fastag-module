package com.dipcoin.partner.toll.commons;

import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.Getter;

@Getter
public enum TollIssuerFunctionCodes {

  //@formatter:off
  SETTELED_TXN(200, "Settled Transaction","", "", "F", 1),
  DEBIT_CHARGEBACK_RAISE(450, "chargeback raise", "200,763", "40 days from 200,763", "N", 23),
  //CREDIT_CHARGEBACK_RAISE(451, "credit chargeback raise", "200,762", "40 days from 200,762", "N"),
  RE_PRESENTMENT_ACCEPTANCE(261, "Re-presentment Acceptance", "205", "10 days from 205", "N", 24),
  PRE_ARBITRATION_RAISE(471, "Pre-Arbitration Raise", "205", "10 days from 205", "N", 25),
  ARBITRATION_RAISE(479, "Arbitration Raise", "473,503", "10 days from 473,503", "N", 26),
  ARBITRATION_WITHDRAWAL(482, "Arbitration Withdrawal", "479", "10 days from 479", "N", 27),
  MEMBER_FUND_COLLECTION_ISSUER(700, "Member Fund Collection", "483", "5 days from 483", "F", 3),
  //GOOD_FAITH_ACCEPTANCE(681, "Good Faith Acceptance", "680", "30 days from 680", "F"), it will not work through file system i.e. why commented
  //GOOD_FAITH_DECLINE(682, "Good Faith Decline", "200,762", "30 days from 680", "N"),
  
  
  //Issuer compliance decision is based on Aquirer compliance decision but preRequisite code is same for both issuer and aquirer
  PRE_COMPLIANCE_RAISE(672, "Pre-Compliance Raise", "200", "160 days from 200", "N", 28),
  PRE_COMPLIANCE_ACCEPTANCE(673, "Pre-Compliance Acceptance", "672", "10 days from 672", "F", 2),
  PRE_COMPLIANCE_DECLINED(674, "Pre-Compliance Declined", "672", "10 days from 672", "N", 29),
  COMPLIANCE_RAISE(675, "Compliance Raise", "674,506", "10 days from 674,506", "N", 30),
  COMPLIANCE_ACCEPTANCE(676, "Compliance Acceptance", "675", "10 days from 675", "F", 2),
  COMPLIANCE_WITHDRAWN(678, "Compliance Withdrawn", "675", "10 days from 675", "N", 31),
  COMPLIANCE_CONTINUATION(677, "Compliance Continuation", "675", "10 days from 675", "N", 32);
  
  //@formatter:on
  private static final Logger LOG = LogManager.getLogger(TollIssuerFunctionCodes.class);


  TollIssuerFunctionCodes(int functionCode, String disputeName, String preRequisite, String tat, String financialNonFinancial, int chargeBackTransactionStatus) {
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
   
   for (TollIssuerFunctionCodes tollErrorCode : TollIssuerFunctionCodes.values()) {
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
