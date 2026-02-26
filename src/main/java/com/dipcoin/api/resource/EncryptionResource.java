package com.dipcoin.api.resource;

import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.EncryptionResponse;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.LogFormatter.Mask;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.core.CryptoUtil.AlgoScheme;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component("encryptionResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class EncryptionResource {

	private static final Logger LOG = LogManager.getLogger(EncryptionResource.class);

	public static final AlgoScheme DefaultAlgoScheme = AlgoScheme.AES_CBC_PKCS5PADDING;

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private CryptoUtil cryptoUtil;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	public void setHttpServletContext(HttpServletContext httpServletContext) {
		this.httpServletContext = httpServletContext;
	}

	/*
	 * 
	 */
	public ResponseEntity getEncryptionKey(final User user, final Merchant merchant, final Bank bank,
			boolean forceUpdate) throws Exception {

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.SC_BAD_REQUEST).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("UserId: " + user.getId()).format());
		if (merchant != null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("MerchantId: " + merchant.getId())
					.format());
		}
		if (bank != null) {
			LOG.debug(
					LogFormatter.instance(httpServletContext.getTraceId()).message("BankId: " + bank.getId()).format());
		}

		// if merchant/bank is not active and user is not admin
		if (merchant != null && !this.userDBService.isActive(merchant)
				&& !(this.userDBService.isMerchantAdmin(user) || this.userDBService.isMerchantSuperAdmin(user))) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("MerchantId", merchant.getId())
					.data("Status", merchant.getStatus()).format());
			return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED)
					.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));

		} else if (bank != null && !this.userDBService.isActive(bank)
				&& !(this.userDBService.isBankAdmin(user) || this.userDBService.isBankSuperAdmin(user))) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankId", bank.getId())
					.data("Status", bank.getStatus()).format());
			return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED)
					.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));

		}

		EncryptionResponse response = new EncryptionResponse();
		// check existing encryption
		if (!forceUpdate && !isEncryptionExpired(httpServletContext.getTraceId(), user, merchant, bank)) {
			populateEncryptionInfo(httpServletContext.getTraceId(), user, response);
			return ResponseEntity.ok(response);
		}

		// create key for 10 min
		if (initUserEncryption(user, 10)) {
			User loggedUser = this.userDBService.updateUser(user);
			if (loggedUser != null) {
				populateEncryptionInfo(httpServletContext.getTraceId(), loggedUser, response);
				return ResponseEntity.ok(response);
			}
		}

		return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
				.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
	}

	public ResponseEntity expireEncryption(final User user, final Merchant merchant, final Bank bank) throws Exception {

		if (!this.userDBService.isActive(user)) {

			return ResponseEntity.status(HttpStatus.SC_BAD_REQUEST).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("UserId: " + user.getId()).format());
		if (merchant != null)
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("MerchantId: " + merchant.getId())
					.format());
		if (bank != null)
			LOG.debug(
					LogFormatter.instance(httpServletContext.getTraceId()).message("BankId: " + bank.getId()).format());

		// if merchant/bank is not active and user is not admin
		if (merchant != null && !this.userDBService.isActive(merchant)
				&& !(this.userDBService.isMerchantAdmin(user) || this.userDBService.isMerchantSuperAdmin(user))) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("MerchantId", merchant.getId())
					.data("Status", merchant.getStatus()).format());
			return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED)
					.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));

		} else if (bank != null && !this.userDBService.isActive(bank)
				&& !(this.userDBService.isBankAdmin(user) || this.userDBService.isBankSuperAdmin(user))) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankId", bank.getId())
					.data("Status", bank.getStatus()).format());
			return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED)
					.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));

		}

		// set expiry time to now
		user.setEncryptionKeyExpiryTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
		User loggedUser = this.userDBService.updateUser(user);
		if (loggedUser != null) {
			return ResponseEntity.status(HttpStatus.SC_OK).build();
		}

		return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
				.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
	}

	private static boolean isEncryptionExpired(String traceId, final User user, final Merchant merchant,
			final Bank bank) {
		LOG.debug(LogFormatter.instance(traceId).message("Encryption Time")
				.data("now", DateTime.now(DateTimeZone.UTC).getMillis())
				.data("User Enc Time", user.getEncryptionKeyExpiryTime()).format());

		return (user.getEncryptionKeyExpiryTime() != null && user.getEncryptionKey() != null
				&& user.getEncryptionPadding() != null
				&& new DateTime(Long.parseLong(user.getEncryptionKeyExpiryTime())).isBeforeNow());
	}

	public boolean initUserEncryption(final User user, Integer ttlInMin) throws Exception {
		if (cryptoUtil != null && user != null) {
			CryptoUtil.AlgoScheme algoScheme = CryptoUtil.AlgoScheme.AES_CBC_PKCS5PADDING;
			String key = cryptoUtil.base64RandomSecretKey(algoScheme);

			user.setEncryptionKey(key);
			user.setEncryptionAlgo(algoScheme.algo());
			user.setEncryptionPadding(algoScheme.scheme());

			String expiryTime = String.valueOf(DateTime.now(DateTimeZone.UTC).plusMinutes(ttlInMin).getMillis());
			user.setEncryptionKeyExpiryTime(expiryTime);

			return true;
		}

		return false;
	}

	public String decrypt(final User user, final Merchant merchant, final Bank bank, String encryptedData)
			throws APIException {
		try {
			String algo = user.getEncryptionAlgo();
			String padding = user.getEncryptionPadding();
			String key = user.getEncryptionKey();
			if (isEncryptionExpired(httpServletContext.getTraceId(), user, merchant, bank)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message(
						"Encryption key expired/invalid. Recreating a new encryption key for user " + user.getId())
						.format());
				ResponseEntity resp = getEncryptionKey(user, merchant, bank, true);
				if (resp.getStatusCodeValue() != HttpStatus.SC_OK) {
					throw new APIException(resp.getStatusCode(), (APIResponse) resp.getBody());
				}
				EncryptionResponse eResp = (EncryptionResponse) resp.getBody();
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("EncryptionResponse", eResp)
						.format());
				algo = eResp.getAlgo();
				padding = eResp.getPadding();
				key = eResp.getKey();
			}

			CryptoUtil.AlgoScheme algoScheme = CryptoUtil.AlgoScheme.scheme(algo, padding);
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Data", encryptedData)
					.maskedData("Encryptionkey", Mask.KEY_MASKED.value()).data("Algo", algo).data("Padding", padding)
					.format());

			return new String(cryptoUtil.decrypt(Base64.decodeBase64(encryptedData), algoScheme,
					key.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught").format(), e);
		}

		return null;
	}

	public String encrypt(final User user, final Merchant merchant, final Bank bank, String rawData)
			throws APIException {
		try {
			String algo = user.getEncryptionAlgo();
			String padding = user.getEncryptionPadding();
			String key = user.getEncryptionKey();
			if (isEncryptionExpired(httpServletContext.getTraceId(), user, merchant, bank)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message(
						"Encryption key expired/invalid. Recreating a new encryption key for user " + user.getId())
						.format());
				ResponseEntity resp = getEncryptionKey(user, merchant, bank, true);
				if (resp.getStatusCodeValue() != HttpStatus.SC_OK) {
					throw new APIException(resp.getStatusCode(), (APIResponse) resp.getBody());
				}
				EncryptionResponse eResp = (EncryptionResponse) resp.getBody();
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("EncryptionResponse", eResp)
						.format());
				algo = eResp.getAlgo();
				padding = eResp.getPadding();
				key = eResp.getKey();
			}
			CryptoUtil.AlgoScheme algoScheme = CryptoUtil.AlgoScheme.scheme(algo, padding);
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Data", rawData)
					.maskedData("Encryptionkey", Mask.KEY_MASKED.value()).data("Algo", algo).data("Padding", padding)
					.format());
			return Base64.encodeBase64String(cryptoUtil.encrypt(rawData.getBytes(StandardCharsets.UTF_8), algoScheme,
					key.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught").format(), e);
		}

		return null;
	}

	private void populateEncryptionInfo(String traceId, final User user, EncryptionResponse response) {
		if (user != null && response != null) {

			LOG.debug(LogFormatter.instance(traceId).maskedData("key", Mask.KEY_MASKED.value())
					.data("Algo", user.getEncryptionAlgo()).data("Padding", user.getEncryptionPadding())
					.data("ExpiryTime", user.getEncryptionKeyExpiryTime()).format());
			response.setKey(user.getEncryptionKey());
			response.setAlgo(user.getEncryptionAlgo());
			response.setPadding(user.getEncryptionPadding());
			response.setExpiryTime(user.getEncryptionKeyExpiryTime());
		}
	}

}
