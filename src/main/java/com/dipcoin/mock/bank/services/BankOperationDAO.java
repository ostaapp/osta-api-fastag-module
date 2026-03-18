package com.dipcoin.mock.bank.services;

import com.dipcoin.mock.bank.model.*;

public interface BankOperationDAO {

	CustomerFundTransferResponse customerFundTransfer(CustomerFundTransferRequest paramCustomerFundTransferRequest,
			TransactionData paramTransactionData);

	DeleteAccountResponse deleteAccount(DeleteAccountRequest paramDeleteAccountRequest,
			TransactionData paramTransactionData);

	RemoveLienResponse removeLien(RemoveLienRequest paramRemoveLienRequest, TransactionData paramTransactionData);

	MarkLienResponse markLien(MarkLienRequest paramMarkLienRequest, TransactionData paramTransactionData)
			throws Exception;

	AccountFundTransferResponse accountFundTransfer(AccountFundTransferRequest paramAccountFundTransferRequest,
			TransactionData paramTransactionData);

//	ValidateAccountDetailsResponse validateAccountDetails(
//			ValidateAccountDetailsRequest paramValidateAccountDetailsRequest);

}
