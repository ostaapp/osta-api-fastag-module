/**
 * 
 */
package com.dipcoin.api.resource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.MerchantDipcoinRequest;
//import com.dipcoin.api.model.PaymentDetailsResponse;
import com.dipcoin.api.model.UserDeviceInfoResponse;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.qrcode.QRCodeUtils;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.dao.TollTagDao;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import com.dipcoin.db.services.model.UserDevice;

/**
 *
 */
@Component("merchantResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class MerchantResource extends PartnerResource {

	private static final Logger LOG = LogManager.getLogger(MerchantResource.class);
	public static final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa");

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private CryptoUtil cryptoUtil;

	@Autowired
	TollTagDao tollTagDao;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	public ResponseEntity getMerchantDipcoinRequestQRCode(final User user, final Merchant merchant,
			final MerchantDipcoinRequest createReq, Integer width, Integer height, String image) throws Exception {

		ResponseEntity response = getMerchantDipcoinRequest(user, merchant, createReq);
		if (response.getStatusCode() != HttpStatus.OK) {
			return response;
		}

		final byte[] data = QRCodeUtils.generateQRCode(response.getBody(), width, height, "merchant_osta_request",
				image);

		return APIUtils.generateMultiPartResponse(data, "merchant_osta_request." + image);

	}

	public ResponseEntity getMerchantDipcoinRequest(final User user, final Merchant merchant,
			final MerchantDipcoinRequest createReq) throws Exception {

		if (!this.userDBService.merchantRepresentative(user) || !this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}
		if (createReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching User Device")
				.data("user", user.getId()).format());
		List<UserDevice> devices = userDBService.asyncGetUserDevicesByTypes(user.getId(), null).get();
		if (!CollectionUtils.isEmpty(devices)) {
			// @TODO - choose UserDevice associated with merchant only.
			UserDevice device = devices.get(0);

			UserDeviceInfoResponse info = new UserDeviceInfoResponse();
			info.setDeviceType(device.getDeviceType());
			info.setImeiNo(device.getImeiNo());
			info.setRegistrationToken(device.getRegistrationToken());

			// @TODO - Move this encryption to per merchant basis
			// @NOTE - For decryption refer to CustomerDipcoinResource.processDipcoin
			createReq.setDeviceHash(APIUtils.encryptUserDeviceInfo(cryptoUtil, info));
		}
		createReq.setPartnerReferenceId(merchant.getReferenceId());

		return ResponseEntity.ok(createReq);
	}
}
