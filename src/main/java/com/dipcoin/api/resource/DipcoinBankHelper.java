package com.dipcoin.api.resource;

import org.springframework.http.ResponseEntity;

import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.User;

public class DipcoinBankHelper {

	public ResponseEntity markLien(User user, CustomerAccount originDcoinCustomerAccount, Dipcoin lienMarkedDcoin,
			BankTransaction lienMarkedBTx, BankRequestContext bankRequestContext) {
		// TODO Auto-generated method stub
		return null;
	}

}
