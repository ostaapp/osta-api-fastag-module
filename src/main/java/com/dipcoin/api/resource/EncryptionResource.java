package com.dipcoin.api.resource;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.api.commons.APIException;
import com.dipcoin.db.services.model.User;

@Component("encryptionResource")
@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public class EncryptionResource {

	public String encrypt(User user, Object object, Object object2, String token) {
		// TODO Auto-generated method stub
		return null;
	}

	public String decrypt(User user, Object object, Object object2, String encDcoin) {
		// TODO Auto-generated method stub
		return null;
	}

}
