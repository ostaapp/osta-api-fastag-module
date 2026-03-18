package com.dipcoin.mock.bank.services;

import com.dipcoin.mock.bank.model.TransactionData;

public interface TransactionDAO {
	TransactionData addTransactionDetails(TransactionData paramTransactionData);

	TransactionData updateTransactionResponse(TransactionData paramTransactionData);
}
