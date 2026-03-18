package com.dipcoin.mock.bank.model;

import com.dipcoin.mock.bank.model.MockBankResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeleteAccountResponse extends MockBankResponse {}
